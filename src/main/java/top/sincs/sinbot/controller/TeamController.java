package top.sincs.sinbot.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.sincs.sinbot.constant.ErrorCode;
import top.sincs.sinbot.constant.HeaderConstant;
import top.sincs.sinbot.dto.team.CreateTeamDTO;
import top.sincs.sinbot.dto.team.UpdateSettlementDTO;
import top.sincs.sinbot.exception.BusinessException;
import top.sincs.sinbot.security.SecurityUtils;
import top.sincs.sinbot.service.TeamService;
import top.sincs.sinbot.vo.team.TeamDetailVo;
import top.sincs.sinbot.vo.team.TeamInfoVo;

import java.util.List;

@RestController
@RequestMapping("/team")
@RequiredArgsConstructor
@Slf4j
public class TeamController {

    private final TeamService teamService;

    @PostMapping("/createTeam")
    public TeamInfoVo createTeam(@RequestHeader(HeaderConstant.GROUP_NUMBER) String groupNumber,
                                 @Validated @RequestBody CreateTeamDTO team) {
        // DR-08：操作人不入参也不由调用方自称，一律从当前身份取。
        // 这条接口同时服务机器人与管理 App，因此不能写成 @RequestHeader(X-Operator-Qq)——
        // App 不带这个头。两条链路已在 SecurityContext 里合流成 QQ 号，
        // requireOperatorQq() 负责拒绝「机器人带了合法 key 却没声明谁在发指令」的请求。
        String operatorQq = SecurityUtils.requireOperatorQq();
        log.info("创建团队，来自群{} 操作人{}", groupNumber, operatorQq);
        return teamService.createTeam(groupNumber, operatorQq, team);
    }

    @DeleteMapping("/deleteTeam")
    public boolean deleteTeam(@NotBlank @RequestParam("teamId") String teamId) {
        log.warn("删除团队{}!", teamId);
        try {
            teamService.deleteTeam(Long.parseLong(teamId));
            return true;
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.TEAM_ID_FORMAT_ERROR);
        }
    }

    /** 招募中的团队（DR-13）：群内「查看当前团队」 */
    @GetMapping("/getAvailableTeamsByGroupNumber")
    public List<TeamInfoVo> getAvailableTeamsByGroupNumber(@NotBlank @RequestParam("groupNumber") String groupNumber) {
        return teamService.getAvailableTeamByGroup(groupNumber);
    }

    /** 结算窗口内的团队（DR-16）：群内「查询历史团队」，即刚打完待结算的团 */
    @GetMapping("/getHistoryTeamsByGroupNumber")
    public List<TeamInfoVo> getHistoryTeamsByGroupNumber(@NotBlank @RequestParam("groupNumber") String groupNumber) {
        return teamService.getSettlementWindowTeamsByGroup(groupNumber);
    }

    /** 团队详情 + 报名名单（DR-16）：群内「查看团队 团队编号」，编号→team_id 已由机器人换算（OQ-10） */
    @GetMapping("/getTeamDetail")
    public TeamDetailVo getTeamDetail(@NotBlank @RequestParam("groupNumber") String groupNumber,
                                      @RequestParam("teamId") Long teamId) {
        return teamService.getTeamDetail(groupNumber, teamId);
    }

    /**
     * 机器人侧结算回填（DR-26）：服务端校验写入窗口，归档团队一律拒绝。
     *
     * <p>归档后的漏结算只能由管理端 {@code /team/admin/updateSettlement} 补写。</p>
     */
    @PutMapping("/updateSettlement")
    public TeamInfoVo updateSettlement(@NotBlank @RequestParam("groupNumber") String groupNumber,
                                       @RequestParam("teamId") Long teamId,
                                       @Validated @RequestBody UpdateSettlementDTO dto) {
        String operatorQq = SecurityUtils.requireOperatorQq();
        log.info("结算回填，群{} 团队{} 操作人{}", groupNumber, teamId, operatorQq);
        return teamService.updateSettlement(groupNumber, operatorQq, teamId, dto);
    }

    /** 管理端结算补写（DR-26）：不校验时间窗口，归档团队也能写 */
    @PutMapping("/admin/updateSettlement")
    public TeamInfoVo updateSettlementForAdmin(@RequestParam("teamId") Long teamId,
                                               @Validated @RequestBody UpdateSettlementDTO dto) {
        String operatorQq = SecurityUtils.requireOperatorQq();
        log.info("管理端结算补写，团队{} 操作人{}", teamId, operatorQq);
        return teamService.updateSettlementForAdmin(teamId, operatorQq, dto);
    }

    /** 管理端按群查全量团队（P-15）：不带任何时间过滤，含已归档 */
    @GetMapping("/admin/getTeamsByGroupNumber")
    public List<TeamInfoVo> getTeamsByGroupNumber(@NotBlank @RequestParam("groupNumber") String groupNumber) {
        return teamService.getTeamsByGroup(groupNumber);
    }
}
