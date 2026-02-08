package team.carrypigeon.backend.api.bo.domain.channel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 频道（Channel）领域对象。
 * <p>
 * 该对象描述频道的基础资料（名称/简介/头像/创建时间等），不包含成员关系与权限信息。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPChannel {
    /**
     * 频道 ID。
     */
    private long id;
    /**
     * 频道名称。
     */
    private String name;
    /**
     * 频道所有者用户 ID。
     */
    private long owner;
    /**
     * 频道简介（可为空）。
     */
    private String brief;
    /**
     * 频道头像资源 ID（如 fileId / resourceId）。
     */
    private long avatar;
    /**
     * 频道创建时间。
     */
    private LocalDateTime createTime;
}
