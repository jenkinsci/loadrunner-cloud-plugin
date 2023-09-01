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

package com.microfocus.lrc.core;

import org.junit.Test;

import static org.junit.Assert.*;

public class UtilsTest {

    @Test
    public void maskString() {
        assertEquals("no change on string", "abcdef",
                Utils.maskString("abcdef", Utils.MASK_PREFIX_LEN, Utils.MASK_SUFFIX_LEN));
        assertEquals("string", "abc1**4def",
                Utils.maskString("abc1234def", Utils.MASK_PREFIX_LEN, Utils.MASK_SUFFIX_LEN));
        assertEquals("email address", "abc@**********.com",
                Utils.maskString("abc@microfocus.com", Utils.MASK_PREFIX_LEN, Utils.MASK_SUFFIX_LEN));
        assertEquals("email address 2 + 3", "ab*************com",
                Utils.maskString("abc@microfocus.com", 2, 3));
    }

    @Test
    public void isValidUrl() {
        assertFalse(Utils.isValidUrl("file:///tmp/t.html"));
        assertTrue(Utils.isValidUrl("http://test.com"));
        assertTrue(Utils.isValidUrl("https://test.com"));
    }

    @Test
    public void isValidLRCUrl() {
        assertTrue(Utils.isValidLRCUrl("https://loadrunner-cloud.saas.microfocus.com"));
        assertTrue(Utils.isValidLRCUrl("https://loadrunner-cloud-eur.saas.microfocus.com"));
        assertTrue(Utils.isValidLRCUrl("https://srl-qa100.saas.microfocus.com"));
        assertFalse(Utils.isValidLRCUrl("https://toolongaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
    }

    @Test
    public void isValidLRCTenant() {
        assertTrue(Utils.isValidLRCTenant("123456789"));
        assertFalse(Utils.isValidLRCTenant(""));
        assertFalse(Utils.isValidLRCTenant("123456789012345678901"));
    }
}
