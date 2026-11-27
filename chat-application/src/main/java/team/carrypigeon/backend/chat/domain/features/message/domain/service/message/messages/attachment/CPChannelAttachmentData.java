package team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages.attachment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelAttachmentData {
    private String shareKey;
    private String fileName;
    private long fileSize;
    private String fileType;
    private String sha256;
    private Instant createdAt;
}
