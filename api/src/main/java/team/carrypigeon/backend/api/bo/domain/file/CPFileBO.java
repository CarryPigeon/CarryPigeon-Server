package team.carrypigeon.backend.api.bo.domain.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class CPFileBO {
    private long id;
    private String name;
    private String sha256;
    private long size;
    private long time;
}
