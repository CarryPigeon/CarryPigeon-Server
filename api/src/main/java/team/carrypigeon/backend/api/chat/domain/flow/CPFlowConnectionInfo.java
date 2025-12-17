package team.carrypigeon.backend.api.chat.domain.flow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 一次 LiteFlow 执行过程中与底层连接相关的只读信息快照。
 * <p>
 * 该信息通常由连接层在会话创建时写入 {@code CPSession} 的属性，
 * 再由 {@link CPFlowContext} 在链路开始时读取并固定下来，供各个 Node 使用。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPFlowConnectionInfo {

    /**
     * 原始远端地址字符串，例如 "127.0.0.1:12345"。
     */
    private String remoteAddress;

    /**
     * 远端 IP 字符串，例如 "127.0.0.1"。
     */
    private String remoteIp;

    /**
     * 远端端口号。
     */
    private Integer remotePort;
}

