package top.sincs.sinbot.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import top.sincs.sinbot.constant.ErrorCode;
import top.sincs.sinbot.exception.BusinessException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 自动脱壳客户端的行为契约测试。
 */
class ResultRestClientTest {

    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    /** 目标类型，验证脱壳后是否正确映射。 */
    record TeamDto(Long teamId, String teamName) {
    }

    private MockRestServiceServer server;

    private ResultRestClient client;

    @BeforeEach
    void setUp() {
        ThirdPartyClientProperties properties = new ThirdPartyClientProperties();
        RestClient.Builder builder =
                ThirdPartyRestClientConfig.applyDefaults(RestClient.builder(), properties);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ResultRestClient(builder.build(), properties.isUnwrapEnabled());
    }

    @Test
    @DisplayName("正常响应：自动剥离外壳并映射为强类型对象")
    void shouldUnwrapDataIntoTargetType() {
        server.expect(requestTo("/teams/1"))
                .andRespond(withSuccess("""
                        {"code":0,"message":"OK","data":{"teamId":1,"teamName":"alpha"}}
                        """, JSON));

        TeamDto actual = client.getForData("/teams/1", TeamDto.class);

        assertThat(actual.teamId()).isEqualTo(1L);
        assertThat(actual.teamName()).isEqualTo("alpha");
        server.verify();
    }

    @Test
    @DisplayName("泛型嵌套：支持 List<T> 这类带泛型的目标类型")
    void shouldUnwrapGenericNestedData() {
        server.expect(requestTo("/teams"))
                .andRespond(withSuccess("""
                        {"code":0,"message":"OK","data":[{"teamId":1,"teamName":"alpha"},{"teamId":2,"teamName":"beta"}]}
                        """, JSON));

        List<TeamDto> actual = client.getForData("/teams", new ParameterizedTypeReference<List<TeamDto>>() {
        });

        assertThat(actual).hasSize(2);
        assertThat(actual.get(1).teamName()).isEqualTo("beta");
        server.verify();
    }

    @Test
    @DisplayName("业务失败：上游 code != 0 时抛出携带原始 code/message 的 BusinessException")
    void shouldThrowBusinessExceptionWhenUpstreamCodeNotZero() {
        server.expect(requestTo("/teams/9"))
                .andRespond(withSuccess("""
                        {"code":20001,"message":"数据不存在"}
                        """, JSON));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> client.getForData("/teams/9", TeamDto.class));

        assertThat(exception.getCode()).isEqualTo(20001);
        assertThat(exception.getMessage()).isEqualTo("数据不存在");
        server.verify();
    }

    @Test
    @DisplayName("传输层失败：HTTP 5xx 映射为 UPSTREAM_ERROR 错误码")
    void shouldThrowBusinessExceptionOnHttpError() {
        server.expect(requestTo("/teams/1"))
                .andRespond(withServerError());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> client.getForData("/teams/1", TeamDto.class));

        assertThat(exception.getCode()).isEqualTo(ErrorCode.UPSTREAM_ERROR.getCode());
        server.verify();
    }

    @Test
    @DisplayName("空数据：code == 0 且 data 缺失时返回 null 而非抛异常")
    void shouldReturnNullWhenDataIsAbsent() {
        server.expect(requestTo("/teams/8"))
                .andRespond(withSuccess("""
                        {"code":0,"message":"OK"}
                        """, JSON));

        TeamDto actual = client.getForData("/teams/8", TeamDto.class);

        assertThat(actual).isNull();
        server.verify();
    }

    @Test
    @DisplayName("写入请求：POST 响应同样自动脱壳")
    void shouldUnwrapDataFromPostRequest() {
        server.expect(requestTo("/teams"))
                .andRespond(withSuccess("""
                        {"code":0,"message":"OK","data":{"teamId":9,"teamName":"gamma"}}
                        """, JSON));

        TeamDto actual = client.postForData("/teams", Map.of("teamName", "gamma"), TeamDto.class);

        assertThat(actual.teamId()).isEqualTo(9L);
        server.verify();
    }

    @Test
    @DisplayName("关闭脱壳：响应体本身就是目标类型时直接解析，不再按外壳处理")
    void shouldDeserializeDirectlyWhenUnwrapDisabled() {
        RestClient.Builder builder =
                ThirdPartyRestClientConfig.applyDefaults(RestClient.builder(), new ThirdPartyClientProperties());
        MockRestServiceServer rawServer = MockRestServiceServer.bindTo(builder).build();
        ResultRestClient rawClient = new ResultRestClient(builder.build(), false);
        rawServer.expect(requestTo("/raw/teams/1"))
                .andRespond(withSuccess("""
                        {"teamId":1,"teamName":"alpha"}
                        """, JSON));

        TeamDto actual = rawClient.getForData("/raw/teams/1", TeamDto.class);

        assertThat(actual.teamName()).isEqualTo("alpha");
        rawServer.verify();
    }

    @Test
    @DisplayName("逃生通道：需要保留原始外壳时不抛异常，由调用方自行判断")
    void shouldKeepEnvelopeWhenRequestedExplicitly() {
        server.expect(requestTo("/teams/9"))
                .andRespond(withSuccess("""
                        {"code":20001,"message":"数据不存在"}
                        """, JSON));

        RemoteResult<TeamDto> actual =
                client.getForResult("/teams/9", new ParameterizedTypeReference<TeamDto>() {
                });

        assertThat(actual.isSuccess()).isFalse();
        assertThat(actual.getCode()).isEqualTo(20001);
        assertThat(actual.getMessage()).isEqualTo("数据不存在");
        server.verify();
    }
}
