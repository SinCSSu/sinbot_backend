package top.sincs.sinbot.config.nacos;

import lombok.Getter;
import lombok.Setter;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Nacos 配置中心的引导参数（{@code nacos.config.*}）。
 *
 * <p>这里只描述「如何连上 Nacos、拉哪几份配置」这一最小集合；真正的业务配置
 * （数据库连接与连接池、Redis、机器人凭据、鉴权）全部放在 Nacos 上，
 * 不再散落在本地配置文件中。</p>
 *
 * <p>Nacos 是配置的唯一来源：任一份配置拉不到都直接终止启动，不做任何本地降级，
 * 避免实例带着过期或错误的配置跑起来。引导参数自身仍写在
 * {@code application.properties}，并支持环境变量 / 命令行覆盖（如 {@code NACOS_SERVER_ADDR}）。</p>
 */
@Getter
@Setter
public class NacosConfigProperties {

    private static final String PREFIX = "nacos.config";

    /** Nacos 服务地址，格式 host:port，集群用逗号分隔 */
    private String serverAddr = "127.0.0.1:8848";

    /** 命名空间 ID（public 命名空间留空） */
    private String namespace = "";

    /** 配置分组 */
    private String group = "DEFAULT_GROUP";

    /**
     * 要拉取的配置项（Data ID）列表，按声明顺序加载，<b>后者覆盖前者</b>。
     *
     * <p>取自 {@code nacos.config.data-ids}（逗号分隔，支持多份）；
     * 未配置时回退到 {@code nacos.config.data-id}（单份）。</p>
     */
    private List<String> dataIds = List.of("sinbot.yaml");

    /** Nacos 控制台账号（未开启鉴权时留空） */
    private String username = "";

    /** Nacos 控制台密码（未开启鉴权时留空） */
    private String password = "";

    /** 拉取单份配置的超时时间（毫秒） */
    private long timeout = 10_000L;

    /** 是否监听配置变更并刷新 Environment */
    private boolean refreshEnabled = true;

    /** 从 Environment 读取（占位符已由 Environment 解析，故仍可用 {@code ${NACOS_xxx:默认}}） */
    public static NacosConfigProperties from(Environment environment) {
        NacosConfigProperties properties = new NacosConfigProperties();
        properties.setServerAddr(environment.getProperty(PREFIX + ".server-addr", "127.0.0.1:8848"));
        properties.setNamespace(environment.getProperty(PREFIX + ".namespace", ""));
        properties.setGroup(environment.getProperty(PREFIX + ".group", "DEFAULT_GROUP"));
        properties.setDataIds(readDataIds(environment));
        properties.setUsername(environment.getProperty(PREFIX + ".username", ""));
        properties.setPassword(environment.getProperty(PREFIX + ".password", ""));
        properties.setTimeout(environment.getProperty(PREFIX + ".timeout", Long.class, 10_000L));
        properties.setRefreshEnabled(environment.getProperty(PREFIX + ".refresh-enabled", Boolean.class, true));
        return properties;
    }

    /**
     * 解析 Data ID 列表：优先 {@code data-ids}（逗号分隔，多份），
     * 未配置时回退 {@code data-id}（单份）；保持声明顺序、去重。
     */
    private static List<String> readDataIds(Environment environment) {
        LinkedHashSet<String> dataIds = new LinkedHashSet<>();
        addDataIds(dataIds, environment.getProperty(PREFIX + ".data-ids", ""));
        if (dataIds.isEmpty()) {
            addDataIds(dataIds, environment.getProperty(PREFIX + ".data-id", ""));
        }
        if (dataIds.isEmpty()) {
            dataIds.add("sinbot.yaml");
        }
        return new ArrayList<>(dataIds);
    }

    private static void addDataIds(LinkedHashSet<String> target, String raw) {
        if (!StringUtils.hasText(raw)) {
            return;
        }
        for (String dataId : raw.split(",")) {
            String trimmed = dataId.trim();
            if (!trimmed.isEmpty()) {
                target.add(trimmed);
            }
        }
    }
}
