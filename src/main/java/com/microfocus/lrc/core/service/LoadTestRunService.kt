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

package com.microfocus.lrc.core.service

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.microfocus.lrc.core.ApiClient
import com.microfocus.lrc.core.Utils
import com.microfocus.lrc.core.entity.*
import com.microfocus.lrc.jenkins.LoggerProxy
import java.io.IOException

class LoadTestRunService(
    private val client: ApiClient,
    private val loggerProxy: LoggerProxy,
) {
    fun fetch(runId: String): LoadTestRun? {
        val apiPath = ApiGetTestRun(
            mapOf("runId" to runId)
        ).path
        val response = client.get(apiPath)
        response.use {
            if (response.isSuccessful) {
                val json = response.body?.string() ?: return null
                val jsonObj = Utils.parseJsonString(json, "Failed to parse test run data for #${runId}")
                val lt = LoadTest(client.getServerConfiguration().projectId, jsonObj.get("testId").asInt)
                val testRun = LoadTestRun(
                    runId.toInt(),
                    lt
                )

                testRun.update(jsonObj)

                return testRun
            } else {
                loggerProxy.error("Failed to fetch run $runId. HTTP status code: ${response.code}, " +
                        "body: ${response.body?.string()?.take(512)}")
                return null
            }
        }
    }

    fun fetch(testRun: LoadTestRun) {
        val apiPath = ApiGetTestRun(
            mapOf("runId" to testRun.id.toString())
        ).path
        val response = client.get(apiPath)
        response.use {
            if (response.isSuccessful) {
                val json = response.body?.string()
                val jsonObj: JsonObject
                try {
                    jsonObj = Gson().fromJson(json, JsonObject::class.java)
                } catch (ex: Exception) {
                    this.loggerProxy.error("Failed to parse run status")
                    this.loggerProxy.debug("Got run status response: $json")
                    throw IOException("Unauthorized")
                }
                testRun.update(jsonObj)
            } else {
                if (response.code == 401) {
                    throw IOException("Unauthorized")
                }

                throw IOException("Failed to fetch run ${testRun.id}. HTTP status code: ${response.code}, " +
                        "body: ${response.body?.string()?.take(512)}")
            }
        }
    }

    fun fetchStatus(testRun: LoadTestRun) {
        val apiPath = ApiGetRunStatus(
            mapOf(
                "projectId" to "${this.client.getServerConfiguration().projectId}",
                "loadTestId" to "${testRun.loadTest.id}",
                "runId" to "${testRun.id}",
            )
        ).path
        val res = this.client.get(apiPath)
        res.use {
            val code = res.code
            if (code != 200) {
                if (code == 401) {
                    throw IOException("Unauthorized")
                }

                throw IOException("Failed to fetch status for run ${testRun.id}: $code")
            }
            val body = res.body?.string()
            this.loggerProxy.debug("Fetching test run status got $code, $body")
            val obj = Utils.parseJsonString(body, "Failed to parse test run status data for #${testRun.id}")
            testRun.update(obj)
        }
    }

    fun abort(testRun: LoadTestRun) {
        val apiPath = ApiChangeTestRunStatus(
            mapOf(
                "runId" to "${testRun.id}",
            )
        ).path

        val res = this.client.put(apiPath, mapOf("action" to "STOP"), JsonObject())
        res.use {
            val code = res.code
            val body = res.body?.string()
            this.loggerProxy.debug("Aborting test run got $code, $body")
            if (code != 200) {
                this.loggerProxy.info("Aborting test run failed: $code, $body")
                throw IOException("Aborting test run [${testRun.id}] failed")
            }

            this.loggerProxy.info("Aborting test run successfully.")
        }
    }

    fun getResults(runId: Int): TestRunResultsResponse {
        val apiPath = ApiTestRunResults(
            mapOf(
                "runId" to "$runId",
            )
        ).path

        val res = this.client.get(apiPath)
        res.use {
            if (res.code != 200) {
                val msg = "Failed to fetch test run results: ${res.code}, ${res.body?.string()}"
                this.loggerProxy.info(msg)
                throw IOException(msg)
            }

            val body = res.body?.string()
            // this.loggerProxy.debug("Fetched test run results: $body")
            try {
                return Gson().fromJson(body, TestRunResultsResponse::class.java)
            } catch (e: JsonSyntaxException) {
                this.loggerProxy.info("Failed to parse test run results: $body")
                throw e
            }
        }
    }

    fun getTransactions(runId: Int): Array<TestRunTransactionsResponse> {
        val apiPath = ApiTestRunTransctions(
            mapOf(
                "runId" to "$runId",
            )
        ).path

        val res = this.client.get(apiPath)
        res.use {
            if (res.code != 200) {
                val msg = "Failed to fetch test run transactions: ${res.code}, ${res.body?.string()}"
                this.loggerProxy.info(msg)
                throw IOException(msg)
            }

            val body = res.body?.string()
            this.loggerProxy.debug("Fetched transactions results: $body")
            try {
                return Gson().fromJson(body, Array<TestRunTransactionsResponse>::class.java)
            } catch (e: JsonSyntaxException) {
                this.loggerProxy.info("Failed to parse test run transactions: $body")
                throw e
            }
        }
    }

    fun getTrtSummary(runId: Int): Array<TestRunTrtSummaryResponse> {
        val apiPath = ApiTestRunTrtSummary(
            mapOf(
                "runId" to "$runId",
            )
        ).path

        val res = this.client.get(apiPath)
        res.use {
            if (res.code != 200) {
                val msg = "Failed to fetch test run trt summary: ${res.code}, ${res.body?.string()}"
                this.loggerProxy.info(msg)
                throw IOException(msg)
            }

            val body = res.body?.string()
            this.loggerProxy.debug("Fetched trt summary results: $body")
            try {
                return Gson().fromJson(body, Array<TestRunTrtSummaryResponse>::class.java)
            } catch (e: JsonSyntaxException) {
                this.loggerProxy.info("Failed to parse test run trt summary: $body")
                throw e
            }
        }
    }
}
