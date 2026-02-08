package team.carrypigeon.backend.chat.domain.attribute;

import team.carrypigeon.backend.api.bo.domain.file.CPFileInfo;
import team.carrypigeon.backend.api.chat.domain.flow.CPKey;

/**
 * File related context keys (FileInfo_* / FileToken).
 * <p>
 * These keys are used by HTTP file upload/download chains and related nodes.
 */
public final class CPNodeFileKeys {

    /** {@code CPFileInfo}: 文件元信息实体。 */
    public static final CPKey<CPFileInfo> FILE_INFO = CPKey.of("FileInfo", CPFileInfo.class);

    /** {@code String}: 文件 id（十进制字符串）。 */
    public static final CPKey<String> FILE_INFO_ID = CPKey.of("FileInfo_Id", String.class);
    /** {@code String}: 对外 share key（用于下载）。 */
    public static final CPKey<String> FILE_INFO_SHARE_KEY = CPKey.of("FileInfo_ShareKey", String.class);
    /** {@code Long}: 文件 owner uid（上传者）。 */
    public static final CPKey<Long> FILE_INFO_OWNER_UID = CPKey.of("FileInfo_OwnerUid", Long.class);
    /** {@code String}: 原始文件名（可为空）。 */
    public static final CPKey<String> FILE_INFO_FILENAME = CPKey.of("FileInfo_Filename", String.class);
    /** String: 文件内容 sha256 摘要（hex） */
    public static final CPKey<String> FILE_INFO_SHA256 = CPKey.of("FileInfo_Sha256", String.class);
    /** Long: 文件大小（字节） */
    public static final CPKey<Long> FILE_INFO_SIZE = CPKey.of("FileInfo_Size", Long.class);
    /** String: 存储系统中的对象名 */
    public static final CPKey<String> FILE_INFO_OBJECT_NAME = CPKey.of("FileInfo_ObjectName", String.class);
    /** String: 内容类型 mime-type */
    public static final CPKey<String> FILE_INFO_CONTENT_TYPE = CPKey.of("FileInfo_ContentType", String.class);
    /** {@code Boolean}: 是否已上传完成。 */
    public static final CPKey<Boolean> FILE_INFO_UPLOADED = CPKey.of("FileInfo_Uploaded", Boolean.class);
    /** Long: 记录创建时间（毫秒时间戳） */
    public static final CPKey<Long> FILE_INFO_CREATE_TIME = CPKey.of("FileInfo_CreateTime", Long.class);
    /** {@code String}: 一次性文件操作 token（用于 HTTP 上传/下载授权）。 */
    public static final CPKey<String> FILE_TOKEN = CPKey.of("FileToken", String.class);

    private CPNodeFileKeys() {
    }
}
