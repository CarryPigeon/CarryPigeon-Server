package team.carrypigeon.backend.chat.domain.features.auth.domain.api;

import team.carrypigeon.backend.chat.domain.features.auth.domain.command.RegisterCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.SendEmailCodeCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.RegisterResult;

/**
 * 鉴权账号领域 API。
 * 职责：暴露账号注册与邮箱验证码等账号入口能力。
 * 边界：不暴露会话刷新、注销和 token 签发细节。
 * 输入：账号注册与验证码发送命令对象。
 * 输出：注册结果投影或验证码发送副作用。
 * 失败语义：参数校验、邮箱验证码、账号唯一性等问题由领域问题异常表达。
 * 调用方：controller 或其它 feature 只能依赖本接口，不直接依赖具体实现类。
 */
public interface AuthAccountApi {

    /**
     * 注册新账号并初始化账号关联资料。
     * 输入：注册命令包含邮箱、验证码、用户名和原始密码等注册资料。
     * 输出：注册成功后的账号标识、用户名和初始化结果投影。
     * 约束：验证码必须有效，邮箱和用户名必须满足账号唯一性规则。
     *
     * @param command 账号注册业务命令
     * @return 注册完成后的账号结果投影
     */
    RegisterResult register(RegisterCommand command);

    /**
     * 为账号注册或验证流程发送邮箱验证码。
     * 输入：验证码发送命令包含目标邮箱与验证码使用场景。
     * 副作用：生成验证码并交给验证码投递能力处理。
     * 约束：调用方不应依赖验证码存储、过期和投递实现细节。
     *
     * @param command 邮箱验证码发送业务命令
     */
    void sendEmailCode(SendEmailCodeCommand command);
}
