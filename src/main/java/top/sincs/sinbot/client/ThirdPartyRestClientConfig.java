package top.sincs.sinbot.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import top.sincs.sinbot.common.ErrorCode;
import top.sincs.sinbot.exception.BusinessException;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * 第三方服务的 {@link RestClient} 装配。
 *
 * <p>命名 Bean（{@code thirdPartyRestClient}）以隔离不同外部服务的客户端实例；
 * 传输层错误统一转成 {@link BusinessException}，避免 {@code RestClientResponseException} 泄漏到上层造成分支分裂。</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ThirdPartyClientProperties.class)
public class ThirdPartyRestClientConfig {

    public static final String THIRD_PARTY_REST_CLIENT = "thirdPartyRestClient";

    @Bean(THIRD_PARTY_REST_CLIENT)
    public RestClient thirdPartyRestClient(ThirdPartyClientProperties properties) {
        return applyDefaults(RestClient.builder(), properties).build();
    }

    @Bean
    public ResultRestClient resultRestClient(
            @Qualifier(THIRD_PARTY_REST_CLIENT) RestClient restClient,
            ThirdPartyClientProperties properties) {
        return new ResultRestClient(restClient, properties.isUnwrapEnabled());
    }

    /**
     * 应用超时、状态码处理与日志拦截器；提取为静态方法便于单元测试复用同一套配置。
     */
    public static RestClient.Builder applyDefaults(RestClient.Builder builder,
                                                   ThirdPartyClientProperties properties) {
        if (StringUtils.hasText(properties.getBaseUrl())) {
            builder.baseUrl(properties.getBaseUrl());
        }
        return builder
                .requestFactory(requestFactory(properties))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptor(new MetaDataLoggingInterceptor())
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    HttpStatusCode status = response.getStatusCode();
                    log.warn("第三方服务调用失败: {} {} -> {}", request.getMethod(), request.getURI(), status.value());
                    throw new BusinessException(ErrorCode.UPSTREAM_ERROR,
                            ErrorCode.UPSTREAM_ERROR.getMessage() + "（HTTP " + status.value() + "）");
                });
    }

    /**
     * 显式设置连接/读取超时，避免上游服务慢导致本服务线程池被耗尽。
     */
    private static JdkClientHttpRequestFactory requestFactory(ThirdPartyClientProperties properties) {
        Duration connectTimeout = properties.getConnectTimeout();
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(properties.getReadTimeout());
        return factory;
    }

    /**
     * 只记录方法、URI、状态码与耗时，不记录请求/响应体（第三方接口可能含敏感信息）。
     */
    private static class MetaDataLoggingInterceptor implements ClientHttpRequestInterceptor {

        @Override
        public ClientHttpResponse intercept(HttpRequest request,
                                            byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {
            long start = System.currentTimeMillis();
            ClientHttpResponse response = execution.execute(request, body);
            if (log.isDebugEnabled()) {
                log.debug("第三方请求: {} {} -> {}, 耗时 {}ms",
                        request.getMethod(), request.getURI(), response.getStatusCode().value(),
                        System.currentTimeMillis() - start);
            }
            return response;
        }
    }
}
