package team.carrypigeon.backend.starter.config.openapi;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.RequestBody;
import java.util.Map;

/**
 * OpenAPI 请求示例目录。
 * 职责：按 HTTP 方法与模板路径为关键写接口补充可导入的请求示例。
 * 边界：只维护文档样例，不参与请求解析和参数校验。
 */
final class OpenApiRequestExamples {

    private static final String JSON_MEDIA_TYPE = "application/json";
    private static final String MULTIPART_FORM_DATA_TYPE = "multipart/form-data";
    private static final String OCTET_STREAM_TYPE = "application/octet-stream";
    private static final Map<String, RequestExample> REQUEST_EXAMPLES = Map.ofEntries(
            Map.entry("POST /api/auth/login", jsonExample("测试账号登录", """
                    {
                      "username": "carry-owner",
                      "password": "carrypigeon123"
                    }
                    """)),
            Map.entry("POST /api/auth/register", jsonExample("用户名密码注册", """
                    {
                      "username": "carry-user",
                      "password": "carrypigeon123"
                    }
                    """)),
            Map.entry("POST /api/auth/email_codes", jsonExample("发送邮箱验证码", """
                    {
                      "email": "carry-user@example.test"
                    }
                    """)),
            Map.entry("POST /api/auth/tokens", jsonExample("邮箱验证码登录", """
                    {
                      "grant_type": "email_code",
                      "email": "carry-user@example.test",
                      "code": "123456",
                      "client": {
                        "device_id": "apifox-device-1",
                        "installed_plugins": []
                      }
                    }
                    """)),
            Map.entry("POST /api/auth/refresh", jsonExample("刷新访问令牌", """
                    {
                      "refresh_token": "{{refreshToken}}",
                      "client": {
                        "device_id": "apifox-device-1"
                      }
                    }
                    """)),
            Map.entry("POST /api/auth/revoke", jsonExample("撤销刷新令牌", """
                    {
                      "refresh_token": "{{refreshToken}}",
                      "client": {
                        "device_id": "apifox-device-1"
                      }
                    }
                    """)),
            Map.entry("POST /api/gates/required/check", jsonExample("required gate 预检查", """
                    {
                      "client": {
                        "device_id": "apifox-device-1",
                        "installed_plugins": []
                      }
                    }
                    """)),
            Map.entry("PUT /api/users/me/email", jsonExample("更新邮箱", """
                    {
                      "email": "carry-user@example.test",
                      "code": "123456"
                    }
                    """)),
            Map.entry("PATCH /api/users/me", jsonExample("更新当前用户资料", """
                    {
                      "username": "carry-owner",
                      "avatar": "avatars/users/carry-owner.png",
                      "brief": "Updated from Apifox",
                      "sex": 0,
                      "birthday": 0
                    }
                    """)),
            Map.entry("POST /api/users/me/background", formExample(
                    "上传用户背景图", Map.of("background", "<select image file>"))),
            Map.entry("POST /api/files/uploads", jsonExample("申请文件上传", """
                    {
                      "filename": "apifox-note.txt",
                      "mime_type": "text/plain",
                      "size_bytes": 128
                    }
                    """)),
            Map.entry("PUT /api/files/uploads/{shareKey}", binaryExample(
                    "上传文件内容", "<raw file content>")),
            Map.entry("POST /api/channels", jsonExample("创建频道", """
                    {
                      "name": "apifox-channel",
                      "brief": "Created from Apifox",
                      "avatar": "avatars/channels/apifox-channel.png"
                    }
                    """)),
            Map.entry("PATCH /api/channels/{channelId}", jsonExample("更新频道资料", """
                    {
                      "name": "project-alpha",
                      "brief": "Updated from Apifox"
                    }
                    """)),
            Map.entry("PUT /api/channels/{channelId}/bans/{targetAccountId}", jsonExample("禁言频道成员", """
                    {
                      "reason": "spam",
                      "until": 1893456000000
                    }
                    """)),
            Map.entry("PUT /api/channels/{channelId}/notification_preference", jsonExample("更新频道通知偏好", """
                    {
                      "mode": "inherit",
                      "muted_until": 0
                    }
                    """)),
            Map.entry("POST /api/channels/{channelId}/applications", jsonExample("申请加入频道", """
                    {
                      "reason": "I want to join this channel"
                    }
                    """)),
            Map.entry("POST /api/channels/{channelId}/applications/{applicationId}/decisions", jsonExample("审批入群申请", """
                    {
                      "decision": "approve"
                    }
                    """)),
            Map.entry("POST /api/channels/{channelId}/messages", jsonExample("发送文本消息", """
                    {
                      "domain": "Core:Text",
                      "domain_version": "1.0.0",
                      "data": {
                        "text": "hello from Apifox"
                      },
                      "mentions": ["{{accountId}}"],
                      "client_message_id": "apifox-msg-001"
                    }
                    """)),
            Map.entry("POST /api/channels/{channelId}/messages/attachments", formExample(
                    "上传消息附件",
                    Map.of("message_type", "file", "file", "<select attachment file>"))),
            Map.entry("POST /api/channels/{channelId}/pins/{messageId}", jsonExample("置顶频道消息", """
                    {
                      "note": "Important message"
                    }
                    """)),
            Map.entry("POST /api/messages/{messageId}/forward", jsonExample("合并转发消息", """
                    {
                      "target_cid": "{{channelId}}",
                      "comment": "FYI",
                      "merged_mids": ["{{messageId}}", "{{secondMessageId}}"],
                      "idempotency_key": "apifox-forward-001"
                    }
                    """)),
            Map.entry("PUT /api/channels/{channelId}/read_state", jsonExample("更新频道已读状态", """
                    {
                      "last_read_mid": "{{messageId}}",
                      "last_read_time": 1700000000000
                    }
                    """)),
            Map.entry("PUT /api/mentions/read_state", jsonExample("批量标记提及已读", """
                    {
                      "before_mention_id": "{{mentionId}}",
                      "cid": "{{channelId}}"
                    }
                    """)),
            Map.entry("PUT /api/notification_preferences/server", jsonExample("更新服务通知偏好", """
                    {
                      "mode": "all",
                      "muted_until": null
                    }
                    """))
    );

    private OpenApiRequestExamples() {
    }

    static void ensureRequestExample(Operation operation, String method, String path) {
        RequestExample requestExample = REQUEST_EXAMPLES.get(method + " " + path);
        if (requestExample == null) {
            return;
        }
        RequestBody requestBody = operation.getRequestBody();
        if (requestBody == null) {
            requestBody = new RequestBody();
            operation.setRequestBody(requestBody);
        }
        if (requestBody.getContent() == null) {
            requestBody.setContent(new Content());
        }
        MediaType mediaType = requestBody.getContent().get(requestExample.mediaType());
        if (mediaType == null) {
            mediaType = new MediaType();
            requestBody.getContent().addMediaType(requestExample.mediaType(), mediaType);
        }
        if (mediaType.getExamples() == null || !mediaType.getExamples().containsKey(requestExample.name())) {
            mediaType.addExamples(requestExample.name(), new Example()
                    .description(requestExample.name())
                    .summary(requestExample.name())
                    .value(requestExample.value()));
        }
    }

    private static RequestExample jsonExample(String name, String value) {
        return new RequestExample(name, JSON_MEDIA_TYPE, value);
    }

    private static RequestExample formExample(String name, Map<String, String> value) {
        return new RequestExample(name, MULTIPART_FORM_DATA_TYPE, value);
    }

    private static RequestExample binaryExample(String name, String value) {
        return new RequestExample(name, OCTET_STREAM_TYPE, value);
    }

    private record RequestExample(String name, String mediaType, Object value) {
    }
}
