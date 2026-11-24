package team.carrypigeon.backend.chat.domain.http.openapi;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.GlobalOperationComponentsCustomizer;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.utils.SpringDocAnnotationsUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import team.carrypigeon.backend.chat.domain.shared.result.Result;

/** 用方法的完整泛型生成成功分支，保留页内元素类型和无返回值操作的必填 null。 */
@Component
final class OpenApiErrorResponses implements GlobalOperationComponentsCustomizer {

    private static final String JSON = "application/json";
    private final SpringDocConfigProperties config;

    OpenApiErrorResponses(SpringDocConfigProperties config) {
        this.config = config;
    }

    @Override
    public Operation customize(Operation operation, HandlerMethod method) {
        return operation;
    }

    @Override
    public Operation customize(Operation operation, Components components, HandlerMethod method) {
        Type type = method.getMethod().getGenericReturnType();
        if (!(type instanceof ParameterizedType result) || result.getRawType() != Result.class) {
            return operation;
        }
        SpecVersion version = config.getSpecVersion();
        Type payload = result.getActualTypeArguments()[0];
        Schema<?> data;
        if (payload == Void.class) {
            data = new Schema<>();
            data.addType("null");
            if (version == SpecVersion.V30) {
                data = new Schema<>().type("object").nullable(true);
                data.setEnum(java.util.Collections.singletonList(null));
            }
        } else {
            data = SpringDocAnnotationsUtils.extractSchema(components, payload, null, null, version);
        }
        ObjectSchema success = new ObjectSchema();
        success.addProperty("code", new IntegerSchema()._enum(List.of(0)));
        success.addProperty("message", new StringSchema()._enum(List.of("OK")));
        success.addProperty("data", data);
        success.setRequired(List.of("code", "message", "data"));
        success.setAdditionalProperties(false);

        ComposedSchema union = new ComposedSchema();
        union.addOneOfItem(success);
        union.addOneOfItem(errorSchema());
        ApiResponse ok = operation.getResponses().get("200");
        ok.setContent(json(union));
        operation.setResponses(new ApiResponses().addApiResponse("200", ok));
        return operation;
    }

    static void documentProtocol(Operation operation, String path) {
        if ("/internal/readiness".equals(path)) {
            operation.setResponses(new ApiResponses()
                .addApiResponse("204", new ApiResponse().description("已就绪，响应体为空"))
                .addApiResponse("503", new ApiResponse().description("未就绪，响应体为空"))
                .addApiResponse("403", new ApiResponse().description("来源无权访问，响应体为空")));
            return;
        }
        if (!path.startsWith("/api/")) {
            return;
        }
        if (path.startsWith("/api/files/download/")) {
            ApiResponse binary = operation.getResponses().get("200");
            operation.setResponses(new ApiResponses().addApiResponse("200", binary));
            add(operation, "401", "认证失败，下载前返回 JSON")
                .addHeaderObject("WWW-Authenticate", new Header().schema(new StringSchema().example("Bearer")));
            add(operation, "404", "文件不存在");
            add(operation, "500", "文件读取或内部错误");
            add(operation, "503", "依赖不可用");
        } else {
            add(operation, "406", "客户端不接受 JSON");
        }
        if ("/api/files/uploads/{shareKey}".equals(path)) {
            add(operation, "411", "缺少 Content-Length");
            add(operation, "413", "上传的请求体超过传输上限");
        } else if (operation.getRequestBody() != null) {
            add(operation, "400", "JSON 语法无效");
            add(operation, "415", "请求媒体类型不支持");
        }
    }

    private static ApiResponse add(Operation operation, String status, String description) {
        ApiResponse response = new ApiResponse().description(description).content(json(errorSchema()));
        operation.getResponses().addApiResponse(status, response);
        return response;
    }

    private static Schema<?> errorSchema() {
        ObjectSchema error = new ObjectSchema();
        error.addProperty("code", new IntegerSchema().minimum(BigDecimal.valueOf(10000)).maximum(BigDecimal.valueOf(99999)));
        error.addProperty("message", new StringSchema());
        error.setRequired(List.of("code", "message"));
        error.setAdditionalProperties(false);
        return error;
    }

    private static Content json(Schema<?> schema) {
        return new Content().addMediaType(JSON, new MediaType().schema(schema));
    }
}
