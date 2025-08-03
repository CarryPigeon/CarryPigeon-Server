package team.carrypigeon.backend.chat.domain.manager.email;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import team.carrypigeon.backend.api.connection.vo.CPResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CPEmailCodeManager {
    private final Map<String,CPEmailCode> mapper = new ConcurrentHashMap<>();

    public CPEmailCode getCode(String email) {
        return mapper.get(email);
    }

    public void addCode(String email, CPEmailCode code) {
        mapper.put(email, code);
    }

    // 定期清理过期的验证码
    @Scheduled(fixedRate = 1000 * 60 * 5)
    public void clean() {
        for (Map.Entry<String, CPEmailCode> entry : mapper.entrySet()) {
            if (entry.getValue().isValid()){
                mapper.remove(entry.getKey());
            }
        }
    }

    public boolean verify(String email, int code) {
        CPEmailCode emailCode = getCode(email);
        return emailCode != null && !emailCode.isValid() && emailCode.getCode() == code;
    }

}
