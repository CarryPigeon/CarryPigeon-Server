package team.carrypigeon.backend.commander;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import team.carrypigeon.backend.connectionpool.channel.NettySession;
import team.carrypigeon.backend.connectionpool.security.CPEncryptionState;
import team.carrypigeon.backend.connectionpool.security.CPKeyMessage;
import team.carrypigeon.backend.connectionpool.protocol.encryption.aes.AESUtil;

import static team.carrypigeon.backend.commander.State.SUCCESS;
import static team.carrypigeon.backend.connectionpool.attribute.ConnectionAttributes.SESSIONS;

public class CommanderHandler extends SimpleChannelInboundHandler<String> {

    private TestClientState testClientState;
    private ObjectMapper objectMapper = new ObjectMapper();

    public CommanderHandler(TestClientState testClientState) {
        this.testClientState = testClientState;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws JsonProcessingException {
        System.out.println("接受到消息");
        switch (testClientState.getState()) {
            case WAITE_RECEIVE_KEY:
                CPKeyMessage cpKeyMessage = objectMapper.readValue(msg, CPKeyMessage.class);
                // String s = ECCUtil.eccDecrypt(testClientState.getECCKey(), cpKeyMessage.getKey());
                // testClientState.setAesKey(s);
                // 输出验证消息
                try {
                    ctx.writeAndFlush(AESUtil.encrypt("verification",AESUtil.convertStringToKey(testClientState.getAesKey())));
                } catch (Exception e) {

                }
                testClientState.setState(SUCCESS);
                ctx.channel().attr(SESSIONS).set(new NettySession(ctx,new CPEncryptionState().setKey(testClientState.getAesKey())));
                break;
            case SUCCESS:
                break;
        }
        try {
            System.out.println(msg);
            System.out.println(
                    AESUtil.decrypt(msg,AESUtil.convertStringToKey(testClientState.getAesKey()))
            );
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
