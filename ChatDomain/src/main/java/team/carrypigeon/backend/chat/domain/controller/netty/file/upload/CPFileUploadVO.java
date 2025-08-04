package team.carrypigeon.backend.chat.domain.controller.netty.file.upload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPFileUploadVO {
    private int size;
    private String name;
    private String sha256;
}