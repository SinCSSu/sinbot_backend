package top.sincs.sinbot.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import top.sincs.sinbot.constant.ErrorCode;
import top.sincs.sinbot.exception.BusinessException;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 带自动脱壳能力的 HTTP 客户端，基于同步 {@link RestClient}。
 *
 * <p>调用方只需声明目标类型 {@code T}（支持 {@code Foo}、{@code List<Foo>} 等任意嵌套泛型），
 * 本客户端负责把上游响应外壳 {@link RemoteResult} 中的 {@code data} 提取出来返回；
 * 上游返回 {@code code != 0} 时抛出携带原始 code/message 的 {@link BusinessException}。</p>
 *
 * <p>若第三方接口不遵循 {@code code/data} 结构，请使用 {@link #getForResult(String, ParameterizedTypeReference)}
 * 或 {@link #raw()} 逃生。</p>
 */
public class ResultRestClient {

    private final RestClient restClient;

    private final boolean unwrapEnabled;

    /**
     * 目标类型到「 {@code RemoteResult<目标类型>} 类型引用」的缓存，避免每次调用重复反射构造。
     */
    private final Map<Type, ParameterizedTypeReference<?>> envelopeCache = new ConcurrentHashMap<>();

    public ResultRestClient(RestClient restClient) {
        this(restClient, true);
    }

    public ResultRestClient(RestClient restClient, boolean unwrapEnabled) {
        this.restClient = restClient;
        this.unwrapEnabled = unwrapEnabled;
    }

    /* ---------------- 查询类 ---------------- */

    public <T> T getForData(String url, Class<T> targetType) {
        return extractData(restClient.get().uri(url), targetType);
    }

    public <T> T getForData(String url, ParameterizedTypeReference<T> targetType) {
        return extractData(restClient.get().uri(url), targetType.getType());
    }

    /* ---------------- 写入类 ---------------- */

    public <T> T postForData(String url, Object body, Class<T> targetType) {
        return extractData(restClient.post().uri(url).body(body), targetType);
    }

    public <T> T postForData(String url, Object body, ParameterizedTypeReference<T> targetType) {
        return extractData(restClient.post().uri(url).body(body), targetType.getType());
    }

    /* ---------------- 逃生通道 ---------------- */

    /**
     * 保留完整外壳，由调用方自行判断 {@code code}，本方法不会因业务码失败而抛异常。
     */
    public <T> RemoteResult<T> getForResult(String url, ParameterizedTypeReference<T> targetType) {
        return fetchEnvelope(restClient.get().uri(url), targetType.getType());
    }

    /**
     * 自定义请求后脱壳，用于需要附加请求头、参数或使用非 GET/POST 方法的场景。
     */
    public <T> T exchangeForData(
            Function<RestClient.RequestHeadersUriSpec<?>, RestClient.RequestHeadersSpec<?>> exchangeFunction,
            ParameterizedTypeReference<T> targetType) {
        RestClient.RequestHeadersSpec<?> spec = exchangeFunction.apply(restClient.get());
        return extractData(spec, targetType.getType());
    }

    /**
     * 暴露底层 {@link RestClient}，供完全不遵循统一结构的第三方接口使用。
     */
    public RestClient raw() {
        return restClient;
    }

    /* ---------------- 内部实现 ---------------- */

    /**
     * 请求并按配置为 {@code T} 提取数据；关闭脱壳时直接按 {@code T} 解析响应体。
     */
    private <T> T extractData(RestClient.RequestHeadersSpec<?> spec, Type targetType) {
        if (!unwrapEnabled) {
            return spec.retrieve().body(ParameterizedTypeReference.forType(targetType));
        }
        RemoteResult<T> envelope = fetchEnvelope(spec, targetType);
        return unwrap(envelope);
    }

    private <T> RemoteResult<T> fetchEnvelope(RestClient.RequestHeadersSpec<?> spec, Type targetType) {
        return spec.retrieve().body(envelopeOf(targetType));
    }

    /**
     * 脱壳核心：成功返回 {@code data}；失败抛出携带上游 code/message 的异常。
     */
    private <T> T unwrap(RemoteResult<T> envelope) {
        if (envelope == null) {
            throw new BusinessException(ErrorCode.UPSTREAM_RESPONSE_INVALID, "上游响应体为空，无法解析");
        }
        if (!envelope.isSuccess()) {
            String message = StringUtils.hasText(envelope.getMessage())
                    ? envelope.getMessage()
                    : ErrorCode.UPSTREAM_ERROR.getMessage();
            throw new BusinessException(envelope.getCode(), message);
        }
        return envelope.getData();
    }

    /**
     * 把目标类型 {@code T} 提升为 {@code RemoteResult<T>} 的类型引用。
     *
     * <p>Java 无法直接书写 {@code new ParameterizedTypeReference<RemoteResult<T>>(){}}（泛型在运行时被擦除），
     * 因此通过 {@link ResolvableType} 动态构造类型，再交由 {@link ParameterizedTypeReference#forType} 转换。</p>
     */
    @SuppressWarnings("unchecked")
    private <T> ParameterizedTypeReference<RemoteResult<T>> envelopeOf(Type targetType) {
        return (ParameterizedTypeReference<RemoteResult<T>>) envelopeCache.computeIfAbsent(targetType,
                type -> ParameterizedTypeReference.forType(
                        ResolvableType.forClassWithGenerics(RemoteResult.class, ResolvableType.forType(type))
                                .getType()));
    }
}
