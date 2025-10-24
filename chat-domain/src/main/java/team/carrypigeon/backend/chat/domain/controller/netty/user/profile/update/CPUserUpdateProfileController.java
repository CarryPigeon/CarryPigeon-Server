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
@CPControllerTag("/core/user/profile/update")
public class CPUserUpdateProfileController implements CPController {

    private final ObjectMapper objectMapper;
    private final UserDao userDao;
    private final ChannelMemberDao channelMemberDao;
    private final CPNotificationService cpNotificationService;

    public CPUserUpdateProfileController(ObjectMapper objectMapper, UserDao userDao, ChannelMemberDao channelMemberDao, CPNotificationService cpNotificationService) {
        this.objectMapper = objectMapper;
        this.userDao = userDao;
        this.channelMemberDao = channelMemberDao;
        this.cpNotificationService = cpNotificationService;
    }

    @Override
    @LoginPermission
    public CPResponse process(JsonNode data, CPSession session) {
        // 信息解析
        CPUserUpdateProfileVO cpUserUpdateProfileVO;
        try {
            cpUserUpdateProfileVO = objectMapper.treeToValue(data, CPUserUpdateProfileVO.class);
        } catch (Exception e) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error parsing request data");
        }
        // 查询数据
        long uid = session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class);
        CPUser user = userDao.getById(uid);
        if (user == null){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("user not exists");
        }
        // 修改数据
        user.setUsername(cpUserUpdateProfileVO.getUsername())
                .setAvatar(cpUserUpdateProfileVO.getAvatar())
                .setSex(CPUserSexEnum.valueOf(cpUserUpdateProfileVO.getSex()))
                .setBrief(cpUserUpdateProfileVO.getBrief())
                .setBirthday(TimeUtil.MillisToLocalDateTime(cpUserUpdateProfileVO.getBirthday()));
        // 更新数据
        if (!userDao.save(user)){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error updating user");
        }

        // 构建待通知人
        Set<Long> uids = new HashSet<>();
        CPChannelMember[] allMemberByUserId = channelMemberDao.getAllMemberByUserId(uid);
        for (CPChannelMember cpChannelMember : allMemberByUserId) {
            CPChannelMember[] allMember = channelMemberDao.getAllMember(cpChannelMember.getCid());
            for (CPChannelMember member : allMember) {
                uids.add(member.getUid());
            }
        }

        // 构建通知包
        CPNotification notification = new CPNotification();
        notification.setRoute("/core/user/profile/get");

        // 调用通知接口
        cpNotificationService.sendNotification(uids, notification);

        return CPResponse.SUCCESS_RESPONSE.copy();
    }
}