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

class TestRunTrtSummaryResponseTest {
    fun DoubleEqual(a: Double, b: Double): Boolean {
        return abs(a - b) < 0.000001
    }

    @Test
    fun parseTrtSummaryResponseSON() {
        val body =
            "  [\n" +
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
                    "    \"successRate\": null,\n" +
                    "    \"avgTPS\": 0.03723427074657141,\n" +
                    "    \"stdDeviation\": 1.228752025001221\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"name\": \"Login_Transaction\",\n" +
                    "    \"loadTestScriptId\": 41,\n" +
                    "    \"scriptName\": \"prodemand-v03\",\n" +
                    "    \"maxTRT\": 11.500606775283813,\n" +
                    "    \"avgTRT\": 6.1492629258529,\n" +
                    "    \"minTRT\": 5.175869941711426,\n" +
                    "    \"passed\": 46,\n" +
                    "    \"failed\": 0,\n" +
                    "    \"avgTPS\": 0.03723427074657141,\n" +
                    "    \"stdDeviation\": 1.4477354885807747\n" +
                    "  }\n" +
                    "]"
        val ret = Gson().fromJson(body, Array<TestRunTrtSummaryResponse>::class.java)
        assert(ret.size == 3)
        assert(ret[0].passed == 46)
        assert(ret[0].failed == 0)
        assert(DoubleEqual(ret[0].successRate, 100.0))
        assert(DoubleEqual(ret[1].successRate, 0.0))
        assert(DoubleEqual(ret[2].successRate, 0.0))
    }
}
