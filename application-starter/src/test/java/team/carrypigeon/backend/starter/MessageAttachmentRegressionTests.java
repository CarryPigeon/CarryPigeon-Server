package team.carrypigeon.backend.starter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AuthAccountApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.api.AuthSessionApi;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.LoginCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.command.RegisterCommand;
import team.carrypigeon.backend.chat.domain.features.auth.domain.projection.AuthTokenResult;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthRequestContext;
import team.carrypigeon.backend.chat.domain.features.auth.controller.support.AuthenticatedPrincipal;
import team.carrypigeon.backend.chat.domain.features.channel.controller.dto.UpdateChannelReadStateRequest;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.ChannelReadStateController;
import team.carrypigeon.backend.chat.domain.features.file.controller.dto.CreateFileUploadRequest;
import team.carrypigeon.backend.chat.domain.features.file.controller.http.FileController;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.ChannelMessagePublishingApi;
import team.carrypigeon.backend.chat.domain.features.message.domain.command.SendChannelMessageCommand;
import team.carrypigeon.backend.chat.domain.features.message.domain.draft.TextChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.controller.http.ChannelMessageController;
import team.carrypigeon.backend.infrastructure.basic.config.BasicInfrastructureAutoConfiguration;
import team.carrypigeon.backend.infrastructure.basic.json.JacksonAutoConfiguration;
import team.carrypigeon.backend.starter.config.InitializationCheckConfiguration;
import team.carrypigeon.backend.starter.support.StarterRegressionConfiguration;
import team.carrypigeon.backend.starter.support.StarterTestRuntimeConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 消息附件跨层回归测试。
 * 职责：验证 starter 级 Spring 装配下的 files 基准协议与消息未读链路回归。
 * 边界：不依赖真实外部服务，使用内存替身验证跨层语义。
 */
@Tag("regression")
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

    @Test
    @DisplayName("starter regression file upload grant and download flow returns same-origin endpoints")
    void starterRegression_fileUploadGrantAndDownloadFlow_returnsSameOriginEndpoints() {
        contextRunner.run(context -> {
            StarterTestRuntimeConfiguration.StarterTestState state = context.getBean(StarterTestRuntimeConfiguration.StarterTestState.class);
            state.reset();
            AuthAccountApi authAccountApi = context.getBean(AuthAccountApi.class);
            AuthSessionApi authSessionApi = context.getBean(AuthSessionApi.class);
            FileController fileController = context.getBean(FileController.class);
            AuthRequestContext authRequestContext = context.getBean(AuthRequestContext.class);

            authAccountApi.register(new RegisterCommand("carry-storage-user", "strong-pass-9911"));
            AuthTokenResult loginResult = authSessionApi.login(new LoginCommand("carry-storage-user", "strong-pass-9911"));
            long accountId = loginResult.accountId();

            MockHttpServletRequest request = new MockHttpServletRequest();
            authRequestContext.bind(request, new AuthenticatedPrincipal(accountId, "carry-storage-user"));

            var uploadResponse = fileController.createUpload(new CreateFileUploadRequest("image.png", "image/png", 12L, null), request);
            assertThat(uploadResponse.upload().url()).isEqualTo("/api/files/uploads/" + uploadResponse.shareKey());

            MockHttpServletRequest uploadRequest = new MockHttpServletRequest();
            uploadRequest.setContentType("image/png");
            uploadRequest.setContent("hello-image!".getBytes());
            authRequestContext.bind(uploadRequest, new AuthenticatedPrincipal(accountId, "carry-storage-user"));
            fileController.uploadFile(uploadResponse.shareKey(), uploadRequest);

            var downloadResponse = fileController.download(uploadResponse.shareKey(), request);
            assertThat(downloadResponse.getStatusCode().value()).isEqualTo(302);
            assertThat(downloadResponse.getHeaders().getLocation()).isNotNull();
            assertThat(downloadResponse.getHeaders().getLocation().toString())
                    .isEqualTo("http://test.local/objects/files/accounts/" + accountId + "/" + uploadResponse.fileId());
        });
    }

    @Test
    @DisplayName("starter regression read state and unreads flow returns unread aggregate")
    void starterRegression_readStateAndUnreadsFlow_returnsUnreadAggregate() {
        contextRunner.run(context -> {
            StarterTestRuntimeConfiguration.StarterTestState state = context.getBean(StarterTestRuntimeConfiguration.StarterTestState.class);
            state.reset();
            AuthAccountApi authAccountApi = context.getBean(AuthAccountApi.class);
            AuthSessionApi authSessionApi = context.getBean(AuthSessionApi.class);
            ChannelMessageController channelMessageController = context.getBean(ChannelMessageController.class);
            ChannelReadStateController channelReadStateController = context.getBean(ChannelReadStateController.class);
            ChannelMessagePublishingApi channelMessagePublishingApi = context.getBean(ChannelMessagePublishingApi.class);
            AuthRequestContext authRequestContext = context.getBean(AuthRequestContext.class);

            authAccountApi.register(new RegisterCommand("carry-read-user", "strong-pass-3322"));
            AuthTokenResult loginResult = authSessionApi.login(new LoginCommand("carry-read-user", "strong-pass-3322"));
            long accountId = loginResult.accountId();
            authAccountApi.register(new RegisterCommand("carry-read-peer", "strong-pass-4411"));
            AuthTokenResult peerLoginResult = authSessionApi.login(new LoginCommand("carry-read-peer", "strong-pass-4411"));

            MockHttpServletRequest request = new MockHttpServletRequest();
            authRequestContext.bind(request, new AuthenticatedPrincipal(accountId, "carry-read-user"));

            channelMessagePublishingApi.sendChannelMessage(new SendChannelMessageCommand(
                    peerLoginResult.accountId(),
                    1L,
                    new TextChannelMessageDraft("未读消息")
            ));

            var unreadResponse = channelReadStateController.listUnreads(request);
            assertThat(unreadResponse.items()).hasSize(1);
            assertThat(unreadResponse.items().getFirst().cid()).isEqualTo("1");
            assertThat(unreadResponse.items().getFirst().unreadCount()).isEqualTo(1L);

            var historyResponse = channelMessageController.getChannelMessages(1L, null, null, null, null, 20, request);
            String messageId = historyResponse.items().getFirst().mid();

            var readStateResponse = channelReadStateController.updateReadState(
                    1L,
                    new UpdateChannelReadStateRequest(messageId, 1700000000000L),
                    request
            );
            assertThat(readStateResponse.lastReadMid()).isEqualTo(messageId);
        });
    }
}
