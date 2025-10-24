package team.carrypigeon.backend.api.dao.database.file;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.bo.domain.file.CPFileBO;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("file")
public class FilePO {
    private Long id;
    private String name;
    private String sha256;
    private Long size;
    private LocalDateTime time;

    public void fill(CPFileBO fileBO){
        this.id = fileBO.getId();
        this.name = fileBO.getName();
        this.sha256 = fileBO.getSha256();
        this.size = fileBO.getSize();
        this.time = TimeUtil.MillisToLocalDateTime(fileBO.getTime());
    }

    public CPFileBO toBO(){
        return new CPFileBO(this.id, this.name, this.sha256, this.size, TimeUtil.LocalDateTimeToMillis(this.time));
    }
}
