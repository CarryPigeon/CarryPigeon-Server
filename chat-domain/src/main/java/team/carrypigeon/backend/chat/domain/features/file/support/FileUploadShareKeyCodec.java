package team.carrypigeon.backend.chat.domain.features.file.support;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 受签名保护的上传 share_key 编解码器。
 * 职责：为通用文件上传生成不可伪造的稳定 share_key，并在后续访问时验证其完整性。
 * 边界：只处理通用上传文件，不处理附件 share_key 与保留系统对象键。
 */
public class FileUploadShareKeyCodec {

    private static final String SHARE_KEY_PREFIX = "shr_";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final byte[] signingSecret;

    public FileUploadShareKeyCodec(String signingSecret) {
        if (signingSecret == null || signingSecret.isBlank()) {
            throw new IllegalArgumentException("file upload signing secret must not be blank");
        }
        this.signingSecret = signingSecret.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 生成稳定上传 share_key。
     *
     * @param ownerAccountId 上传归属账户
     * @param fileId 文件 ID
     * @param declaredSizeBytes 客户端声明大小
     * @return 稳定对外 share_key
     */
    public String issue(long ownerAccountId, long fileId, long declaredSizeBytes) {
        String payload = ownerAccountId + ":" + fileId + ":" + declaredSizeBytes;
        return SHARE_KEY_PREFIX + base64Url(payload) + "." + sign(payload);
    }

    /**
     * 解析并校验 share_key。
     *
     * @param shareKey 对外 share_key
     * @return 已校验的上传 share_key 快照
     */
    public IssuedUploadShareKey parse(String shareKey) {
        if (shareKey == null || shareKey.isBlank() || !shareKey.startsWith(SHARE_KEY_PREFIX)) {
            throw invalidShareKey();
        }
        String encoded = shareKey.substring(SHARE_KEY_PREFIX.length());
        int dotIndex = encoded.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex >= encoded.length() - 1) {
            throw invalidShareKey();
        }
        String encodedPayload = encoded.substring(0, dotIndex);
        String signature = encoded.substring(dotIndex + 1);
        String payload = decodeBase64(encodedPayload);
        if (!constantTimeEquals(sign(payload), signature)) {
            throw invalidShareKey();
        }
        String[] segments = payload.split(":");
        if (segments.length != 3) {
            throw invalidShareKey();
        }
        try {
            long ownerAccountId = Long.parseLong(segments[0]);
            long fileId = Long.parseLong(segments[1]);
            long declaredSizeBytes = Long.parseLong(segments[2]);
            if (ownerAccountId <= 0 || fileId <= 0 || declaredSizeBytes <= 0) {
                throw invalidShareKey();
            }
            return new IssuedUploadShareKey(ownerAccountId, fileId, declaredSizeBytes);
        } catch (NumberFormatException exception) {
            throw invalidShareKey();
        }
    }

    /**
     * 为上传 share_key 推导 canonical object key。
     */
    public String objectKey(IssuedUploadShareKey shareKey) {
        return "files/accounts/" + shareKey.ownerAccountId() + "/" + shareKey.fileId();
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingSecret, HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw ProblemException.fail("file_share_key_sign_failed", "failed to sign file share key");
        }
    }

    private String base64Url(String payload) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private String decodeBase64(String encodedPayload) {
        try {
            return new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw invalidShareKey();
        }
    }

    private ProblemException invalidShareKey() {
        return ProblemException.validationFailed("invalid_share_key", "share_key is invalid");
    }

    private boolean constantTimeEquals(String left, String right) {
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
        if (leftBytes.length != rightBytes.length) {
            return false;
        }
        int result = 0;
        for (int index = 0; index < leftBytes.length; index++) {
            result |= leftBytes[index] ^ rightBytes[index];
        }
        return result == 0;
    }

    public record IssuedUploadShareKey(long ownerAccountId, long fileId, long declaredSizeBytes) {
    }
}
