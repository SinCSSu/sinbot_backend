package top.sincs.sinbot.config.nacos;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/**
 * 监听 Nacos 配置变更并替换 Environment 中的 PropertySource。
 *
 * <p>注意：这里只更新 Environment，已注入完成的 {@code @Value} 与
 * {@code @ConfigurationProperties}（连接池、Redis 等）不会自动重绑，
 * 这类配置变更请重启实例生效。</p>
 */
@Slf4j
@Component
public class NacosConfigRefresher implements ApplicationRunner, DisposableBean {

    private final ConfigurableEnvironment environment;

    private final NacosConfigProperties properties;

    private volatile ConfigService configService;

    public NacosConfigRefresher(ConfigurableEnvironment environment) {
        this.environment = environment;
        this.properties = NacosConfigProperties.from(environment);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isRefreshEnabled()) {
            log.info("Nacos 配置变更监听已关闭（nacos.config.refresh-enabled=false）");
            return;
        }
        String name = NacosConfigLoader.PROPERTY_SOURCE_PREFIX + properties.getDataId();
        try {
            configService = NacosConfigLoader.createConfigService(properties);
            configService.addListener(properties.getDataId(), properties.getGroup(), new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    refresh(name, configInfo);
                }
            });
            log.info("已监听 Nacos 配置变更 [dataId={}, group={}]", properties.getDataId(), properties.getGroup());
        } catch (Exception ex) {
            log.error("注册 Nacos 配置监听失败，配置变更不会自动生效: {}", ex.getMessage(), ex);
        }
    }

    private void refresh(String name, String configInfo) {
        try {
            PropertySource<?> source = NacosConfigLoader.loadYaml(name, configInfo);
            if (environment.getPropertySources().contains(name)) {
                environment.getPropertySources().replace(name, source);
            } else {
                environment.getPropertySources().addFirst(source);
            }
            log.info("Nacos 配置已更新并替换 PropertySource [{}]；连接池、Redis 等已注入的 Bean 需重启生效", name);
        } catch (Exception ex) {
            log.error("Nacos 配置刷新失败，保持原配置不变: {}", ex.getMessage(), ex);
        }
    }

    @Override
    public void destroy() {
        ConfigService service = this.configService;
        if (service != null) {
            try {
                service.shutDown();
            } catch (Exception ex) {
                log.warn("关闭 Nacos 配置监听失败: {}", ex.getMessage());
            }
        }
    }
}
