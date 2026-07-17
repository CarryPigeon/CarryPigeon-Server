package team.carrypigeon.backend.chat.domain.shared.controller;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.ChannelApplicationController;
import team.carrypigeon.backend.chat.domain.features.channel.controller.http.ChannelController;
import team.carrypigeon.backend.chat.domain.features.user.controller.http.UserProfileController;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HTTP 路由契约测试。
 * 职责：验证当前源码只声明 v1 资源路径，不重新暴露已明确废弃的过渡接口。
 * 边界：只检查 Controller 注解声明，不启动 Spring MVC，也不验证运行时路径匹配算法。
 */
@Tag("contract")
class HttpRouteContractTests {

    /**
     * 验证用户和频道 Controller 没有重新声明旧过渡接口。
     */
    @Test
    @DisplayName("controllers do not declare removed transitional routes")
    void controllers_removedTransitionalRoutes_notDeclared() {
        Set<Route> routes = collectRoutes(UserProfileController.class, ChannelController.class, ChannelApplicationController.class);

        assertFalse(routes.contains(new Route("GET", "/api/users/page")));
        assertFalse(routes.contains(new Route("GET", "/api/users/search")));
        assertFalse(routes.contains(new Route("PUT", "/api/users/me")));
        assertFalse(routes.contains(new Route("GET", "/api/channels/default")));
        assertFalse(routes.contains(new Route("GET", "/api/channels/system")));
        assertFalse(routes.contains(new Route("POST", "/api/channels/private")));
        assertFalse(routes.contains(new Route("POST", "/api/channels/{}/invites")));
        assertFalse(routes.contains(new Route("POST", "/api/channels/{}/invites/accept")));
        assertFalse(routes.contains(new Route("POST", "/api/channels/{}/ownership-transfer")));
        assertFalse(routes.contains(new Route("POST", "/api/channels/{}/members/{}/mute")));
        assertFalse(routes.contains(new Route("DELETE", "/api/channels/{}/members/{}/mute")));
        assertFalse(routes.contains(new Route("POST", "/api/channels/{}/bans")));
    }

    /**
     * 验证当前 v1 用户和频道资源路径仍保持声明，避免删除过渡接口时误删正式路径。
     */
    @Test
    @DisplayName("controllers declare current v1 resource routes")
    void controllers_currentV1Routes_declared() {
        Set<Route> routes = collectRoutes(UserProfileController.class, ChannelController.class, ChannelApplicationController.class);

        assertTrue(routes.contains(new Route("GET", "/api/users/me")));
        assertTrue(routes.contains(new Route("PATCH", "/api/users/me")));
        assertTrue(routes.contains(new Route("PUT", "/api/users/me/email")));
        assertTrue(routes.contains(new Route("GET", "/api/channels")));
        assertTrue(routes.contains(new Route("POST", "/api/channels")));
        assertTrue(routes.contains(new Route("GET", "/api/channels/{}")));
        assertTrue(routes.contains(new Route("POST", "/api/channels/{}/applications")));
        assertTrue(routes.contains(new Route("POST", "/api/channels/{}/applications/{}/decisions")));
        assertTrue(routes.contains(new Route("PUT", "/api/channels/{}/bans/{}")));
        assertTrue(routes.contains(new Route("DELETE", "/api/channels/{}/bans/{}")));
    }

    private static Set<Route> collectRoutes(Class<?>... controllerTypes) {
        Set<Route> routes = new HashSet<>();
        for (Class<?> controllerType : controllerTypes) {
            for (String classPath : classPaths(controllerType)) {
                for (Method method : controllerType.getDeclaredMethods()) {
                    methodRoutes(method, classPath, routes);
                }
            }
        }
        return routes;
    }

    private static List<String> classPaths(Class<?> controllerType) {
        RequestMapping requestMapping = controllerType.getAnnotation(RequestMapping.class);
        if (requestMapping == null) {
            return List.of("");
        }
        String[] values = requestMapping.path().length == 0 ? requestMapping.value() : requestMapping.path();
        if (values.length == 0) {
            return List.of("");
        }
        return List.of(values);
    }

    private static void methodRoutes(Method method, String classPath, Set<Route> routes) {
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        if (getMapping != null) {
            addRoutes(routes, "GET", classPath, paths(getMapping.path(), getMapping.value()));
        }
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        if (postMapping != null) {
            addRoutes(routes, "POST", classPath, paths(postMapping.path(), postMapping.value()));
        }
        PutMapping putMapping = method.getAnnotation(PutMapping.class);
        if (putMapping != null) {
            addRoutes(routes, "PUT", classPath, paths(putMapping.path(), putMapping.value()));
        }
        PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
        if (patchMapping != null) {
            addRoutes(routes, "PATCH", classPath, paths(patchMapping.path(), patchMapping.value()));
        }
        DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
        if (deleteMapping != null) {
            addRoutes(routes, "DELETE", classPath, paths(deleteMapping.path(), deleteMapping.value()));
        }
    }

    private static String[] paths(String[] path, String[] value) {
        if (path.length > 0) {
            return path;
        }
        return value.length == 0 ? new String[] {""} : value;
    }

    private static void addRoutes(Set<Route> routes, String method, String classPath, String[] methodPaths) {
        for (String methodPath : methodPaths) {
            routes.add(new Route(method, normalizePath(combine(classPath, methodPath))));
        }
    }

    private static String combine(String classPath, String methodPath) {
        if (classPath == null || classPath.isBlank()) {
            return methodPath == null ? "" : methodPath;
        }
        if (methodPath == null || methodPath.isBlank()) {
            return classPath;
        }
        return classPath.endsWith("/") || methodPath.startsWith("/")
                ? classPath + methodPath
                : classPath + "/" + methodPath;
    }

    private static String normalizePath(String path) {
        String normalized = path.replaceAll("/{2,}", "/");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.replaceAll("\\{[^/]+}", "{}");
    }

    private record Route(String method, String path) {
    }
}
