package team.carrypigeon.backend.chat.domain.features.plugin.domain.extension;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 频道消息 domain 扩展契约。
 * 职责：校验并规范化对应 domain 的 canonical data。
 * 边界：插件不负责频道权限、事务、仓储或 realtime 投递。
 */
public interface ChannelMessagePlugin {

    String supportedType();

    /**
     * 返回插件负责校验和构建的对外消息 domain。
     *
     * @return 稳定消息 domain
     */
    String supportedDomain();

    /**
     * 返回该 domain 是否允许通过客户端发送入口创建。
     *
     * @return 客户端可发送时为 true
     */
    default boolean clientSendable() {
        return true;
    }

    /**
     * 校验并规范化 canonical data。
     * 输入：服务端生成的消息上下文、domain version 与待校验 data。
     * 输出：只包含该 domain 合法专属字段的数据和派生 preview。
     * 边界：插件不得在此执行消息持久化、频道权限判断或 realtime 投递。
     *
     * @param context 消息构建上下文
     * @param domainVersion 待创建消息的 domain 版本
     * @param data 待校验的 domain data
     * @return 已校验的 canonical data 与 preview
     */
    CanonicalData validateCanonicalData(
            ChannelMessageBuildContext context,
            String domainVersion,
            Map<String, Object> data
    );

    /**
     * canonical 消息构建上下文。
     *
     * @param messageId 新消息 ID
     * @param channelId 目标频道 ID
     * @param senderId 发送者 ID
     * @param sendTime 服务端发送时间
     */
    record ChannelMessageBuildContext(long messageId, long channelId, long senderId, Instant sendTime) {
    }

    /**
     * 插件校验后的 canonical 内容。
     *
     * @param data 已校验和规范化的 domain 专属数据
     * @param preview 服务端派生摘要
     */
    record CanonicalData(Map<String, Object> data, String preview) {

        public CanonicalData {
            data = data == null
                    ? Map.of()
                    : Collections.unmodifiableMap(new LinkedHashMap<>(data));
            preview = preview == null ? "" : preview;
        }
    }
}
