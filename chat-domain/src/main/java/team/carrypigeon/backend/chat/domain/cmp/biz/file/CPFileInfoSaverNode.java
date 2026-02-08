package team.carrypigeon.backend.chat.domain.cmp.biz.file;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.file.CPFileInfo;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.api.dao.database.file.FileInfoDao;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeFileKeys;

/**
 * Persist {@link CPFileInfo} metadata.
 * <p>
 * Input: {@link CPNodeFileKeys#FILE_INFO}
 */
@Slf4j
@AllArgsConstructor
@LiteflowComponent("CPFileInfoSaver")
public class CPFileInfoSaverNode extends CPNodeComponent {

    private final FileInfoDao fileInfoDao;

    @Override
    protected void process(CPFlowContext context) throws Exception {
        CPFileInfo info = requireContext(context, CPNodeFileKeys.FILE_INFO);
        boolean success = fileInfoDao.save(info);
        if (!success) {
            log.error("CPFileInfoSaver failed, fileId={}", info.getId());
            fail(CPProblem.of(500, "internal_error", "failed to save file info"));
        }
        log.debug("CPFileInfoSaver success, fileId={}", info.getId());
    }
}

