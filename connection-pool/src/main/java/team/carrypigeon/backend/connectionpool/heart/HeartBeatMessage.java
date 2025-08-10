package team.carrypigeon.backend.connectionpool.heart;

import team.carrypigeon.backend.api.connection.vo.CPPacket;

public class HeartBeatMessage {
    public static final CPPacket HEARTBEAT = new CPPacket(-1,"HeartBeat",null);
}