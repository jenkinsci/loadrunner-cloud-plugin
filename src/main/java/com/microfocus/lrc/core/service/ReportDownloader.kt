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
import com.microfocus.lrc.core.ApiClient
import com.microfocus.lrc.core.Constants
import com.microfocus.lrc.core.XmlReport
import com.microfocus.lrc.core.entity.*
import com.microfocus.lrc.jenkins.LoggerProxy
import java.io.ByteArrayOutputStream

class ReportDownloader(
    private val apiClient: ApiClient,
    private val loggerProxy: LoggerProxy,
    private val testRunOptions: TestRunOptions
) {
    companion object {
        @JvmStatic
        fun writeCsvBytesArray(transactions: Array<TestRunTransactionsResponse>): ByteArray {
            val stream = ByteArrayOutputStream()
            val writer = stream.writer()
            writer.appendLine("Script Name, Transaction, %Breakers, SLA Status, AVG Duration, Min, Max, STD. Deviation, Passed, Failed, Percentile, SLA Threshold, Percentile Trend")
            transactions.forEach { tx ->
                writer.appendLine("${tx.scriptName}, ${tx.name}, ${tx.breakers}, ${tx.slaStatus}, ${tx.avgTRT}, ${tx.minTRT}, ${tx.maxTRT}, ${tx.stdDeviation}, ${tx.passed}, ${tx.failed}, ${tx.percentileTRT}, ${tx.slaThreshold}, ${tx.slaTrend}")
            }
            writer.flush()

            return stream.toByteArray()
        }
    }

    fun download(testRun: LoadTestRun, reportTypes: Array<String>) {
        var validReportTypes = arrayOf("csv", "pdf")
        if (this.testRunOptions.skipPdfReport) {
            validReportTypes = arrayOf("csv")
        }
        // validate report types
        val filteredReportTypes = reportTypes.filter {
            it in validReportTypes
        }
        if (filteredReportTypes.isEmpty()) {
            this.loggerProxy.info("Invalid report types: ${reportTypes.joinToString(", ")}")
            this.loggerProxy.info("Skip downloading reports")
            return
        }

        // request reports generating
        filteredReportTypes.forEach { reportType ->
            this.loggerProxy.info("Requesting $reportType report ...")
            val reportId = this.requestReportId(testRun.id, reportType)
            // wait for the report to be ready
            var retryWaitingTimes = 0
            var maxRetry = Constants.REPORT_READY_MAX_RETRY
            if (reportType == "pdf") {
                maxRetry = Constants.PDF_REPORT_READY_MAX_RETRY
            }

            val pollingInterval = if (testRunOptions.isTestMode) 100 else Constants.REPORT_DOWNLOAD_POLLING_INTERVAL

            while (retryWaitingTimes < maxRetry && !this.isReportReady(reportId)) {
                Thread.sleep(pollingInterval)
                retryWaitingTimes += 1
            }

            if (retryWaitingTimes >= maxRetry) {
                this.loggerProxy.info("Report #$reportId is not ready after $retryWaitingTimes retries")
                return
            }

            val fileName = genFileName(reportType, testRun)
            testRun.reports[fileName] = reportId
        }
    }

    private fun requestReportId(runId: Int, reportType: String): Int {
        val apiPath = ApiGenTestRunReport(
            mapOf(
                "projectId" to "${this.apiClient.getServerConfiguration().projectId}",
                "runId" to "$runId",
            )
        ).path

        val payload = JsonObject()
        payload.addProperty("reportType", reportType)

        val res = this.apiClient.post(apiPath, payload = payload)
        res.use {
            val body = res.body?.string()
            if (res.code != 200) {
                throw Exception("Failed to request report: ${res.code}, $body")
            }
            this.loggerProxy.debug("Requested report: $body")
            val result = Gson().fromJson(body, JsonObject::class.java)
            if (!result.has("reportId")) {
                throw Exception("Failed to request report: $body")
            }

            return result.get("reportId").asInt
        }
    }

    private fun isReportReady(reportId: Int): Boolean {
        val apiPath = ApiTestRunReport(
            mapOf(
                "reportId" to "$reportId",
            )
        ).path

        val res = this.apiClient.get(apiPath)
        res.use {
            if (res.code != 200) {
                this.loggerProxy.info("Report #$reportId is not ready: ${res.code}, ${res.body?.string()}")
                return false
            }
            val contentType = res.header("content-type", null)
            if (contentType?.contains(Constants.APPLICATION_JSON) == true) {
                val body = res.body?.string()
                val result = Gson().fromJson(body, JsonObject::class.java)
                if (result["message"]?.asString == "In progress") {
                    this.loggerProxy.info("Report #$reportId is not ready yet...")
                    return false
                } else {
                    throw Exception("Report #$reportId invalid status: $body")
                }
            }

            if (contentType?.contains("application/octet-stream") == true) {
                this.loggerProxy.info("Report #$reportId is ready.")

                return true
            }

            throw Exception("Unknown content type: $contentType")
        }
    }

    private fun genFileName(reportType: String, testRun: LoadTestRun): String {
        return "lrc_report_${this.apiClient.getServerConfiguration().tenantId}-${testRun.id}.${reportType}"
    }

    fun fetchTrending(testRun: LoadTestRun, benchmark: TrendingDataWrapper?): TrendingDataWrapper {
        val loadTestRunSvc = LoadTestRunService(
            this.apiClient,
            this.loggerProxy
        )

        val results = loadTestRunSvc.getResults(testRun.id)
        val transactions = loadTestRunSvc.getTransactions(testRun.id)
        return TrendingDataWrapper(
            testRun,
            results,
            transactions,
            this.apiClient.getServerConfiguration().tenantId,
            benchmark
        )
    }

    fun genXmlFile(testRun: LoadTestRun) {
        val fileName = genFileName("xml", testRun)
        val reportUrl = "${this.apiClient.getServerConfiguration().url}/run-overview/${testRun.id}/report/?TENANTID=${this.apiClient.getServerConfiguration().tenantId}&projectId=${this.apiClient.getServerConfiguration().projectId}"
        val dashboardUrl = "${this.apiClient.getServerConfiguration().url}/run-overview/${testRun.id}/dashboard/?TENANTID=${this.apiClient.getServerConfiguration().tenantId}&projectId=${this.apiClient.getServerConfiguration().projectId}"

        if (testRun.statusEnum != TestRunStatus.ABORTED) {
            this.loggerProxy.info("View report at: $reportUrl")
            this.loggerProxy.info("View dashboard at: $dashboardUrl")
        }

        var slaInfo: String? = null
        if (testRun.hasReport &&
            (testRun.statusEnum == TestRunStatus.FAILED) &&
            testRun.transactions.isNotEmpty()) {
            slaInfo = getSlaInfo(testRun)
        }

        val content = XmlReport.write(
            testRun,
            reportUrl,
            dashboardUrl,
            slaInfo
        )
        testRun.reportsByteArray[fileName] = content
    }

    fun genTxCsv(testRun: LoadTestRun) {
        val fileName = "lrc_report_trans_${this.apiClient.getServerConfiguration().tenantId}-${testRun.id}.csv"
        testRun.reportsByteArray[fileName] = writeCsvBytesArray(testRun.transactions)
    }

    // region getSlaInfo
    private class PercentileTrtSla(
        val worstTransaction: String?,
        val slaThreshold: Double,
        val worstBreakers: Double,
        val count: Int,
    )

    private class FailedTrxSla(
        val worstTransaction: String?,
        val failedTrxRatio: Double,
        val worstSuccessRate: Double,
        val count: Int,
    )

    private fun getSlaInfo(testRun: LoadTestRun): String {
        val testId = testRun.loadTest.id
        val runId = testRun.id

        val loadTestSvc = LoadTestService(
            this.apiClient,
            this.loggerProxy
        )
        val loadTestRunSvc = LoadTestRunService(
            this.apiClient,
            this.loggerProxy
        )

        val percentile = loadTestSvc.getPercentile(testId)
        val loadTestTransactions = loadTestSvc.getTransactions(testId)
        val testRunTrtSummary = loadTestRunSvc.getTrtSummary(runId)

        val scriptTransactionsMap = initScriptTransactionsMap(loadTestTransactions)
        val percentileTrtSla = getPercentileTrtSla(percentile, scriptTransactionsMap, testRun.transactions)
        val failedTrxSla = getFailedTrxSla(scriptTransactionsMap, testRunTrtSummary)

        return createSlaText(percentileTrtSla, failedTrxSla)
    }

    private fun initScriptTransactionsMap(loadTestTransactions: Array<LoadTestTransactionsResponse>): HashMap<Int, HashMap<String, LoadTestTransactionsResponse>> {
        val scriptTransactionsMap = HashMap<Int, HashMap<String, LoadTestTransactionsResponse>>()
        for (transaction in loadTestTransactions) {
            var transactions: HashMap<String, LoadTestTransactionsResponse>? = scriptTransactionsMap[transaction.testScriptId]
            if (transactions == null) {
                transactions = HashMap()
            }
            transactions[transaction.transactionName] = transaction
            scriptTransactionsMap[transaction.testScriptId] = transactions
        }

        return scriptTransactionsMap
    }

    private fun getPercentileTrtSla(
        percentile: Int,
        scriptTransactions: HashMap<Int, HashMap<String, LoadTestTransactionsResponse>>,
        testRunTransactions: Array<TestRunTransactionsResponse>
    ): PercentileTrtSla {
        var worstTransaction: String? = null
        var slaThreshold = 0.0
        var worstBreakers = 0.0
        var count = 0
        for (transaction in testRunTransactions) {
            val transactions = scriptTransactions[transaction.loadTestScriptId]
            if (transactions != null) {
                val loadTestTransaction = transactions[transaction.name]
                if ((loadTestTransaction != null) &&
                    loadTestTransaction.enabled &&
                    ((transaction.breakers - (100 - percentile)) > 0)
                ) {
                    count++
                    if (transaction.breakers > worstBreakers) {
                        worstTransaction = transaction.name
                        slaThreshold = transaction.slaThreshold
                        worstBreakers = transaction.breakers
                    }
                }
            }
        }
        return PercentileTrtSla(worstTransaction, slaThreshold, worstBreakers, count)
    }

    private fun getFailedTrxSla(
        scriptTransactionsMap: HashMap<Int, HashMap<String, LoadTestTransactionsResponse>>,
        testRunTrtSummary: Array<TestRunTrtSummaryResponse>
    ): FailedTrxSla {
        var worstTransaction: String? = null
        var failedTrxRatio = 100.0
        var worstSuccessRate = 100.0
        var count = 0

        testRunTrtSummary.forEach { transaction ->
            val transactions = scriptTransactionsMap[transaction.loadTestScriptId]
            if (transactions != null){
                val loadTestTransaction = transactions[transaction.name]
                if ((loadTestTransaction != null) && loadTestTransaction.failedTrxEnabled) {
                    val trxDeviation = 100 - transaction.successRate - loadTestTransaction.failedTrxRatio
                    if ((trxDeviation) > 0) {
                        count++
                        if (trxDeviation > (100 - failedTrxRatio - worstSuccessRate)) {
                            worstTransaction = transaction.name
                            failedTrxRatio = loadTestTransaction.failedTrxRatio
                            worstSuccessRate = transaction.successRate
                        }
                    }
                }
            }
        }
        return FailedTrxSla(worstTransaction, failedTrxRatio, worstSuccessRate, count)
    }

    private fun createSlaText(percentileTrtSla: PercentileTrtSla, failedTrxSla: FailedTrxSla): String {
        var slaText = ""
        if (percentileTrtSla.worstTransaction != null) {
            slaText += "Percentile TRT (sec) SLA was breached by " + percentileTrtSla.count.toString() + " transactions. "
            slaText += "The worst transaction was ‘" + percentileTrtSla.worstTransaction + "’ with " + String.format(
                "%.2f",
                percentileTrtSla.worstBreakers
            ).trimEnd('0').trimEnd('.') + "% exceeding " + percentileTrtSla.slaThreshold + " sec. "
        }
        if (failedTrxSla.worstTransaction != null) {
            slaText += "Failed TRX (%) SLA was breached by " + failedTrxSla.count.toString() + " transactions. "
            slaText += "The worst transaction was ‘" + failedTrxSla.worstTransaction + "’ with a failure ratio of " + String.format(
                "%.2f",
                (100.0 - failedTrxSla.worstSuccessRate)
            ).trimEnd('0').trimEnd('.') + "%. "
        }
        return slaText
    }

    // endregion
}
