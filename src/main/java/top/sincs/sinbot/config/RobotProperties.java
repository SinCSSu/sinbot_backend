package top.sincs.sinbot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 机器人凭据配置（DR-03）。
 *
 * <p>每个机器人实例一把独立 key，便于单独吊销与按 key 区分日志来源。
 * 值由 {@code sinbot.robot.api-keys} 注入，配置集中维护在 Nacos 的 {@code sinbot.yaml}，
 * 不建凭据表。</p>
 *
 * <p>未配置时列表为空，机器人侧请求一律拒绝——fail closed，不提供「免认证」退化路径。</p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sinbot.robot")
public class RobotProperties {

    private List<String> apiKeys = List.of();

    public boolean matches(String apiKey) {
        return apiKey != null && apiKeys.stream().anyMatch(apiKey::equals);
    }
}
