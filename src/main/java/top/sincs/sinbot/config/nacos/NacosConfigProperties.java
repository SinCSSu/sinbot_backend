package top.sincs.sinbot.config.nacos;

import lombok.Getter;
import lombok.Setter;
import org.springframework.core.env.Environment;

/**
 * Nacos 配置中心的引导参数（{@code nacos.config.*}）。
 *
 * <p>这里只描述「如何连上 Nacos」这一最小集合；真正的业务配置
 * （数据库连接与连接池、Redis、机器人凭据、鉴权）全部放在 Nacos 上的
 * {@code sinbot.yaml}，不再散落在本地配置文件中。</p>
 *
 * <p>Nacos 是配置的唯一来源：拉取不到配置直接终止启动，不做任何本地降级，
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

    /** 配置集 ID */
    private String dataId = "sinbot.yaml";

    /** Nacos 控制台账号（未开启鉴权时留空） */
    private String username = "";

    /** Nacos 控制台密码（未开启鉴权时留空） */
    private String password = "";

    /** 拉取配置的超时时间（毫秒） */
    private long timeout = 10_000L;

    /** 是否监听配置变更并刷新 Environment */
    private boolean refreshEnabled = true;

    /** 从 Environment 读取（占位符已由 Environment 解析，故仍可用 {@code ${NACOS_xxx:默认}}） */
    public static NacosConfigProperties from(Environment environment) {
        NacosConfigProperties properties = new NacosConfigProperties();
        properties.setServerAddr(environment.getProperty(PREFIX + ".server-addr", "127.0.0.1:8848"));
        properties.setNamespace(environment.getProperty(PREFIX + ".namespace", ""));
        properties.setGroup(environment.getProperty(PREFIX + ".group", "DEFAULT_GROUP"));
        properties.setDataId(environment.getProperty(PREFIX + ".data-id", "sinbot.yaml"));
        properties.setUsername(environment.getProperty(PREFIX + ".username", ""));
        properties.setPassword(environment.getProperty(PREFIX + ".password", ""));
        properties.setTimeout(environment.getProperty(PREFIX + ".timeout", Long.class, 10_000L));
        properties.setRefreshEnabled(environment.getProperty(PREFIX + ".refresh-enabled", Boolean.class, true));
        return properties;
    }
}
