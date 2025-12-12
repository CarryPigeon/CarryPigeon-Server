package team.carrypigeon.backend.connection.protocol.aad;

import cn.hutool.core.util.ByteUtil;

import java.nio.ByteOrder;

/**
 * AAD (Additional Authenticated Data) used in AES-GCM packets.
 * <p>
 * Layout (20 bytes total, big-endian):
 * <pre>
 * 0-3   : packageId (int)
 * 4-11  : sessionId (long)
 * 12-19 : timestampMillis (long)
 * </pre>
 */
public final class AeadAad {

    public static final int LENGTH = 20;

    private static final int OFFSET_PACKAGE_ID = 0;
    private static final int OFFSET_SESSION_ID = 4;
    private static final int OFFSET_TIMESTAMP = 12;

    private final int packageId;
    private final long sessionId;
    private final long timestampMillis;

    public AeadAad(int packageId, long sessionId, long timestampMillis) {
        this.packageId = packageId;
        this.sessionId = sessionId;
        this.timestampMillis = timestampMillis;
    }

    public int getPackageId() {
        return packageId;
    }

    public long getSessionId() {
        return sessionId;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    /**
     * Encode current fields into a 20-byte AAD buffer.
     */
    public byte[] encode() {
        byte[] aad = new byte[LENGTH];
        System.arraycopy(ByteUtil.intToBytes(packageId), 0, aad, OFFSET_PACKAGE_ID, 4);
        System.arraycopy(ByteUtil.longToBytes(sessionId), 0, aad, OFFSET_SESSION_ID, 8);
        System.arraycopy(ByteUtil.longToBytes(timestampMillis), 0, aad, OFFSET_TIMESTAMP, 8);
        return aad;
    }

    /**
     * Decode a 20-byte AAD buffer into an {@link AeadAad} instance.
     *
     * @throws IllegalArgumentException if aad is null or length is not {@link #LENGTH}
     */
    public static AeadAad decode(byte[] aad) {
        if (aad == null || aad.length != LENGTH) {
            throw new IllegalArgumentException("AAD length must be " + LENGTH + " bytes");
        }
        int pkgId = ByteUtil.bytesToInt(aad, OFFSET_PACKAGE_ID, ByteOrder.BIG_ENDIAN);
        long sessId = ByteUtil.bytesToLong(aad, OFFSET_SESSION_ID, ByteOrder.BIG_ENDIAN);
        long ts = ByteUtil.bytesToLong(aad, OFFSET_TIMESTAMP, ByteOrder.BIG_ENDIAN);
        return new AeadAad(pkgId, sessId, ts);
    }
}

