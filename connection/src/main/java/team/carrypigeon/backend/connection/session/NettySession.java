package team.carrypigeon.backend.connection.session;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ByteUtil;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.connection.attribute.ConnectionAttributes;
import team.carrypigeon.backend.connection.protocol.encryption.aes.AESData;
import team.carrypigeon.backend.connection.protocol.encryption.aes.AESUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * netty会话实现类，用于对netty通道进行封装用于carrypigeon <br/>
 * @author midreamsheep
 **/
@Slf4j
public class NettySession implements CPSession {

    // netty channel上下文，用于进行数据的写操作
    private final ChannelHandlerContext context;
    // 实现私有的ConcurrentHashMap存储上下文数据并保障多线程读写安全
    private final Map<String,Object> attributes = new ConcurrentHashMap<>();

    public NettySession(ChannelHandlerContext context) {
        this.context = context;
        init();
    }

    private void init(){
        // 加入基础属性进入属性容器attributes中
        // 添加加密状态属性
        attributes.put(ConnectionAttributes.ENCRYPTION_KEY, "");
        // 添加状态机
        attributes.put(ConnectionAttributes.ENCRYPTION_STATE, false);
        // 添加会话唯一id
        attributes.put(ConnectionAttributes.PACKAGE_SESSION_ID, IdUtil.generateId());
        // 添加包序列
        attributes.put(ConnectionAttributes.PACKAGE_ID, 0);
        // 添加本地包序列
        attributes.put(ConnectionAttributes.LOCAL_PACKAGE_ID, 0);
    }

    @Override
    public void write(String msg, boolean encrypted) {
        // 创建aad
        byte[] aad = new byte[20];
        // 填充包序列
        System.arraycopy(ByteUtil.intToBytes(getAttributeValue(ConnectionAttributes.LOCAL_PACKAGE_ID, Integer.class)),0,aad,0,4);
        // 包序列自增
        setAttributeValue(ConnectionAttributes.LOCAL_PACKAGE_ID, getAttributeValue(ConnectionAttributes.LOCAL_PACKAGE_ID, Integer.class) + 1);
        // 填充会话id
        System.arraycopy(ByteUtil.longToBytes(getAttributeValue(ConnectionAttributes.PACKAGE_SESSION_ID, Long.class)),0,aad,4,8);
        // 填充时间戳
        System.arraycopy(ByteUtil.longToBytes(System.currentTimeMillis()),0,aad,16,4);

        if(encrypted){
            // 创建密文
            AESData aesData;
            try {
                aesData = AESUtil.encryptWithAAD(msg, aad, Base64.decode(getAttributeValue(ConnectionAttributes.ENCRYPTION_KEY, String.class)));
            } catch (Exception e) {
                log.error("unexpected error: AES encrypt failed");
                log.error(e.getMessage(),e);
                close();
                return;
            }
            // 构建发送的数据
            byte[] data = new byte[aesData.ciphertext().length + aad.length+aesData.nonce().length];
            // 填入nonce
            System.arraycopy(aesData.nonce(),0,data,0,aesData.nonce().length);
            // 填入aad
            System.arraycopy(aad,0,data,aesData.nonce().length,aad.length);
            // 填入ciphertext密文
            System.arraycopy(aesData.ciphertext(),0,data,aesData.nonce().length+aad.length,aesData.ciphertext().length);

            context.writeAndFlush(data);
        }else{
            byte[] bytes = msg.getBytes();
            byte[] data = new byte[bytes.length + aad.length+12];
            System.arraycopy(new byte[12],0,data,0,new byte[12].length);
            System.arraycopy(aad,0,data,12,aad.length);
            System.arraycopy(bytes,0,data,32,bytes.length);
            context.writeAndFlush(data);
        }

    }

    @Override
    public <T> T getAttributeValue(String key, Class<T> type) {
        Object value = attributes.get(key);
        if (!type.isInstance(value)) {
            return null;
        }
        return type.cast(value);
    }

    @Override
    public void setAttributeValue(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public void close() {
        context.close();
    }
}
