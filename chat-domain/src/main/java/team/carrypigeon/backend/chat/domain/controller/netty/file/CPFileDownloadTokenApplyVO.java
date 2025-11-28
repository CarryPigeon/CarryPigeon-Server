package team.carrypigeon.backend.chat.domain.controller.netty.file;

import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;

/**
 * 请求下载文件 token 的参数。
 * 需要携带 fileId（即存储时的 sha256 作为 objectName）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPFileDownloadTokenApplyVO implements CPControllerVO {

    /**
     * 文件标识（sha256）
     */
    private String fileId;

    @Override
    public boolean insertData(DefaultContext context) {
        if (fileId == null || fileId.isEmpty()) {
            return false;
        }
        context.setData("FileInfo_Id", fileId);
        return true;
    }
}

