package top.sincs.sinbot.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 第三方服务客户端配置，绑定 {@code third-party.*}。
 *
 * <p>敏感或环境相关的地址信息不写在配置文件里，由 {@code application.properties}
 * 通过 {@code ${THIRD_PARTY_BASE_URL:}} 占位符从启动参数/环境变量注入。</p>
 */
@ConfigurationProperties(prefix = "third-party")
public class ThirdPartyClientProperties {

    /**
     * 第三方服务根地址；为空时调用方需传入完整绝对 URL。
     */
    private String baseUrl = "";

    private Duration connectTimeout = Duration.ofSeconds(3);

    private Duration readTimeout = Duration.ofSeconds(10);

    /**
     * 是否自动剥离响应外壳。关闭后 {@link ResultRestClient} 返回完整的 {@link RemoteResult}。
     */
    private boolean unwrapEnabled = true;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public boolean isUnwrapEnabled() {
        return unwrapEnabled;
    }

    public void setUnwrapEnabled(boolean unwrapEnabled) {
        this.unwrapEnabled = unwrapEnabled;
    }
}
