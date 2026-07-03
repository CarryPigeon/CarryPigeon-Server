package team.carrypigeon.backend.chat.domain.features.message.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 消息插件治理配置。
 * 职责：收敛当前内建消息插件的最小启停配置边界。
 * 边界：这里只描述当前仓库真实使用的启停能力，不扩展为完整插件管理平台配置模型。
 */
@ConfigurationProperties(prefix = "cp.chat.message.plugins")
public class MessagePluginGovernanceProperties {

    private boolean textEnabled = true;
    private boolean pluginEnabled = true;
    private boolean customEnabled = true;
    private boolean systemEnabled = true;
    private boolean fileEnabled = true;
    private boolean voiceEnabled = true;

    public boolean textEnabled() {
        return textEnabled;
    }

    public void setTextEnabled(boolean textEnabled) {
        this.textEnabled = textEnabled;
    }

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

    /**
     * 设置 system 消息插件是否启用。
     * 影响范围：关闭后系统消息类型不会注册到消息插件目录。
     *
     * @param systemEnabled true 表示启用 system 消息插件
     */
    public void setSystemEnabled(boolean systemEnabled) {
        this.systemEnabled = systemEnabled;
    }

    public boolean fileEnabled() {
        return fileEnabled;
    }

    /**
     * 设置 file 消息插件是否启用。
     * 影响范围：关闭后文件消息类型不会注册到消息插件目录。
     *
     * @param fileEnabled true 表示启用 file 消息插件
     */
    public void setFileEnabled(boolean fileEnabled) {
        this.fileEnabled = fileEnabled;
    }

    public boolean voiceEnabled() {
        return voiceEnabled;
    }

    /**
     * 设置 voice 消息插件是否启用。
     * 影响范围：关闭后语音消息类型不会注册到消息插件目录。
     *
     * @param voiceEnabled true 表示启用 voice 消息插件
     */
    public void setVoiceEnabled(boolean voiceEnabled) {
        this.voiceEnabled = voiceEnabled;
    }
}
