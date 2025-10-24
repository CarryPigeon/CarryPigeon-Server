package team.carrypigeon.backend.api.bo.domain.channel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPChannel {
    // 通道id
    private long id;
    //  通道名
    private String name;
    // 通道所有者
    private long owner;
    // 通道简介
    private String brief;
    // 通道头像资源id
    private long avatar;
    // 通道创建时间
    private LocalDateTime createTime;
}