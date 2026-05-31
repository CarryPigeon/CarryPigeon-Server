package team.carrypigeon.backend.chat.domain.features.server.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * required gate 预检查结果。
 * 职责：向匿名客户端返回当前缺失的必需插件列表。
 * 边界：只表达 gate 判断结果，不承载登录或权限语义。
 */
public record RequiredGateCheckResult(
        @Schema(description = "当前缺失的必需插件列表")
        List<String> missingPlugins
) {
}
