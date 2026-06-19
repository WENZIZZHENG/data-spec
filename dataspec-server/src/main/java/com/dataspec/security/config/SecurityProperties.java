package com.dataspec.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DataSpec 轻量安全配置。
 */
@Component
@ConfigurationProperties(prefix = "dataspec.security")
public class SecurityProperties {

    /**
     * 是否启用 Bearer token 校验。
     * 默认关闭，避免破坏个人本地开发和既有脚本。
     */
    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
