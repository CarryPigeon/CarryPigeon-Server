package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    List<Mention> persistMentions(ChannelMessage message, List<Long> recipientAccountIds, String previousMentionsJson) {
        List<Mention> mentions = buildMentions(message, recipientAccountIds, previousMentionsJson);
        for (Mention mention : mentions) {
            mentionRepository.save(mention);
        }
        return mentions;
    }

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
}
