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

package com.microfocus.lrc.core.entity;

import hudson.util.Secret;

import java.io.Serializable;
import java.util.Map;

public final class ServerConfiguration implements Serializable {
    private String url;
    private String username;
    private Secret password;
    private String tenantId;
    private final int projectId;
    private ProxyConfiguration proxyConfiguration;
    private final boolean sendEmail;

    // #region getter/setter
    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password.getPlainText();
    }

    public String getTenantId() {
        return tenantId;
    }

    public ProxyConfiguration getProxyConfiguration() {
        return proxyConfiguration;
    }

    public int getProjectId() {
        return projectId;
    }

    public boolean isSendEmail() {
        return sendEmail;
    }
    // #endregion

    /**
     * constructor.
     * @param url
     * @param username
     * @param password
     * @param tenantId
     * @param projectId
     * @param sendEmail
     */
    public ServerConfiguration(final String url, final String username, final String password, final String tenantId,
            final int projectId, final boolean sendEmail) {
        this.url = url;
        this.username = username;
        this.password = Secret.fromString(password);
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.sendEmail = sendEmail;
    }

    public void setProxyConfiguration(final ProxyConfiguration proxyConfiguration) {
        this.proxyConfiguration = proxyConfiguration;
    }

    public void overrideConfig(final Map<String, String> conf) {
        if (conf.containsKey(StringOptionInEnvVars.LRC_URL.name())) {
            this.url = conf.get(StringOptionInEnvVars.LRC_URL.name());
        }
        if (conf.containsKey(StringOptionInEnvVars.LRC_TENANT_ID.name())) {
            this.tenantId = conf.get(StringOptionInEnvVars.LRC_TENANT_ID.name());
        }
        if (conf.containsKey(StringOptionInEnvVars.LRC_USERNAME.name())) {
            this.username = conf.get(StringOptionInEnvVars.LRC_USERNAME.name());
        }
        if (conf.containsKey(StringOptionInEnvVars.LRC_PASSWORD.name())) {
            this.password = Secret.fromString(conf.get(StringOptionInEnvVars.LRC_PASSWORD.name()));
        }
    }
}
