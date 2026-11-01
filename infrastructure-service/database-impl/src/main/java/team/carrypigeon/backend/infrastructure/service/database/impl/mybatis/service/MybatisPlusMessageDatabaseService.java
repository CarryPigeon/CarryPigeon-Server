package team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import team.carrypigeon.backend.infrastructure.basic.page.PageQuery;
import team.carrypigeon.backend.infrastructure.basic.page.PageResult;
import team.carrypigeon.backend.infrastructure.service.database.api.model.MessageRecord;
import team.carrypigeon.backend.infrastructure.service.database.api.service.MessageDatabaseService;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.entity.MessageEntity;
import team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.mapper.MessageMapper;

/**
 * MyBatis-Plus 消息数据库服务。
 * 职责：在 database-impl 中完成消息表级读写与专用搜索查询。
 * 边界：只负责持久化映射和数据谓词，不承载消息业务规则。
 */
@Component
@ConditionalOnProperty(prefix = "cp.infrastructure.service.database", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MybatisPlusMessageDatabaseService implements MessageDatabaseService {

    private final MessageMapper messageMapper;

    public MybatisPlusMessageDatabaseService(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    /**
     * 插入新消息记录。
     */
    @Override
    public void insert(MessageRecord record) {
        messageMapper.insert(MessageEntity.from(record));
    }

    /**
     * 批量插入消息记录。
     */
    @Override
    public void insertAll(List<MessageRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        messageMapper.insertAll(records.stream().map(MessageEntity::from).toList());
    }

    /**
     * 按消息 ID 删除记录。
     */
    @Override
    public void delete(long messageId) {
        messageMapper.deleteById(messageId);
    }

    /**
     * 按消息 ID 批量删除记录。
     */
    @Override
    public void deleteAll(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }
        messageMapper.deleteByIds(messageIds);
    }

    /**
     * 分页查询消息记录。
     */
    @Override
    public PageResult<MessageRecord> list(PageQuery query) {
        IPage<MessageEntity> result = messageMapper.selectPage(
                new Page<>(query.page(), query.pageSize(), true),
                new LambdaQueryWrapper<MessageEntity>()
                        .orderByAsc(MessageEntity::getMessageId)
        );
        return new PageResult<>(
                result.getRecords().stream().map(MessageEntity::toRecord).toList(),
                query.page(),
                query.pageSize(),
                result.getTotal()
        );
    }

    /**
     * 按消息 ID 批量查询记录。
     */
    @Override
    public List<MessageRecord> findByIds(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return List.of();
        }
        return messageMapper.selectByIds(messageIds).stream()
                .map(MessageEntity::toRecord)
                .toList();
    }

    /**
     * 按消息 ID 查询记录。
     */
    @Override
    public Optional<MessageRecord> findById(long messageId) {
        return Optional.ofNullable(messageMapper.selectById(messageId))
                .map(MessageEntity::toRecord);
    }

    /**
     * 更新完整消息记录。
     * 边界：字段变化由 Repository 决定，本层不筛选列。
     */
    @Override
    public void update(MessageRecord record) {
        messageMapper.updateById(MessageEntity.from(record));
    }

    /**
     * 查询频道最新消息。
     */
    @Override
    public List<MessageRecord> findLatestByChannelId(long channelId, int limit) {
        return selectLimited(new LambdaQueryWrapper<MessageEntity>()
                .eq(MessageEntity::getChannelId, channelId)
                .orderByDesc(MessageEntity::getMessageId), limit);
    }

    /**
     * 查询频道内指定消息之前的更旧消息。
     */
    @Override
    public List<MessageRecord> findBeforeByChannelId(long channelId, long cursorMessageId, int limit) {
        return selectLimited(new LambdaQueryWrapper<MessageEntity>()
                .eq(MessageEntity::getChannelId, channelId)
                .lt(MessageEntity::getMessageId, cursorMessageId)
                .orderByDesc(MessageEntity::getMessageId), limit);
    }

    /**
     * 查询频道内指定消息之后的更新消息。
     */
    @Override
    public List<MessageRecord> findAfterByChannelId(long channelId, long cursorMessageId, int limit) {
        return selectLimited(new LambdaQueryWrapper<MessageEntity>()
                .eq(MessageEntity::getChannelId, channelId)
                .gt(MessageEntity::getMessageId, cursorMessageId)
                .orderByAsc(MessageEntity::getMessageId), limit);
    }

    /**
     * 按复合过滤条件搜索频道消息。
     * 说明：搜索是专用查询；status 由 Repository 传入，不在本层硬编码。
     */
    @Override
    public List<MessageRecord> searchByChannelId(
            long channelId,
            String keyword,
            Long cursorMessageId,
            Long senderAccountId,
            String domain,
            String status,
            int limit
    ) {
        return selectLimited(new LambdaQueryWrapper<MessageEntity>()
                .eq(MessageEntity::getChannelId, channelId)
                .eq(status != null && !status.isBlank(), MessageEntity::getStatus, status)
                .and(wrapper -> wrapper
                        .like(MessageEntity::getPreview, keyword)
                        .or()
                        .apply("CAST(data AS CHAR) LIKE CONCAT('%', {0}, '%')", keyword))
                .lt(cursorMessageId != null, MessageEntity::getMessageId, cursorMessageId)
                .eq(senderAccountId != null, MessageEntity::getSenderId, senderAccountId)
                .eq(domain != null && !domain.isBlank(), MessageEntity::getDomain, domain)
                .orderByDesc(MessageEntity::getMessageId), limit);
    }

    /**
     * 使用 MyBatis-Plus 分页能力执行固定上限查询。
     * 说明：关闭 count 查询，仅使用分页拦截器生成 LIMIT。
     */
    private List<MessageRecord> selectLimited(
            LambdaQueryWrapper<MessageEntity> wrapper,
            int limit
    ) {
        IPage<MessageEntity> page = messageMapper.selectPage(
                new Page<>(1, limit, false),
                wrapper
        );
        return page.getRecords().stream()
                .map(MessageEntity::toRecord)
                .toList();
    }
}
