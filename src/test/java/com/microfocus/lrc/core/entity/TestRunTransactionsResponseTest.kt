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

package com.microfocus.lrc.core.entity

import com.google.gson.Gson
import org.junit.Test
import kotlin.math.abs

class TestRunTransactionsResponseTest {
    fun DoubleEqual(a: Double, b: Double) : Boolean {
        return abs(a - b) < 0.000001;
    }

    @Test
    fun parseTXResponseJSON() {
        val body =
                "  {\n" +
                "    \"name\": \"Visit Home Page\",\n" +
                "    \"loadTestScriptId\": 1,\n" +
                "    \"scriptName\": \"Peaceful App V2\",\n" +
                "    \"breakers\": 3.2703374264473153,\n" +
                "    \"slaStatus\": \"Passed\",\n" +
                "    \"slaThreshold\": 10,\n" +
                "    \"slaTrend\": null,\n" +
                "    \"passed\": 401243,\n" +
                "    \"failed\": 28,\n" +
                "    \"avgTRT\": 2.707237459818728,\n" +
                "    \"minTRT\": 0.3170619010925293,\n" +
                "    \"maxTRT\": 51.61132001876831,\n" +
                "    \"percentileTRT\": 5.472877685611247,\n" +
                "    \"stdDeviation\": 3.2573858691456974\n" +
                "  }";
        val ret = Gson().fromJson(body, TestRunTransactionsResponse::class.java);
        assert(DoubleEqual(ret.slaThreshold, 10.0));
    }

    @Test
    fun parseTXResponseJSONArray() {
        val body =
                "[\n" +
                "  {\n" +
                "    \"name\": \"Visit Home Page\",\n" +
                "    \"loadTestScriptId\": 1,\n" +
                "    \"scriptName\": \"Peaceful App V2\",\n" +
                "    \"breakers\": 3.2703374264473153,\n" +
                "    \"slaStatus\": \"Passed\",\n" +
                "    \"slaThreshold\": 10,\n" +
                "    \"slaTrend\": null,\n" +
                "    \"passed\": 401243,\n" +
                "    \"failed\": 28,\n" +
                "    \"avgTRT\": 2.707237459818728,\n" +
                "    \"minTRT\": 0.3170619010925293,\n" +
                "    \"maxTRT\": 51.61132001876831,\n" +
                "    \"percentileTRT\": 5.472877685611247,\n" +
                "    \"stdDeviation\": 3.2573858691456974\n" +
                "  },\n" +
                "  {\n" +
                "    \"name\": \"Action_Transaction\",\n" +
                "    \"loadTestScriptId\": 1,\n" +
                "    \"scriptName\": \"Peaceful App V2\",\n" +
                "    \"breakers\": 0,\n" +
                "    \"slaStatus\": \"N/A\",\n" +
                "    \"slaThreshold\": null,\n" +
                "    \"slaTrend\": null,\n" +
                "    \"passed\": 401235,\n" +
                "    \"failed\": 28,\n" +
                "    \"avgTRT\": 5.707923019197213,\n" +
                "    \"minTRT\": 3.316663980484009,\n" +
                "    \"maxTRT\": 54.610921144485474,\n" +
                "    \"percentileTRT\": 8.46955207698463,\n" +
                "    \"stdDeviation\": 3.2574520378192484\n" +
                "  },\n" +
                "  {\n" +
                "    \"name\": \"Search and Buy\",\n" +
                "    \"loadTestScriptId\": 1,\n" +
                "    \"scriptName\": \"Peaceful App V2\",\n" +
                "    \"breakers\": 13.204730394905727,\n" +
                "    \"slaStatus\": \"Failed\",\n" +
                "    \"slaThreshold\": 10,\n" +
                "    \"slaTrend\": null,\n" +
                "    \"passed\": 401235,\n" +
                "    \"failed\": 28,\n" +
                "    \"avgTRT\": 5.707911945601833,\n" +
                "    \"minTRT\": 3.316663980484009,\n" +
                "    \"maxTRT\": 54.610921144485474,\n" +
                "    \"percentileTRT\": 8.469539964851833,\n" +
                "    \"stdDeviation\": 3.2574568514573947\n" +
                "  },\n" +
                "  {\n" +
                "    \"name\": \"vuser_init_Transaction\",\n" +
                "    \"loadTestScriptId\": 1,\n" +
                "    \"scriptName\": \"Peaceful App V2\",\n" +
                "    \"breakers\": 0,\n" +
                "    \"slaStatus\": \"N/A\",\n" +
                "    \"slaThreshold\": null,\n" +
                "    \"slaTrend\": null,\n" +
                "    \"passed\": 1000,\n" +
                "    \"failed\": 0,\n" +
                "    \"avgTRT\": 0.0004114634990692139,\n" +
                "    \"minTRT\": 0,\n" +
                "    \"maxTRT\": 0.003629922866821289,\n" +
                "    \"percentileTRT\": 0.00044494602415296765,\n" +
                "    \"stdDeviation\": 0.00035359262363640945\n" +
                "  },\n" +
                "  {\n" +
                "    \"name\": \"vuser_end_Transaction\",\n" +
                "    \"loadTestScriptId\": 1,\n" +
                "    \"scriptName\": \"Peaceful App V2\",\n" +
                "    \"breakers\": 0,\n" +
                "    \"slaStatus\": \"N/A\",\n" +
                "    \"slaThreshold\": null,\n" +
                "    \"slaTrend\": null,\n" +
                "    \"passed\": 1000,\n" +
                "    \"failed\": 0,\n" +
                "    \"avgTRT\": 0,\n" +
                "    \"minTRT\": 0,\n" +
                "    \"maxTRT\": 0,\n" +
                "    \"percentileTRT\": 0,\n" +
                "    \"stdDeviation\": 0\n" +
                "  }\n" +
                "]";
        val ret = Gson().fromJson(body, Array<TestRunTransactionsResponse>::class.java)
        assert(ret.size == 5);
        assert(DoubleEqual(ret[0].slaThreshold, 10.0));
        assert(DoubleEqual(ret[4].slaThreshold, 0.0));
    }
}
