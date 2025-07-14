package team.carrypigeon.backend.commander;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import team.carrypigeon.backend.connectionpool.security.CPKeyMessage;
import team.carrypigeon.backend.connectionpool.security.aes.AESUtil;
import team.carrypigeon.backend.connectionpool.security.ecc.ECCUtil;
import team.carrypigeon.backend.connectionpool.security.ecc.RsaKeyPair;

public class CommanderHandler extends SimpleChannelInboundHandler<String> {

    private TestClientState testClientState;
    private ObjectMapper objectMapper = new ObjectMapper();

    public CommanderHandler(TestClientState testClientState) {
        this.testClientState = testClientState;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        switch (testClientState.getState()) {
            case WAITE_RECEIVE_KEY:
                CPKeyMessage cpKeyMessage = objectMapper.readValue(msg, CPKeyMessage.class);
                String s = ECCUtil.eccDecrypt(testClientState.getECCKey(), cpKeyMessage.getKey());
                testClientState.setAesKey(s);
                // 输出验证消息
                ctx.writeAndFlush(AESUtil.encrypt("verification",AESUtil.convertStringToKey(testClientState.getAesKey())));
                break;
            case SUCCESS:
                System.out.println(
                        AESUtil.decrypt(msg,AESUtil.convertStringToKey(testClientState.getAesKey()))
                );
                break;
        }
    }
}
