package team.carrypigeon.backend.chat.domain.service.file;

import io.minio.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import team.carrypigeon.backend.api.bo.domain.file.CPFileBO;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

@Service
public class FileService {

    private final MinioClient minioClient;

    @Value("${minio.bucketName}")
    private String bucketName;

    public FileService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * 流式文件上传
     * */
    public boolean uploadFile(InputStream steam, CPFileBO file) throws Exception {
        if (file == null || file.getName().isEmpty()) {
            throw new IllegalArgumentException("对象名称不能为空");
        }
        // 创建SHA256摘要
        MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
        // 使用DigestInputStream包装文件输入流
        try (DigestInputStream digestInputStream = new DigestInputStream(
                steam, sha256Digest)) {
            // 上传文件
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(file.getId() + ".meta")
                            .stream(digestInputStream, file.getSize(), -1)
                            .build()
            );
            // 计算SHA256值
            byte[] digest = sha256Digest.digest();
            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            if (!hexString.toString().equals(file.getSha256())) {
                deleteFile(file.getId() + ".meta");
                return false;
            }
            return true;
        }
    }

    /**
     * 下载文件
     */
    public InputStream downloadFile(String objectName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }

    /**
     * 删除文件
     */
    public void deleteFile(String objectName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }
}
