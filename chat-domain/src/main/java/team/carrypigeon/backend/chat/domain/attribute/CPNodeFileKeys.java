package team.carrypigeon.backend.chat.domain.attribute;

/**
 * 文件相关 FileInfo_* / FileToken 相关 key。
 */
public final class CPNodeFileKeys {

    public static final String FILE_INFO_ID = "FileInfo_Id";
    /** String: 文件内容 sha256 摘要（hex） */
    public static final String FILE_INFO_SHA256 = "FileInfo_Sha256";
    /** Long: 文件大小（字节） */
    public static final String FILE_INFO_SIZE = "FileInfo_Size";
    /** String: 存储系统中的对象名 */
    public static final String FILE_INFO_OBJECT_NAME = "FileInfo_ObjectName";
    /** String: 内容类型 mime-type */
    public static final String FILE_INFO_CONTENT_TYPE = "FileInfo_ContentType";
    /** Long: 记录创建时间（毫秒时间戳） */
    public static final String FILE_INFO_CREATE_TIME = "FileInfo_CreateTime";
    public static final String FILE_TOKEN = "FileToken";

    private CPNodeFileKeys() {
    }
}
