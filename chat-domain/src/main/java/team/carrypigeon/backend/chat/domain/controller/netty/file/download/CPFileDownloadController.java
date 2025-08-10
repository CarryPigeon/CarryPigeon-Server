package team.carrypigeon.backend.chat.domain.controller.netty.file.download;

import cn.hutool.core.lang.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import team.carrypigeon.backend.api.bo.domain.CPChannel;
import team.carrypigeon.backend.api.bo.domain.file.CPFileBO;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.vo.CPResponse;
import team.carrypigeon.backend.api.dao.file.CPFileDAO;
import team.carrypigeon.backend.chat.domain.manager.file.CPFileTokenData;
import team.carrypigeon.backend.chat.domain.manager.file.CPFileTokenManager;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.common.json.JsonNodeUtil;
import team.carrypigeon.backend.common.time.TimeUtil;

@CPControllerTag("/core/file/upload/apply")
public class CPFileDownloadController implements CPController {

    private final ObjectMapper objectMapper;

    private final CPFileDAO cpFileDAO;

    private final CPFileTokenManager cpFilePermissionManager;

    public CPFileDownloadController(ObjectMapper objectMapper, CPFileDAO cpFileDAO, CPFileTokenManager cpFilePermissionManager) {
        this.objectMapper = objectMapper;
        this.cpFileDAO = cpFileDAO;
        this.cpFilePermissionManager = cpFilePermissionManager;
    }

    @LoginPermission
    @SneakyThrows
    @Override
    public CPResponse process(JsonNode data, CPChannel channel) {
        CPFileDownloadVO cpFileUploadVO = objectMapper.treeToValue(data, CPFileDownloadVO.class);
        CPFileBO fileBO = cpFileDAO.getFileById(cpFileUploadVO.getFid());
        if (fileBO == null) return CPResponse.ERROR_RESPONSE.copy().setTextData("no such file exist");
        // 生成密钥并保存
        String key = UUID.randomUUID().toString();
        cpFilePermissionManager.addFilePermission(key, new CPFileTokenData(TimeUtil.getCurrentLocalTime(), fileBO));
        return CPResponse.SUCCESS_RESPONSE.copy().setData(JsonNodeUtil.createJsonNode("key", key));
    }
}