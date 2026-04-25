package team.carrypigeon.backend.chat.domain.features.message.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 消息插件治理配置。
 * 职责：收敛当前内建消息插件的最小启停配置边界。
 * 边界：这里只描述当前仓库真实使用的启停能力，不扩展为完整插件管理平台配置模型。
 */
@ConfigurationProperties(prefix = "cp.chat.message.plugins")
public class MessagePluginGovernanceProperties {

    private boolean pluginEnabled = true;
    private boolean customEnabled = true;
    private boolean systemEnabled = true;
    private boolean fileEnabled = true;
    private boolean voiceEnabled = true;

    public boolean pluginEnabled() {
        return pluginEnabled;
    }

    public void setPluginEnabled(boolean pluginEnabled) {
        this.pluginEnabled = pluginEnabled;
    }

    public boolean customEnabled() {
        return customEnabled;
    }

    public void setCustomEnabled(boolean customEnabled) {
        this.customEnabled = customEnabled;
    }

    public boolean systemEnabled() {
        return systemEnabled;
    }

    public void setSystemEnabled(boolean systemEnabled) {
        this.systemEnabled = systemEnabled;
    }

    public boolean fileEnabled() {
        return fileEnabled;
    }

    public void setFileEnabled(boolean fileEnabled) {
        this.fileEnabled = fileEnabled;
    }

    public boolean voiceEnabled() {
        return voiceEnabled;
    }

    public void setVoiceEnabled(boolean voiceEnabled) {
        this.voiceEnabled = voiceEnabled;
    }
}
