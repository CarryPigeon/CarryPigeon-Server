package team.carrypigeon.backend.chat.domain.controller.netty.user.profile.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.user.CPUser;
import team.carrypigeon.backend.api.bo.domain.user.CPUserSexEnum;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.notification.CPNotification;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.api.dao.database.user.UserDao;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.chat.domain.service.notification.CPNotificationService;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * 更新用户信息控制器<br/>
 * 请求url:/core/user/profile/update<br/>
 * 请求参数:{@link CPUserUpdateProfileVO}<br/>
 * 响应参数:无<br/>
 * */
@CPControllerTag(
        path = "/core/user/profile/update",
        clazz = CPUserUpdateProfileVO.class
)
public class CPUserUpdateProfileController{}