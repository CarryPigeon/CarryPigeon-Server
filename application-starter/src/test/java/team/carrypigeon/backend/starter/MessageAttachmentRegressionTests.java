package team.carrypigeon.backend.starter;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import team.carrypigeon.backend.chat.domain.features.auth.controller.dto.AuthTokenResponse;
import team.carrypigeon.backend.chat.domain.features.auth.controller.dto.RegisterRequest;
import team.carrypigeon.backend.chat.domain.features.auth.controller.dto.RefreshTokenRequest;
import team.carrypigeon.backend.chat.domain.features.auth.controller.http.AuthController;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthRequestContext;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.message.application.command.SendChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.FileChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.VoiceChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessageApplicationService;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.ChannelMessageHistoryResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.dto.ChannelMessageSearchResponse;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.ChannelMessageController;
import team.carrypigeon.backend.chat.domain.shared.controller.CPResponse;
import team.carrypigeon.backend.infrastructure.basic.config.BasicInfrastructureAutoConfiguration;
import team.carrypigeon.backend.infrastructure.basic.json.JacksonAutoConfiguration;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;
import team.carrypigeon.backend.starter.config.InitializationCheckConfiguration;
import team.carrypigeon.backend.starter.support.StarterRegressionConfiguration;
import team.carrypigeon.backend.starter.support.StarterTestRuntimeConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 消息附件跨层回归测试。
 * 职责：验证 starter 级 Spring 装配下的附件上传、消息发送、历史查询与搜索的关键回归链路。
 * 边界：不依赖真实外部服务，使用内存替身验证跨层语义。
 */
class MessageAttachmentRegressionTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    BasicInfrastructureAutoConfiguration.class,
                    JacksonAutoConfiguration.class
            ))
            .withUserConfiguration(
                    StarterTestRuntimeConfiguration.class,
                    StarterRegressionConfiguration.class,
                    InitializationCheckConfiguration.class
            );

    /**
     * 验证 file 附件链路在 starter 装配下能完成上传、发送和历史查询出站 URL 派生。
     */
    @Test
    @DisplayName("starter regression file attachment flow returns canonical payload with access url")
    void starterRegression_fileAttachmentFlow_returnsCanonicalPayloadWithAccessUrl() {
        contextRunner.run(context -> {
            StarterTestRuntimeConfiguration.StarterTestState state = context.getBean(StarterTestRuntimeConfiguration.StarterTestState.class);
            state.reset();
            AuthController authController = context.getBean(AuthController.class);
            ChannelMessageController channelMessageController = context.getBean(ChannelMessageController.class);
            MessageApplicationService messageApplicationService = context.getBean(MessageApplicationService.class);
            AuthRequestContext authRequestContext = context.getBean(AuthRequestContext.class);
            JsonProvider jsonProvider = context.getBean(JsonProvider.class);

            authController.register(new RegisterRequest("carry-file-user", "strong-pass-1234"));
            CPResponse<AuthTokenResponse> loginResponse = authController.login(
                    new team.carrypigeon.backend.chat.domain.features.auth.controller.dto.LoginRequest("carry-file-user", "strong-pass-1234")
            );
            long accountId = loginResponse.data().accountId();

            MockHttpServletRequest request = new MockHttpServletRequest();
            authRequestContext.bind(request, new AuthenticatedPrincipal(accountId, "carry-file-user"));

            String objectKey = channelMessageController.uploadChannelMessageAttachment(
                    1L,
                    "file",
                    new MockMultipartFile("file", "demo.pdf", "application/pdf", "demo-content".getBytes(StandardCharsets.UTF_8)),
                    request
            ).data().objectKey();

            messageApplicationService.sendChannelMessage(new SendChannelMessageCommand(
                    accountId,
                    1L,
                    new FileChannelMessageDraft("项目文档", objectKey, "demo.pdf", "application/pdf", 12L, null)
            ));

            CPResponse<ChannelMessageHistoryResponse> historyResponse = channelMessageController.getChannelMessages(1L, null, 20, request);
            String payload = historyResponse.data().messages().getFirst().payload();
            var payloadJson = jsonProvider.readTree(payload);

            assertThat(historyResponse.code()).isEqualTo(100);
            assertThat(historyResponse.data().messages().getFirst().messageType()).isEqualTo("file");
            assertThat(payloadJson.path("object_key").asText()).isEqualTo(objectKey);
            assertThat(payloadJson.path("access_url").asText()).contains(objectKey);
        });
    }

    /**
     * 验证 voice 附件链路在 starter 装配下能完成发送并在搜索结果中返回语音元信息与访问 URL。
     */
    @Test
    @DisplayName("starter regression voice attachment flow returns payload with metadata and access url")
    void starterRegression_voiceAttachmentFlow_returnsPayloadWithMetadataAndAccessUrl() {
        contextRunner.run(context -> {
            StarterTestRuntimeConfiguration.StarterTestState state = context.getBean(StarterTestRuntimeConfiguration.StarterTestState.class);
            state.reset();
            AuthController authController = context.getBean(AuthController.class);
            ChannelMessageController channelMessageController = context.getBean(ChannelMessageController.class);
            MessageApplicationService messageApplicationService = context.getBean(MessageApplicationService.class);
            AuthRequestContext authRequestContext = context.getBean(AuthRequestContext.class);
            JsonProvider jsonProvider = context.getBean(JsonProvider.class);

            authController.register(new RegisterRequest("carry-voice-user", "strong-pass-5678"));
            CPResponse<AuthTokenResponse> loginResponse = authController.login(
                    new team.carrypigeon.backend.chat.domain.features.auth.controller.dto.LoginRequest("carry-voice-user", "strong-pass-5678")
            );
            long accountId = loginResponse.data().accountId();

            MockHttpServletRequest request = new MockHttpServletRequest();
            authRequestContext.bind(request, new AuthenticatedPrincipal(accountId, "carry-voice-user"));

            String objectKey = channelMessageController.uploadChannelMessageAttachment(
                    1L,
                    "voice",
                    new MockMultipartFile("file", "demo.mp3", "audio/mpeg", "voice-content".getBytes(StandardCharsets.UTF_8)),
                    request
            ).data().objectKey();

            messageApplicationService.sendChannelMessage(new SendChannelMessageCommand(
                    accountId,
                    1L,
                    new VoiceChannelMessageDraft(null, objectKey, "demo.mp3", "audio/mpeg", 13L, 12000L, "会议纪要", null)
            ));

            CPResponse<ChannelMessageSearchResponse> searchResponse = channelMessageController.searchChannelMessages(1L, "会议纪要", 20, request);
            String payload = searchResponse.data().messages().getFirst().payload();
            var payloadJson = jsonProvider.readTree(payload);

            assertThat(searchResponse.code()).isEqualTo(100);
            assertThat(searchResponse.data().messages().getFirst().messageType()).isEqualTo("voice");
            assertThat(payloadJson.path("object_key").asText()).isEqualTo(objectKey);
            assertThat(payloadJson.path("duration_millis").asLong()).isEqualTo(12000L);
            assertThat(payloadJson.path("transcript").asText()).isEqualTo("会议纪要");
            assertThat(payloadJson.path("access_url").asText()).contains(objectKey);
        });
    }
}
