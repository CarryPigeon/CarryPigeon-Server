package team.carrypigeon.backend.chat.domain.contract;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Contract test: generated protocol artifacts must stay in sync with source code.
 */
class GeneratedProtocolArtifactsContractTests {

    @Test
    void generatedArtifacts_inSync_expectedTrue() throws Exception {
        Path script = resolveScriptPath();

        ProcessBuilder pb = new ProcessBuilder("python3", script.toString(), "--check");
        pb.redirectErrorStream(true);
        Process process = pb.start();

        String output;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
            output = sb.toString();
        }

        int exit = process.waitFor();
        assertEquals(0, exit,
                "Generated artifacts are out-of-date. Run: python3 scripts/generate_protocol_artifacts.py\n" + output);
    }

    /**
     * 解析协议产物生成脚本的本地路径。
     *
     * @return 可执行的脚本文件路径
     */
    private static Path resolveScriptPath() {
        Path candidate1 = Paths.get("../scripts/generate_protocol_artifacts.py");
        if (Files.isRegularFile(candidate1)) {
            return candidate1;
        }
        Path candidate2 = Paths.get("scripts/generate_protocol_artifacts.py");
        if (Files.isRegularFile(candidate2)) {
            return candidate2;
        }
        throw new IllegalStateException("Cannot locate scripts/generate_protocol_artifacts.py");
    }
}
