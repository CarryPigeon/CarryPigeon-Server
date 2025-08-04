package team.carrypigeon.backend.chat.domain.manager.file;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件上传下载功能密钥管理器
 * */
@Component
public class CPFileTokenManager {
    private final Map<String, CPFileTokenData> filePermissionMap = new ConcurrentHashMap<>();

    public void addFilePermission(String token, CPFileTokenData cpFileTokenData) {
        filePermissionMap.put(token, cpFileTokenData);
    }

    public CPFileTokenData getFileTokenData(String token) {
        return filePermissionMap.remove(token);
    }
}
