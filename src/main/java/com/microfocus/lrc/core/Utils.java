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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.microfocus.lrc.jenkins.LoggerProxy;
import net.sf.json.JSONArray;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

public final class Utils {
    public static final int MASK_PREFIX_LEN = 4;
    public static final int MASK_SUFFIX_LEN = 4;

    static final int MAX_LRC_URL_LEN = 80;
    static final int MAX_LRC_TENANT_LEN = 20;

    private Utils() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean isPositiveInteger(final String str) {
        int val;
        try {
            val = Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            return false;
        }

        return (val > 0);
    }

    public static boolean isEmpty(final String str) {
        return StringUtils.isEmpty(str) || StringUtils.isBlank(str);
    }

    public static JsonObject parseJsonString(final String str, final String errMsg) throws IOException {
        try {
            return new Gson().fromJson(str, JsonObject.class);
        } catch (Exception ex) {
            throw new IOException(errMsg);
        }
    }

    public static boolean isValidUrl(final String url) {
        try {
            String protocol = (new URL(url)).getProtocol();

            return (protocol.equalsIgnoreCase("https") || protocol.equalsIgnoreCase("http"));
        } catch (MalformedURLException ex) {
            return false;
        }
    }

    public static boolean isValidLRCUrl(final String url) {
        if (!isValidUrl(url)) {
            return false;
        }

        // check in allowed lists?

        return url.length() <= MAX_LRC_URL_LEN;
    }

    public static boolean isValidLRCTenant(final String tenant) {
        if (isEmpty(tenant)) {
            return false;
        }

        return tenant.length() <= MAX_LRC_TENANT_LEN;
    }

    public static String maskString(final String str, final int prefixLen, final int suffixLen) {
        if (Utils.isEmpty(str)) {
            return str;
        }

        int len = str.length();
        if (len <= prefixLen + suffixLen) {
            return str;
        }

        char[] chars = str.toCharArray();
        Arrays.fill(chars, prefixLen, len - suffixLen, '*');

        return new String(chars);
    }

    public static void logException(final LoggerProxy logger, final String msg, final Exception ex) {
        if (ex.getMessage() != null) {
            logger.error(msg + ex.getMessage());
        } else {
            logger.error(msg);
        }
    }

    public static boolean isValidJsonArray(final String str) {
        try {
            JSONArray.fromObject(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static Document newXmlDocument() throws ParserConfigurationException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        return documentBuilderFactory.newDocumentBuilder().newDocument();
    }

    public static Transformer newXmlTransformer() throws TransformerFactoryConfigurationError,
            TransformerConfigurationException, NullPointerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

        return transformerFactory.newTransformer();
    }
}
