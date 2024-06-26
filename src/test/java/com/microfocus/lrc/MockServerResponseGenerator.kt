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

package com.microfocus.lrc

import com.google.gson.JsonObject
import com.microfocus.lrc.jenkins.TestRunBuilderTest
import okhttp3.mockwebserver.MockResponse

class MockServerResponseGenerator {
    companion object {
        @JvmStatic
        fun mockLogin() {
            val responseLogin = MockResponse()
            val loginResObj = JsonObject()
            loginResObj.addProperty("token", "fake_token")
            responseLogin.setBody(loginResObj.toString())
            TestRunBuilderTest.mockserver.enqueue(responseLogin)

            val responseProjects = MockResponse()
            responseProjects.setBody("[]")
            TestRunBuilderTest.mockserver.enqueue(responseProjects)
        }

        @JvmStatic
        fun mockReports() {
            val fakeReportContent = "FAKE_REPORT_CONTENT"
            val responseReportContent = MockResponse().setBody(fakeReportContent)
            responseReportContent.setHeader("Content-Type", "application/octet-stream")
            TestRunBuilderTest.mockserver.enqueue(responseReportContent)
            TestRunBuilderTest.mockserver.enqueue(responseReportContent)
        }

        @JvmStatic
        fun mockTestRunResults() {
            val responseTestRunResults = MockResponse()
            val resJson =
                "{\"dateTime\":\"06/22/2022 12:54 GMT\",\"status\":\"PASSED\",\"duration\":\"00:06:31\",\"delayDuration\":null,\"apiVusers\":2,\"uiVusers\":0,\"devVusers\":0,\"erpVusers\":0,\"legacyVusers\":0,\"mobileVusers\":0,\"runMode\":\"Duration\",\"excludeThinkTime\":false,\"schedulingPauseDuration\":0,\"percentileValue\":90,\"totalVusers\":2,\"averageThroughput\":\"N/A/s\",\"totalThroughput\":\"N/A\",\"averageHits\":\"0 hits/s\",\"totalHits\":null,\"averageBytesSent\":null,\"totalBytesSent\":null,\"totalTransactionsPassed\":5,\"totalTransactionsFailed\":0,\"failedVusers\":0,\"trtBreakersPassed\":0,\"trtBreakersFailed\":0,\"scriptErrors\":0,\"lgAlerts\":0,\"totalVU\":0,\"totalVUH\":0,\"apiVUH\":0,\"uiVUH\":0,\"devVUH\":0,\"erpVUH\":0,\"legacyVUH\":0,\"mobileVUH\":0,\"allVUH\":0,\"percentileAlgorithm\":\"Absolute\"}"
            responseTestRunResults.setBody(resJson)
            TestRunBuilderTest.mockserver.enqueue(responseTestRunResults)
        }

        @JvmStatic
        fun mockTransactions() {
            val responseTransactions = MockResponse()
            val resJson =
                "[{\"name\":\"Actions_Transaction\",\"loadTestScriptId\":4403,\"scriptName\":\"Kafka3_updated (1)\",\"breakers\":0,\"slaStatus\":\"N/A\",\"slaThreshold\":null,\"slaTrend\":0.08174046321674466,\"passed\":2,\"failed\":0,\"avgTRT\":10.030542016029358,\"minTRT\":10.023746013641357,\"maxTRT\":10.037338018417358,\"percentileTRT\":10.037338018417358,\"stdDeviation\":0.006796002388000488},{\"name\":\"vuser_end_Transaction\",\"loadTestScriptId\":4403,\"scriptName\":\"Kafka3_updated (1)\",\"breakers\":0,\"slaStatus\":\"N/A\",\"slaThreshold\":null,\"slaTrend\":-2303.2204978038067,\"passed\":1,\"failed\":0,\"avgTRT\":30.017520904541016,\"minTRT\":30.017520904541016,\"maxTRT\":30.017520904541016,\"percentileTRT\":30.017520904541016,\"stdDeviation\":0},{\"name\":\"vuser_init_Transaction\",\"loadTestScriptId\":4404,\"scriptName\":\"Kafka2\",\"breakers\":0,\"slaStatus\":\"N/A\",\"slaThreshold\":null,\"slaTrend\":-0.012488121159375871,\"passed\":1,\"failed\":0,\"avgTRT\":9.407335042953491,\"minTRT\":9.407335042953491,\"maxTRT\":9.407335042953491,\"percentileTRT\":9.407335042953491,\"stdDeviation\":0},{\"name\":\"vuser_init_Transaction\",\"loadTestScriptId\":4403,\"scriptName\":\"Kafka3_updated (1)\",\"breakers\":0,\"slaStatus\":\"N/A\",\"slaThreshold\":null,\"slaTrend\":-0.023571880733480634,\"passed\":1,\"failed\":0,\"avgTRT\":9.402602910995483,\"minTRT\":9.402602910995483,\"maxTRT\":9.402602910995483,\"percentileTRT\":9.402602910995483,\"stdDeviation\":0}]"
            responseTransactions.setBody(resJson)
            TestRunBuilderTest.mockserver.enqueue(responseTransactions)
        }

        @JvmStatic
        fun mockTestPercentile() {
            val response = MockResponse()
            val resJson = "{\n" +
                    "  \"percentile\": 90\n" +
                    "}"
            response.setBody(resJson)
            TestRunBuilderTest.mockserver.enqueue(response)
        }

        @JvmStatic
        fun mockTestTransactions() {
            val response = MockResponse()
            val resJson = "[\n" +
                    "  {\n" +
                    "    \"id\": 58,\n" +
                    "    \"enabled\": false,\n" +
                    "    \"scriptId\": 18,\n" +
                    "    \"testScriptId\": 40,\n" +
                    "    \"transactionName\": \"Login\",\n" +
                    "    \"slaPercentileThreshold\": 3,\n" +
                    "    \"stopOnBreak\": false,\n" +
                    "    \"scriptName\": \"prodemand-v03_fixed\",\n" +
                    "    \"failedTrxRatio\": 10,\n" +
                    "    \"failedTrxEnabled\": false\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"id\": 59,\n" +
                    "    \"enabled\": false,\n" +
                    "    \"scriptId\": 18,\n" +
                    "    \"testScriptId\": 40,\n" +
                    "    \"transactionName\": \"LandingPage\",\n" +
                    "    \"slaPercentileThreshold\": 3,\n" +
                    "    \"stopOnBreak\": false,\n" +
                    "    \"scriptName\": \"prodemand-v03_fixed\",\n" +
                    "    \"failedTrxRatio\": 10,\n" +
                    "    \"failedTrxEnabled\": false\n" +
                    "  }\n" +
                    "]"
            response.setBody(resJson)
            TestRunBuilderTest.mockserver.enqueue(response)
        }

        @JvmStatic
        fun mockTrtSummary() {
            val response = MockResponse()
            val resJson = "[\n" +
                    "  {\n" +
                    "    \"name\": \"LandingPage\",\n" +
                    "    \"loadTestScriptId\": 41,\n" +
                    "    \"scriptName\": \"prodemand-v03\",\n" +
                    "    \"maxTRT\": 3.0614006519317627,\n" +
                    "    \"avgTRT\": 2.0746798515319824,\n" +
                    "    \"minTRT\": 1.8320441246032715,\n" +
                    "    \"passed\": 46,\n" +
                    "    \"failed\": 0,\n" +
                    "    \"successRate\": 100,\n" +
                    "    \"avgTPS\": 0.03723427074657141,\n" +
                    "    \"stdDeviation\": 0.2517000435241076\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"name\": \"Login\",\n" +
                    "    \"loadTestScriptId\": 41,\n" +
                    "    \"scriptName\": \"prodemand-v03\",\n" +
                    "    \"maxTRT\": 7.0419111251831055,\n" +
                    "    \"avgTRT\": 2.9132559299468994,\n" +
                    "    \"minTRT\": 2.110764980316162,\n" +
                    "    \"passed\": 46,\n" +
                    "    \"failed\": 0,\n" +
                    "    \"successRate\": 100,\n" +
                    "    \"avgTPS\": 0.03723427074657141,\n" +
                    "    \"stdDeviation\": 1.228752025001221\n" +
                    "  }\n" +
                    "]"
            response.setBody(resJson)
            TestRunBuilderTest.mockserver.enqueue(response)
        }
    }
}
