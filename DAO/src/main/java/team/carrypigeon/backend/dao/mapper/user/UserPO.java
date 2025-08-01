package team.carrypigeon.backend.dao.mapper.user;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.user.CPUserBO;

import java.time.LocalDateTime;
import java.time.ZoneId;

@TableName("user")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class UserPO {
    @TableId
    private Long id;
    private String name;
    private String email;
    private String password;
    private Long profile;
    private Integer authority;
    private String introduction;
    private LocalDateTime registerTime;
    private Long stateId;

    public CPUserBO toUserBO() {
        CPUserBO userBO = new CPUserBO();
        userBO.setId(id);
        userBO.setName(name);
        userBO.setEmail(email);
        userBO.setRegisterTime(registerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        userBO.setStateId(stateId);
        userBO.setIntroduction(introduction);
        userBO.setProfile(profile);
        userBO.setAuthority(authority);
        return userBO;
    }

    public void fillData(CPUserBO userBO,String password) {
        id = userBO.getId();
        name = userBO.getName();
        email = userBO.getEmail();
        if(userBO.getRegisterTime()!=0){
            registerTime =  LocalDateTimeUtil.of(userBO.getRegisterTime());
        }
        stateId = userBO.getStateId();
        this.password = password;
        introduction = userBO.getIntroduction();
        profile = userBO.getProfile();
        authority = userBO.getAuthority();

    }
}
