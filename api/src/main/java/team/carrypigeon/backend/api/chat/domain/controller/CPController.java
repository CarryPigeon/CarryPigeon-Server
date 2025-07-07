package team.carrypigeon.backend.api.chat.domain.controller;

import com.fasterxml.jackson.databind.JsonNode;
import team.carrypigeon.backend.common.response.CPResponse;

/**
 * CarryPigeon Controller,用于处理一个客户端请求
 * */
public interface CPController {
    /**
     * 处理方法
     * @param data 具体的请求携带的数据
     */
    CPResponse process(JsonNode data);
}