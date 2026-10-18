package top.sincs.sinbot.service;

import top.sincs.sinbot.dto.team.CreateTeamDTO;
import top.sincs.sinbot.dto.team.UpdateSettlementDTO;
import top.sincs.sinbot.entity.Team;
import top.sincs.sinbot.vo.team.TeamDetailVo;
import top.sincs.sinbot.vo.team.TeamInfoVo;

import java.util.List;

public interface TeamService {

    /**
     * 创建团队。
     *
     * <p>群号来自请求头 {@code X-Group-Number}（DR-04）；操作人由 controller 从当前身份取好后传入（DR-08），
     * 机器人那条链路要求 {@code X-Operator-Qq} 必填。service 不再自行读身份上下文。</p>
     */
    TeamInfoVo createTeam(String groupNumber, String operatorQq, CreateTeamDTO team);

    void deleteTeam(Long teamId);

    /** 招募中的团队列表（DR-13）：群员的「查看当前团队」 */
    List<TeamInfoVo> getAvailableTeamByGroup(String groupNumber);

    /** 结算窗口内的团队列表（DR-16）：群员的「查询历史团队」 */
    List<TeamInfoVo> getSettlementWindowTeamsByGroup(String groupNumber);

    /** 管理端按群查全量团队，不带时间过滤（DR-02 / P-15） */
    List<TeamInfoVo> getTeamsByGroup(String groupNumber);

    /**
     * 取「属于本群 + 处于招募中」的团队，超时则拒绝。
     *
     * <p>入参是 {@code team_id} 而非列表序号：编号 → ID 的换算在机器人侧完成（OQ-10），
     * 服务端只做「属于本群」兜底与阶段断言。</p>
     */
    Team getRecruitingTeam(String groupNumber, Long teamId);

    /** 团队详情 + 报名名单（「查看团队 团队编号」，机器人换算后传 team_id） */
    TeamDetailVo getTeamDetail(String groupNumber, Long teamId);

    /** 机器人侧结算：校验写入窗口为「已开始且未归档」（DR-26 / §4.5 说明 4） */
    TeamInfoVo updateSettlement(String groupNumber, String operatorQq, Long teamId, UpdateSettlementDTO dto);

    /** 管理端结算：不校验时间窗口，归档团队也能补写（DR-26） */
    TeamInfoVo updateSettlementForAdmin(Long teamId, String operatorQq, UpdateSettlementDTO dto);
}
