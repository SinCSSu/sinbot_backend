package top.sincs.sinbot.config.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

/**
 * Nacos 配置的拉取与解析。
 *
 * <p>配置在 Nacos 上以 YAML 存放，这里复用 Spring Boot 的
 * {@link YamlPropertySourceLoader} 解析，行为与本地 application.yaml 完全一致
 * （嵌套结构展平成 {@code a.b.c} 形式的键）。</p>
 */
public final class NacosConfigLoader {

    /** PropertySource 命名前缀，便于在 actuator env 与日志中辨认来源 */
    public static final String PROPERTY_SOURCE_PREFIX = "nacos:";

    private NacosConfigLoader() {
    }

    /** 创建 Nacos 配置服务客户端 */
    public static ConfigService createConfigService(NacosConfigProperties properties) throws NacosException {
        Properties nacosProperties = new Properties();
        nacosProperties.put(PropertyKeyConst.SERVER_ADDR, properties.getServerAddr());
        if (StringUtils.hasText(properties.getNamespace())) {
            nacosProperties.put(PropertyKeyConst.NAMESPACE, properties.getNamespace());
        }
        if (StringUtils.hasText(properties.getUsername())) {
            nacosProperties.put(PropertyKeyConst.USERNAME, properties.getUsername());
        }
        if (StringUtils.hasText(properties.getPassword())) {
            nacosProperties.put(PropertyKeyConst.PASSWORD, properties.getPassword());
        }
        return NacosFactory.createConfigService(nacosProperties);
    }

    /** 解析 YAML 文本为 PropertySource */
    public static PropertySource<?> loadYaml(String name, String content) throws IOException {
        List<PropertySource<?>> loaded = new YamlPropertySourceLoader()
                .load(name, new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8)));
        if (loaded.isEmpty()) {
            throw new IllegalStateException("配置内容为空或不是合法 YAML: " + name);
        }
        if (loaded.size() > 1) {
            throw new IllegalStateException("配置只支持单个 YAML 文档，实际解析到 " + loaded.size() + " 个: " + name);
        }
        return loaded.get(0);
    }
}
