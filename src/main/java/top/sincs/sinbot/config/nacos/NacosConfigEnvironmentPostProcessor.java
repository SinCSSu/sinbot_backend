package top.sincs.sinbot.config.nacos;

import com.alibaba.nacos.api.config.ConfigService;
import org.apache.commons.logging.Log;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

/**
 * 在 Environment 就绪后（本地 application.properties 之后）从 Nacos 拉取配置，
 * 并以最高优先级注入 Environment。
 *
 * <p>顺序：必须排在 {@code ConfigDataEnvironmentPostProcessor} 之后，
 * 否则读不到 {@code nacos.config.*} 引导参数。</p>
 *
 * <p>多配置项：按 {@code nacos.config.data-ids} 的声明顺序逐份加载并
 * {@code addFirst}，因此<b>后声明的 Data ID 优先级更高</b>，可用于
 * 「公共配置 + 应用私有配置」的分层覆盖。</p>
 *
 * <p>优先级：Nacos 配置整体高于本地 application.properties、环境变量与命令行参数。</p>
 *
 * <p>失败策略：Nacos 是配置的唯一来源，任一份配置连不上、不存在或解析失败都终止启动，
 * 不回退本地配置，避免实例带着过期 / 错误的配置对外提供服务。</p>
 */
public class NacosConfigEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /** 排在 ConfigData（application.properties）之后 */
    public static final int ORDER = Ordered.LOWEST_PRECEDENCE - 100;

    private final Log log;

    public NacosConfigEnvironmentPostProcessor(DeferredLogFactory logFactory) {
        this.log = logFactory.getLog(NacosConfigEnvironmentPostProcessor.class);
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        NacosConfigProperties properties = NacosConfigProperties.from(environment);
        for (String dataId : properties.getDataIds()) {
            String name = NacosConfigLoader.PROPERTY_SOURCE_PREFIX + dataId;
            try {
                PropertySource<?> source = fetchFromNacos(properties, dataId, name);
                // addFirst：后加载的 Data ID 排在更前面，即后声明者优先
                environment.getPropertySources().addFirst(source);
                log.info("已加载配置源 [" + name + "]，来源=Nacos(" + properties.getServerAddr()
                        + ")，配置项 " + countOf(source) + " 个");
            } catch (Exception ex) {
                throw new IllegalStateException(
                        "从 Nacos 加载配置失败，启动中止 [dataId=" + dataId
                                + ", group=" + properties.getGroup()
                                + ", serverAddr=" + properties.getServerAddr()
                                + "]，原因：" + ex.getMessage()
                                + "（若服务端开启鉴权，请配置 nacos.config.username / password）", ex);
            }
        }
    }

    private PropertySource<?> fetchFromNacos(NacosConfigProperties properties, String dataId, String name)
            throws Exception {
        ConfigService configService = NacosConfigLoader.createConfigService(properties);
        try {
            String content = configService.getConfig(dataId, properties.getGroup(), properties.getTimeout());
            if (!StringUtils.hasText(content)) {
                throw new IllegalStateException("Nacos 上配置不存在或内容为空");
            }
            return NacosConfigLoader.loadYaml(name, content);
        } finally {
            // 变更监听由 NacosConfigRefresher 另建连接负责，这里拉完即释放
            shutdownQuietly(configService);
        }
    }

    private String countOf(PropertySource<?> source) {
        return (source instanceof EnumerablePropertySource<?> enumerable)
                ? String.valueOf(enumerable.getPropertyNames().length)
                : "?";
    }

    private void shutdownQuietly(ConfigService configService) {
        try {
            configService.shutDown();
        } catch (Exception ex) {
            log.warn("关闭 Nacos ConfigService 失败: " + ex.getMessage());
        }
    }
}
