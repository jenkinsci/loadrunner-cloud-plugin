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

import java.io.Serializable

class TestRunOptions(
    val testId: Int,
    val sendEmail: Boolean,
    var skipPdfReport: Boolean,
    var isDebug: Boolean,
    var isTestMode: Boolean
) : Serializable {
    constructor(testId: Int, sendEmail: Boolean) : this(
        testId, sendEmail, false, false, false
    )
}
