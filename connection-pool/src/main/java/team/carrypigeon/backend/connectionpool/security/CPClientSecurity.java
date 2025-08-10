package team.carrypigeon.backend.connectionpool.security;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 连接安全，用于标识用户当前加密状态和保存密钥私钥
 * TODO 对加密模块进行解耦
 * */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class CPClientSecurity {
    private CPClientSecurityEnum state = CPClientSecurityEnum.WAIT_ASYMMETRY;
    private String Key;
}
