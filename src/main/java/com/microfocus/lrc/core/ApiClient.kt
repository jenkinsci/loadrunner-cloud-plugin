/*
 * © Copyright 2022 Micro Focus or one of its affiliates.
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.microfocus.lrc.core

import com.google.gson.JsonObject
import com.microfocus.lrc.core.entity.ApiTestRunReport
import com.microfocus.lrc.core.entity.ServerConfiguration
import com.microfocus.lrc.jenkins.LoggerProxy
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

class ApiClient internal constructor(
    private val serverConfiguration: ServerConfiguration,
    private val loggerProxy: LoggerProxy
) : Closeable {
    private companion object {
        val MEDIA_TYPE_JSON = Constants.APPLICATION_JSON.toMediaType()

        const val LRX_AUTH_TOKEN_PATH = "auth/api/public/tenant-oauth2-token"
        const val LRX_TENANTS_PATH = "tenants/api/user/tenants"
        const val LRC_PRODUCT_NAME = "LRC"

        private val CT_TENANT_REGEX = Regex("^t_(\\d+)$")

        @JvmStatic
        fun toCtTenantId(tenantId: String?): String? {
            if (tenantId == null) {
                return null
            }
            return CT_TENANT_REGEX.find(tenantId)?.groupValues?.get(1) ?: tenantId
        }

        @JvmStatic
        fun isOAuthClientId(username: String?): Boolean {
            return ((username != null)
                    && (username.length >= 42)
                    && username.startsWith("oauth2-")
                    && username.endsWith("@microfocus.com"))
        }
    }

    private var tokenAuth: String? = null
    private var okHttpClient: OkHttpClient? = null

    private fun prepareUrlBuilder(apiPath: String): HttpUrl.Builder {
        val urlBuilder: HttpUrl.Builder = this.parseURL(this.serverConfiguration.url)
            .newBuilder()
            .addPathSegments(apiPath)

        if (!this.serverConfiguration.tenantId.isNullOrEmpty()) {
            urlBuilder.addQueryParameter("TENANTID", this.serverConfiguration.tenantId)
        }

        return urlBuilder
    }

    private fun prepareRequestBuilder(
        url: HttpUrl,
        contentType: String = Constants.APPLICATION_JSON
    ): Request.Builder {
        val reqBuilder: Request.Builder = Request.Builder()
            .url(url)
            .addHeader("Content-Type", contentType)
            .addHeader("cache-control", "no-cache")

        if (this.tokenAuth != null) {
            reqBuilder.addHeader("Authorization", "Bearer ${this.tokenAuth}")
        }

        return reqBuilder
    }

    private fun execute(reqBuilder: Request.Builder): Response {
        try {
            return this.getOkhttpClient().newCall(reqBuilder.build()).execute()
        } catch (ex: UnknownHostException) {
            this.loggerProxy.error("Unknown host. Check your configuration.")
            throw ex
        } catch (ex: SSLException) {
            this.loggerProxy.error("SSL exception occurred. Check if you are behind a proxy or firewall.")
            throw ex
        }
    }

    fun get(
        apiPath: String,
        queryParams: Map<String, String>? = null,
        encodedQueryParams: Map<String, String>? = null,
        contentType: String = Constants.APPLICATION_JSON
    ): Response {
        val urlBuilder = this.prepareUrlBuilder(apiPath)

        if (!queryParams.isNullOrEmpty()) {
            queryParams.forEach { (k, v) -> run { urlBuilder.addQueryParameter(k, v); } }
        }

        if (!encodedQueryParams.isNullOrEmpty()) {
            encodedQueryParams.forEach { (k, v) -> run { urlBuilder.addEncodedQueryParameter(k, v) } }
        }

        val reqBuilder = this.prepareRequestBuilder(urlBuilder.build(), contentType).get()
        return this.execute(reqBuilder)
    }

    fun post(apiPath: String, queryParams: Map<String, String>? = null, payload: JsonObject): Response {
        val urlBuilder = this.prepareUrlBuilder(apiPath)
        if (!queryParams.isNullOrEmpty()) {
            queryParams.forEach { (k, v) -> run { urlBuilder.addQueryParameter(k, v); } }
        }
        val reqBuilder = this.prepareRequestBuilder(urlBuilder.build())
        reqBuilder.post(payload.toString().toRequestBody(MEDIA_TYPE_JSON))

        return this.execute(reqBuilder)
    }

    fun put(apiPath: String, queryParams: Map<String, String>? = null, payload: JsonObject): Response {
        val urlBuilder = this.prepareUrlBuilder(apiPath)

        if (!queryParams.isNullOrEmpty()) {
            queryParams.forEach { (k, v) -> run { urlBuilder.addQueryParameter(k, v); } }
        }

        val reqBuilder = this.prepareRequestBuilder(urlBuilder.build())
        reqBuilder.put(payload.toString().toRequestBody(MEDIA_TYPE_JSON))

        return this.execute(reqBuilder)
    }

    private fun loginOAuthViaLrx() {
        // Preserve the original LRx base URL so that repeated logins (e.g. a second
        // ApiClient created from the same, already-resolved ServerConfiguration) keep
        // authenticating against LRx instead of the resolved LRC target URL.
        val lrxBaseUrl = this.serverConfiguration.lrxUrl ?: this.serverConfiguration.url
        this.serverConfiguration.lrxUrl = lrxBaseUrl

        this.tokenAuth = this.getBearerToken(lrxBaseUrl)

        val lrcTargetUrl = this.getLrcTargetUrl(lrxBaseUrl)
        if (lrcTargetUrl.isNullOrEmpty()) {
            throw IOException(
                "Authorization failed: no Core Performance Engineering (LRC) product is available "
                        + "for this tenant/client."
            )
        }

        this.serverConfiguration.url = lrcTargetUrl.trimEnd('/')
        this.loggerProxy.info("Login OK (LRx -> LRC). Target: ${this.serverConfiguration.url}")
    }

    private fun buildLrxUrl(lrxBaseUrl: String, apiPath: String): HttpUrl {
        val builder = this.parseURL(lrxBaseUrl)
            .newBuilder()
            .addPathSegments(apiPath)

        val ctTenantId = toCtTenantId(this.serverConfiguration.tenantId)
        if (!ctTenantId.isNullOrEmpty()) {
            builder.addQueryParameter("TENANTID", ctTenantId)
        }

        return builder.build()
    }

    private fun getBearerToken(lrxBaseUrl: String): String {
        val ctTenantId = toCtTenantId(this.serverConfiguration.tenantId)
        val url = this.buildLrxUrl(lrxBaseUrl, LRX_AUTH_TOKEN_PATH)

        val payload = JsonObject()
        payload.addProperty("tenantId", ctTenantId)
        payload.addProperty("clientKey", this.serverConfiguration.username)
        payload.addProperty("clientSecret", this.serverConfiguration.password)

        val reqBuilder = Request.Builder()
            .url(url)
            .addHeader("Content-Type", Constants.APPLICATION_JSON)
            .addHeader("Accept", Constants.APPLICATION_JSON)
            .addHeader("cache-control", "no-cache")
            .post(payload.toString().toRequestBody(MEDIA_TYPE_JSON))

        this.execute(reqBuilder).use { res ->
            if (!res.isSuccessful) {
                throw IOException(this.mapLrxLoginError(res))
            }

            val body = res.body?.string()
            val contentType = res.header("content-type", null)
            if ((contentType != null) && !contentType.contains("json", ignoreCase = true)) {
                throw IOException(
                    "The configured URL is not a valid LRx endpoint (unexpected response). "
                            + "Make sure the server URL is the tenant's LRx URL, not the LRC URL."
                )
            }

            val resObj = Utils.parseJsonString(body, "Failed to parse authentication response data")
            if (resObj.has("bearerToken") && !resObj["bearerToken"].isJsonNull) {
                return resObj["bearerToken"].asString
            }

            throw IOException("Failed to login $lrxBaseUrl. LRx did not return a bearer token.")
        }
    }

    private fun getLrcTargetUrl(lrxBaseUrl: String): String? {
        val url = this.buildLrxUrl(lrxBaseUrl, LRX_TENANTS_PATH)

        val reqBuilder = Request.Builder()
            .url(url)
            .addHeader("Accept", Constants.APPLICATION_JSON)
            .addHeader("cache-control", "no-cache")
            .addHeader("Authorization", "Bearer ${this.tokenAuth}")
            .get()

        this.execute(reqBuilder).use { res ->
            if (!res.isSuccessful) {
                throw IOException(this.mapLrxLoginError(res))
            }

            val resObj = Utils.parseJsonString(res.body?.string(), "Failed to parse tenants response data")
            if (!resObj.has("products") || !resObj["products"].isJsonArray) {
                return null
            }

            for (element in resObj["products"].asJsonArray) {
                if (!element.isJsonObject) {
                    continue
                }
                val product = element.asJsonObject
                val productName = if (product.has("productName") && !product["productName"].isJsonNull) {
                    product["productName"].asString
                } else {
                    null
                }
                if (LRC_PRODUCT_NAME == productName
                    && product.has("farmUrl") && !product["farmUrl"].isJsonNull
                ) {
                    return product["farmUrl"].asString
                }
            }

            return null
        }
    }

    private fun mapLrxLoginError(res: Response): String {
        return when (res.code) {
            400 ->
                "Authentication failed: Invalid client id or client secret. "
                    .plus("Please verify your credentials and try again.")
            401, 403 ->
                "Connection failed: The configured server endpoint does not match your tenant. "
                    .plus("Please verify that the URL and tenant id are correct.")
            404 ->
                "The configured URL is not a valid LRx endpoint. "
                    .plus("Make sure the server URL is the tenant's LRx URL, not the LRC URL.")
            else ->
                "Failed to login ${this.serverConfiguration.url}. "
                    .plus("Status code: ${res.code}, details: ${res.body?.string()}")
        }
    }

    fun login() {
        if (isOAuthClientId(this.serverConfiguration.username)) {
            this.loginOAuthViaLrx()
        } else {
            throw Exception("Invalid Credentials type. Use valid client ID and secret")
        }
    }

    fun validateTenant() {
        val res = this.get("v1/projects")
        res.use {
            val body = res.body?.string()
            if (res.code != 200) {
                throw IOException("Failed to retrieve projects from tenant: ${res.code}, $body")
            }

            if (!Utils.isValidJsonArray(body)) {
                throw IOException("Failed to retrieve projects from tenant")
            }
        }
    }

    fun getReport(reportId: Int): InputStream? {
        val apiPath = ApiTestRunReport(
            mapOf(
                "reportId" to "$reportId",
            )
        ).path

        val res = this.get(apiPath)
        if (res.code != 200) {
            this.loggerProxy.info("Report #$reportId is not ready: ${res.code}, ${res.body?.string()}")
            res.close()
            return null
        }

        val contentType = res.header("content-type", null)
        if (contentType?.contains("application/octet-stream") == true) {
            return res.body?.byteStream()
        } else {
            this.loggerProxy.info("Unknown content type: $contentType")
            res.close()
            return null
        }
    }

    fun getServerConfiguration(): ServerConfiguration {
        return this.serverConfiguration
    }

    private fun getOkhttpClient(): OkHttpClient {
        var c = this.okHttpClient
        if (c != null) {
            return c
        }

        val builder = OkHttpClient.Builder()
        val proxyConfiguration = this.serverConfiguration.proxyConfiguration
        if (this.serverConfiguration.proxyConfiguration != null) {
            builder.proxy(proxyConfiguration.proxy)
            if (proxyConfiguration.username != null && proxyConfiguration.password != null) {
                val auth = Authenticator { _, response ->
                    val cred: String = Credentials.basic(proxyConfiguration.username, proxyConfiguration.password)
                    response.request.newBuilder().header("Proxy-Authorization", cred).build()
                }

                builder.proxyAuthenticator(auth)
            }
        }

        c = builder
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
        this.okHttpClient = c

        return c
    }

    private fun parseURL(urlStr: String): HttpUrl {
        return urlStr.toHttpUrlOrNull() ?: throw Exception("Invalid URL: $urlStr")
    }

    override fun close() {
        val c = this.okHttpClient
        if (c != null) {
            c.dispatcher.executorService.shutdown()
            c.connectionPool.evictAll()
            this.okHttpClient = null
        }
    }
}

class ApiClientFactory {
    companion object {
        @JvmStatic
        fun getClient(
            serverConfiguration: ServerConfiguration,
            loggerProxy: LoggerProxy = LoggerProxy()
        ): ApiClient {
            val client = ApiClient(serverConfiguration, loggerProxy)
            client.login()
            client.validateTenant()

            return client
        }
    }
}
