package team.carrypigeon.backend.api.bo.domain.file;

import lombok.extern.slf4j.Slf4j;

/**
 * 文件下载访问范围枚举。
 * <p>
 * 该范围决定 `GET /api/files/download/{share_key}` 的可访问人群，
 * 持久化时对应 `file_info.access_scope` 字段。
 */
@Slf4j
public enum CPFileAccessScopeEnum {
    /**
     * 仅上传者本人可下载。
     */
    OWNER,

    /**
     * 任意已登录用户可下载。
     * <p>
     * 常用于应用内头像等公共资源。
     */
    AUTH,

    /**
     * 仅目标频道成员可下载。
     * <p>
     * 使用该范围时通常要求 `file_info.scope_cid > 0`。
     */
    CHANNEL,

    /**
     * 全部用户可下载（包含匿名访问）。
     */
    PUBLIC;

    /**
     * 解析访问范围字符串。
     * <p>
     * 当值为空或非法时回落到 {@link #OWNER}，避免脏数据导致整条请求失败。
     *
     * @param value 数据库存储的访问范围字符串
     * @return 合法范围；非法值返回 {@link #OWNER}
     */
    public static CPFileAccessScopeEnum parseOrDefault(String value) {
        if (value == null || value.isBlank()) {
            return OWNER;
        }
        try {
            return CPFileAccessScopeEnum.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            log.error("File access scope parse failed, value={}", value);
            return OWNER;
        }
    }
}
