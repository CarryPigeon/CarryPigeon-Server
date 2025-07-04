package team.carrypigeon.backend.api.chat.domain.controller;

/**
 * CarryPigeon Controller,用于处理一个客户端请求
 * */
public interface CPController {
    /**
     * 处理方法
     * @param msg 具体的数据
     * */
    void process(String msg);
}