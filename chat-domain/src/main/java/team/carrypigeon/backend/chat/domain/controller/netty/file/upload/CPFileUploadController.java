package team.carrypigeon.backend.chat.domain.controller.netty.file.upload;

import cn.hutool.core.lang.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import team.carrypigeon.backend.api.bo.domain.CPSession;
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

@CPControllerTag("/core/file/download/apply")
public class CPFileUploadController implements CPController {

    private final ObjectMapper objectMapper;

    private final CPFileDAO cpFileDAO;

    private final CPFileTokenManager cpFilePermissionManager;

    public CPFileUploadController(ObjectMapper objectMapper, CPFileDAO cpFileDAO, CPFileTokenManager cpFilePermissionManager) {
        this.objectMapper = objectMapper;
        this.cpFileDAO = cpFileDAO;
        this.cpFilePermissionManager = cpFilePermissionManager;
    }

    @LoginPermission
    @SneakyThrows
    @Override
    public CPResponse process(JsonNode data, CPSession channel) {
        CPFileUploadVO cpFileUploadVO = objectMapper.treeToValue(data, CPFileUploadVO.class);
        CPFileBO fileBO = new CPFileBO()
                .setName(cpFileUploadVO.getName())
                .setSha256(cpFileUploadVO.getSha256())
                .setSize(cpFileUploadVO.getSize());
        // 判断是否存在文件
        CPFileBO fileBySha256AndSize = cpFileDAO.getFileBySha256AndSize(cpFileUploadVO.getSha256(), cpFileUploadVO.getSize());
        if (fileBySha256AndSize != null){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("file exist");
        }
        // 生成密钥并保存
        String key = UUID.randomUUID().toString();
        cpFilePermissionManager.addFilePermission(key, new CPFileTokenData(TimeUtil.getCurrentLocalTime(), fileBO));
        return CPResponse.SUCCESS_RESPONSE.copy().setData(JsonNodeUtil.createJsonNode("key", key));
    }
}