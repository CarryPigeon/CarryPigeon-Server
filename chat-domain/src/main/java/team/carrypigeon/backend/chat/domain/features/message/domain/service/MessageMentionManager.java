package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.EditChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.Mention;
import team.carrypigeon.backend.chat.domain.features.message.domain.repository.MentionRepository;
import team.carrypigeon.backend.infrastructure.basic.id.IdGenerator;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.infrastructure.basic.time.TimeProvider;

/**
 * 消息 mention 协作对象。
 * 职责：规范化 mention 输入、按接收人过滤有效 mention、去重并持久化新 mention。
 * 边界：不负责消息保存、事务提交后发布或 mention 查询读侧能力。
 */
class MessageMentionManager {

    private final MentionRepository mentionRepository;
    private final IdGenerator idGenerator;
    private final JsonProvider jsonProvider;
    private final TimeProvider timeProvider;

    MessageMentionManager(
            MentionRepository mentionRepository,
            IdGenerator idGenerator,
            JsonProvider jsonProvider,
            TimeProvider timeProvider
    ) {
        this.mentionRepository = mentionRepository;
        this.idGenerator = idGenerator;
        this.jsonProvider = jsonProvider;
        this.timeProvider = timeProvider;
    }

    /**
     * 把编辑命令中的 mention 目标规范化为消息可持久化的 canonical JSON。
     * 输入：客户端提交的 mention 目标列表。
     * 输出：去重后的 mention JSON；输入为空时返回 null，表示消息不携带 mention。
     *
     * @param mentions mention 目标列表
     * @return 可写入消息记录的 mention JSON
     */
    String normalizeMentions(List<EditChannelMessageCommand.MentionTargetCommand> mentions) {
        if (mentions == null || mentions.isEmpty()) {
            return null;
        }
        List<Map<String, Object>> normalizedMentions = mentions.stream()
                .map(mention -> Map.<String, Object>of(
                        "type", mention.type(),
                        "uid", Long.toString(mention.uid())
                ))
                .distinct()
                .toList();
        return jsonProvider.toJson(normalizedMentions);
    }

    /**
     * 根据消息 mention JSON 创建新的 mention 记录。
     * 输入：已保存或待保存的消息、有效接收人账号集合和历史 mention JSON。
     * 输出：本次新增的 mention 记录。
     * 副作用：过滤发送者、非接收人和历史已存在 mention 后写入 mention 仓储。
     *
     * @param message 携带 mention JSON 的频道消息
     * @param recipientAccountIds 当前消息可触达的接收人账号 ID
     * @param previousMentionsJson 编辑前已存在的 mention JSON，用于避免重复通知
     * @return 本次新增并已持久化的 mention 记录
     */
    List<Mention> persistMentions(ChannelMessage message, List<Long> recipientAccountIds, String previousMentionsJson) {
        List<Mention> mentions = buildMentions(message, recipientAccountIds, previousMentionsJson);
        for (Mention mention : mentions) {
            mentionRepository.save(mention);
        }
        return mentions;
    }

    /**
     * 用消息当前 mention JSON 同步该消息的 mention 记录。
     * 语义：编辑消息时删除已移除 mention，保留仍存在 mention 的 ID 与已读状态，只返回本次新增 mention 用于实时通知。
     *
     * @param message 携带编辑后 mention JSON 的频道消息
     * @param recipientAccountIds 当前消息可触达的接收人账号 ID
     * @return 本次新增并已持久化的 mention 记录
     */
    List<Mention> replaceMentions(ChannelMessage message, List<Long> recipientAccountIds) {
        List<Mention> currentMentions = buildMentions(message, recipientAccountIds, null);
        List<Mention> persistedMentions = new ArrayList<>();
        List<Mention> newMentions = new ArrayList<>();
        for (Mention mention : currentMentions) {
            Mention existingMention = findExistingMention(mention);
            if (existingMention == null) {
                persistedMentions.add(mention);
                newMentions.add(mention);
            } else {
                persistedMentions.add(existingMention);
            }
        }
        mentionRepository.deleteByMessageId(message.messageId());
        for (Mention mention : persistedMentions) {
            mentionRepository.save(mention);
        }
        return newMentions;
    }

    /**
     * 删除指定消息关联的 mention 记录。
     * 副作用：从 mention 仓储移除该消息 ID 下的全部 mention，用于消息删除或重建 mention 关系。
     *
     * @param messageId 需要清理 mention 的消息 ID
     */
    void deleteByMessageId(long messageId) {
        mentionRepository.deleteByMessageId(messageId);
    }

    /**
     * 从消息 canonical mention JSON 中构建本次新增 mention。
     * 约束：只接受 user mention，过滤发送者自身、非接收人、非法 uid 和历史已存在 mention。
     *
     * @param message 携带 mention JSON 的频道消息
     * @param recipientAccountIds 当前消息可触达的接收人账号 ID
     * @param previousMentionsJson 编辑前已存在的 mention JSON
     * @return 本次需要新增的 mention 列表
     */
    private List<Mention> buildMentions(
            ChannelMessage message,
            List<Long> recipientAccountIds,
            String previousMentionsJson
    ) {
        if (message.mentions() == null || message.mentions().isBlank()) {
            return List.of();
        }
        Set<String> existingMentionKeys = mentionKeys(previousMentionsJson);
        Set<Long> validRecipients = new HashSet<>(recipientAccountIds);
        List<Mention> mentions = new java.util.ArrayList<>();
        for (Map<String, Object> item : jsonProvider.fromJson(message.mentions(), new TypeReference<List<Map<String, Object>>>() {
        })) {
            String type = item.get("type") == null ? null : String.valueOf(item.get("type")).trim();
            String uid = item.get("uid") == null ? null : String.valueOf(item.get("uid")).trim();
            if (!"user".equals(type) || uid == null || uid.isBlank()) {
                continue;
            }
            long targetAccountId;
            try {
                targetAccountId = Long.parseLong(uid);
            } catch (NumberFormatException exception) {
                continue;
            }
            if (targetAccountId == message.senderId() || !validRecipients.contains(targetAccountId)) {
                continue;
            }
            String mentionKey = type + ":" + targetAccountId;
            if (existingMentionKeys.contains(mentionKey)) {
                continue;
            }
            existingMentionKeys.add(mentionKey);
            mentions.add(new Mention(
                    idGenerator.nextLongId(),
                    message.channelId(),
                    message.messageId(),
                    message.senderId(),
                    type,
                    targetAccountId,
                    timeProvider.nowInstant(),
                    false
            ));
        }
        return mentions;
    }

    /**
     * 从已存在 mention JSON 中提取去重键。
     * 语义：键由 mention 类型和目标账号组成，用于编辑消息时避免重复创建同一 mention。
     *
     * @param mentionsJson 已持久化的 mention JSON
     * @return 已存在 mention 的去重键集合
     */
    private Set<String> mentionKeys(String mentionsJson) {
        Set<String> keys = new HashSet<>();
        if (mentionsJson == null || mentionsJson.isBlank()) {
            return keys;
        }
        for (Map<String, Object> item : jsonProvider.fromJson(mentionsJson, new TypeReference<List<Map<String, Object>>>() {
        })) {
            Object type = item.get("type");
            Object uid = item.get("uid");
            if (type != null && uid != null) {
                keys.add(String.valueOf(type).trim() + ":" + String.valueOf(uid).trim());
            }
        }
        return keys;
    }

    private Mention findExistingMention(Mention candidate) {
        return mentionRepository.listByAccountId(
                        candidate.targetAccountId(),
                        null,
                        Integer.MAX_VALUE,
                        false,
                        candidate.channelId()
                ).stream()
                .filter(mention -> mention.messageId() == candidate.messageId())
                .filter(mention -> mention.fromAccountId() == candidate.fromAccountId())
                .filter(mention -> Objects.equals(mention.targetType(), candidate.targetType()))
                .filter(mention -> mention.targetAccountId() == candidate.targetAccountId())
                .findFirst()
                .orElse(null);
    }
}
