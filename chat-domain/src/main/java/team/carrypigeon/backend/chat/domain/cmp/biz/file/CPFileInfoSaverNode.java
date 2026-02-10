package team.carrypigeon.backend.chat.domain.cmp.biz.file;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.file.CPFileInfo;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemReason;
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

    /**
     * 执行当前节点的核心处理逻辑。
     *
     * @param context LiteFlow 上下文，读取文件实体并持久化元数据
     * @throws Exception 执行过程中抛出的异常
     */
    @Override
    protected void process(CPFlowContext context) throws Exception {
        CPFileInfo info = requireContext(context, CPNodeFileKeys.FILE_INFO);
        boolean success = fileInfoDao.save(info);
        if (!success) {
            log.error("CPFileInfoSaver failed, fileId={}", info.getId());
            fail(CPProblem.of(CPProblemReason.INTERNAL_ERROR, "failed to save file info"));
        }
        log.debug("CPFileInfoSaver success, fileId={}", info.getId());
    }
}

