package team.carrypigeon.backend.chat.domain.cmp.api.file;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.file.CPFileInfo;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractResultNode;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeFileKeys;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.util.Map;

/**
 * 文件上传申请结果节点。
 * <p>
 * 将上下文中的上传凭据与文件标识组装为 API 返回结构。
 */
@Slf4j
@LiteflowComponent("ApiFileUploadApplyResult")
public class ApiFileUploadApplyResultNode extends AbstractResultNode<ApiFileUploadApplyResultNode.FileUploadApplyResponse> {

    private static final long DEFAULT_TTL_SEC = 300L;

    /**
     * 构建文件上传申请响应。
     */
    @Override
    protected FileUploadApplyResponse build(CPFlowContext context) {
        CPFileInfo info = requireContext(context, CPNodeFileKeys.FILE_INFO);
        String token = requireContext(context, CPNodeFileKeys.FILE_TOKEN);

        long expiresAt = TimeUtil.currentTimeMillis() + DEFAULT_TTL_SEC * 1000L;

        FileUploadApplyResponse resp = new FileUploadApplyResponse(
                Long.toString(info.getId()),
                info.getShareKey(),
                new UploadInfo("PUT",
                        "/api/files/upload/" + info.getId(),
                        Map.of("x-cp-upload-token", token),
                        expiresAt
                )
        );
        log.debug("ApiFileUploadApplyResult success, fileId={}", info.getId());
        return resp;
    }

    /**
     * 上传申请响应体。
     */
    public record FileUploadApplyResponse(String fileId, String shareKey, UploadInfo upload) {
    }

    /**
     * 上传凭据描述。
     */
    public record UploadInfo(String method, String url, Map<String, String> headers, long expiresAt) {
    }
}
