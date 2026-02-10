package team.carrypigeon.backend.chat.domain.service.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.chat.domain.controller.web.api.config.CpApiProperties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ApiPluginPackageScannerTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void scanOnce_trustAllowlist_shouldFilterPlugins() throws Exception {
        Path dir = Files.createTempDirectory("cp-plugins");

        writeZip(dir.resolve("a.zip"), """
                {
                  "plugin_id": "a",
                  "name": "A",
                  "version": "1.0.0",
                  "min_host_version": "0.1.0",
                  "permissions": [],
                  "provides_domains": [],
                  "contracts": []
                }
                """);
        writeZip(dir.resolve("b.zip"), """
                {
                  "plugin_id": "b",
                  "name": "B",
                  "version": "1.0.0",
                  "min_host_version": "0.1.0",
                  "permissions": [],
                  "provides_domains": [],
                  "contracts": []
                }
                """);

        CpApiProperties props = new CpApiProperties();
        props.getApi().getPluginPackageScan().setEnabled(true);
        props.getApi().getPluginPackageScan().setDir(dir.toAbsolutePath().toString());
        props.getApi().getPluginPackageScan().setLatestOnly(true);

        CpApiProperties.PluginTrust trust = new CpApiProperties.PluginTrust();
        trust.setEnabled(true);
        trust.setAllowedPluginIds(List.of("a"));
        props.getApi().getPluginPackageScan().setTrust(trust);

        ApiPluginCatalogIndex index = new ApiPluginCatalogIndex();
        ApiPluginPackageScanner scanner = new ApiPluginPackageScanner(objectMapper, props, index);

        scanner.scanOnce();

        assertEquals(1, index.snapshot().pluginsView().size());
        assertEquals("a", index.snapshot().pluginsView().getFirst().getPluginId());
    }

    @Test
    void scanOnce_trustSignatureRequired_shouldRejectInvalidAndAcceptValid() throws Exception {
        Path dir = Files.createTempDirectory("cp-plugins-sig");
        KeyPairGenerator g = KeyPairGenerator.getInstance("Ed25519");
        KeyPair kp = g.generateKeyPair();
        String pubKeyB64 = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
        writeZip(dir.resolve("unsigned.zip"), """
                {
                  "plugin_id": "u",
                  "name": "U",
                  "version": "1.0.0",
                  "min_host_version": "0.1.0",
                  "permissions": [],
                  "provides_domains": [],
                  "contracts": []
                }
                """);
        Path signedZip = dir.resolve("signed.zip");
        String manifestNoSig = """
                {
                  "plugin_id": "%s",
                  "name": "S",
                  "version": "%s",
                  "min_host_version": "0.1.0",
                  "permissions": [],
                  "signing_key_id": "k1",
                  "provides_domains": [],
                  "contracts": []
                }
                """.formatted("s", "1.0.0");

        byte[] msg = canonicalManifestBytesForSignature(manifestNoSig);
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(kp.getPrivate());
        signer.update(msg);
        String sigB64 = Base64.getEncoder().encodeToString(signer.sign());

        String manifest = insertSignature(manifestNoSig, sigB64);
        writeZip(signedZip, manifest);

        CpApiProperties props = new CpApiProperties();
        props.getApi().getPluginPackageScan().setEnabled(true);
        props.getApi().getPluginPackageScan().setDir(dir.toAbsolutePath().toString());
        props.getApi().getPluginPackageScan().setLatestOnly(true);

        CpApiProperties.PluginTrust trust = new CpApiProperties.PluginTrust();
        trust.setEnabled(true);
        trust.setRequireEd25519Signature(true);
        CpApiProperties.Ed25519PublicKey key = new CpApiProperties.Ed25519PublicKey();
        key.setKeyId("k1");
        key.setPublicKeyBase64(pubKeyB64);
        trust.setEd25519PublicKeys(List.of(key));
        props.getApi().getPluginPackageScan().setTrust(trust);

        ApiPluginCatalogIndex index = new ApiPluginCatalogIndex();
        ApiPluginPackageScanner scanner = new ApiPluginPackageScanner(objectMapper, props, index);

        scanner.scanOnce();

        assertEquals(1, index.snapshot().pluginsView().size());
        assertEquals("s", index.snapshot().pluginsView().getFirst().getPluginId());
    }

    /**
     * 测试辅助方法。
     *
     * @param zipPath 测试输入参数
     * @param manifestJson 测试输入参数
     * @throws Exception 执行过程中抛出的异常
     */
    private void writeZip(Path zipPath, String manifestJson) throws Exception {
        Files.deleteIfExists(zipPath);
        try (var out = Files.newOutputStream(zipPath);
             ZipOutputStream zos = new ZipOutputStream(out)) {
            ZipEntry e = new ZipEntry("manifest.json");
            zos.putNextEntry(e);
            byte[] bytes = manifestJson.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            zos.write(bytes);
            zos.closeEntry();
        }
    }

    /**
     * 测试辅助方法。
     *
     * @param manifestJson 测试输入参数
     * @return 测试辅助方法返回结果
     * @throws Exception 执行过程中抛出的异常
     */
    private byte[] canonicalManifestBytesForSignature(String manifestJson) throws Exception {
        JsonNode n = objectMapper.readTree(manifestJson);
        assertNotNull(n);
        assertTrue(n.isObject());
        ObjectNode copy = ((ObjectNode) n).deepCopy();
        copy.remove("signature");
        return objectMapper.writer()
                .with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .writeValueAsBytes(copy);
    }

    /**
     * 测试辅助方法。
     *
     * @param manifestWithoutSignature 测试输入参数
     * @param signatureBase64 测试输入参数
     * @return 测试辅助方法返回结果
     * @throws Exception 执行过程中抛出的异常
     */
    private String insertSignature(String manifestWithoutSignature, String signatureBase64) throws Exception {
        JsonNode n = objectMapper.readTree(manifestWithoutSignature);
        assertNotNull(n);
        assertTrue(n.isObject());
        ObjectNode obj = ((ObjectNode) n).deepCopy();
        obj.put("signature", signatureBase64);
        return objectMapper.writer()
                .with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .writeValueAsString(obj);
    }
}
