package top.sincs.sinbot.common;

import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import tools.jackson.databind.ObjectMapper;

/**
 * 对 {@code top.sincs.sinbot.controller} 包下所有接口的返回值统一加壳。
 *
 * <p>使用 {@code basePackages} 限定作用域，从而自动绕开 Druid 监控页、
 * Actuator、OpenAPI 文档以及 BasicErrorController 的 {@code /error}。</p>
 *
 * <p>以下情况不会被包装：</p>
 * <ul>
 *     <li>返回值已是 {@link Result}（例如 {@code GlobalExceptionHandler} 的产物，防止二次包装）</li>
 *     <li>文件下载类返回值：{@link Resource}、{@link StreamingResponseBody}、 {@code byte[]}</li>
 *     <li>已自行声明状态码的 {@link ResponseEntity}（请直接返回 {@code ResponseEntity<Result<?>>}）</li>
 *     <li>标注了 {@link NoWrap} 的 Controller 或方法</li>
 * </ul>
 */
@RestControllerAdvice(basePackages = "top.sincs.sinbot.controller")
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    public GlobalResponseAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        Class<?> type = returnType.getParameterType();
        return !Result.class.isAssignableFrom(type)
                && !ResponseEntity.class.isAssignableFrom(type)
                && !Resource.class.isAssignableFrom(type)
                && !StreamingResponseBody.class.isAssignableFrom(type)
                && !byte[].class.equals(type)
                && !void.class.equals(type)
                && !returnType.hasMethodAnnotation(NoWrap.class)
                && !returnType.getDeclaringClass().isAnnotationPresent(NoWrap.class);
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        Result<Object> wrapper = Result.success(body);

        // Controller 直接返回 String 时，转换器已被选定为 StringHttpMessageConverter，
        // 这里必须自行序列化为 JSON 字符串，并修正 Content-Type，否则客户端会收到 text/plain。
        if (StringHttpMessageConverter.class.equals(selectedConverterType)) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return objectMapper.writeValueAsString(wrapper);
        }
        return wrapper;
    }
}
