package team.carrypigeon.backend.dao.mapper.user;

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

@TableName("user")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class UserPO {
    @TableId
    private long id;
    private String name;
    private String email;
    private String password;
    private String data;
    private LocalDateTime registerTime;
    private long stateId;

    public CPUserBO toUserBO(ObjectMapper objectMapper) {
        CPUserBO userBO = new CPUserBO();
        userBO.setId(id);
        userBO.setName(name);
        userBO.setEmail(email);
        userBO.setRegisterTime(registerTime);
        userBO.setStateId(stateId);
        try {
            userBO.setData(objectMapper.readValue(data, JsonNode.class));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(),e);
        }
        return userBO;
    }

    public void fillData(CPUserBO userBO,String password,ObjectMapper objectMapper) {
        id = userBO.getId();
        name = userBO.getName();
        email = userBO.getEmail();
        registerTime = userBO.getRegisterTime();
        stateId = userBO.getStateId();
        this.password = password;
        try {
            data = objectMapper.writeValueAsString(userBO.getData());
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(),e);
        }
    }
}
