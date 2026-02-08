package team.carrypigeon.backend.chat.domain.cmp.basic;

import team.carrypigeon.backend.api.chat.domain.flow.CPKey;
import team.carrypigeon.backend.api.chat.domain.message.CPMessageData;
import team.carrypigeon.backend.chat.domain.cmp.info.PageInfo;

/**
 * LiteFlow 节点在自定义上下文（{@link team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext}，继承自 DefaultContext）
 * 中读写数据时使用的补充型 key 常量。
 * <ul>
 *   <li>消息解析辅助对象</li>
 *   <li>UserToken 相关上下文数据</li>
 *   <li>邮箱验证码相关上下文数据</li>
 *   <li>通用辅助对象（如 PageInfo）</li>
 * </ul>
 */
public final class CPNodeValueKeyExtraConstants {

    private CPNodeValueKeyExtraConstants() {
    }

    // -------- MessageData --------

    /** CPMessageData: 解析后的消息业务对象 */
    public static final CPKey<CPMessageData> MESSAGE_DATA = CPKey.of("MessageData", CPMessageData.class);


    // -------- 邮箱验证码相关 --------

    /** String: 用户邮箱地址 */
    public static final CPKey<String> EMAIL = CPKey.of("Email", String.class);

    /** Long: 邮箱验证码值 */
    public static final CPKey<Integer> EMAIL_CODE = CPKey.of("Email_Code", Integer.class);

    // -------- 频道成员扩展 --------

    /** Long: 目标成员 uid（用于部分需要同时校验自身成员身份和查询目标成员的链路） */
    public static final CPKey<Long> TARGET_MEMBER_UID = CPKey.of("TargetMember_Uid", Long.class);

    // -------- 通用辅助对象 --------

    /** PageInfo: 分页信息对象 */
    public static final CPKey<PageInfo> PAGE_INFO = CPKey.of("PageInfo", PageInfo.class);
}
