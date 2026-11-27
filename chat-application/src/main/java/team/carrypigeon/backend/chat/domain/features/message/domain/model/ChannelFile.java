package team.carrypigeon.backend.chat.domain.features.message.domain.model;

import java.time.Instant;

public record ChannelFile(
    long id,
    String fileName,
    String fileType,
    String sha256,
    long size,
    Instant createdAt
) {
}
