package top.sincs.sinbot.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.sincs.sinbot.constant.ErrorCode;
import top.sincs.sinbot.dto.team.UpdateSettlementDTO;
import top.sincs.sinbot.exception.BusinessException;
import top.sincs.sinbot.security.SecurityUtils;
import top.sincs.sinbot.service.TeamService;
import top.sincs.sinbot.vo.team.TeamInfoVo;

import java.util.List;

/**
 * 管理 App 侧的团队接口：删除团队、结算补写、按群查全量团队。
 *
 * <p>群内开团与团队查询归属机器人侧（sinbot_bot 的 {@code TeamController}）。
 * {@code /team/createTeam} 是两个入口共用的 URL，单进程内只能有一份映射，
 * 因此本控制器不重复声明它，管理端开团直接命中机器人侧的那个方法。</p>
 */
@RestController
@RequestMapping("/team")
@RequiredArgsConstructor
@Slf4j
public class AdminTeamController {

    private final TeamService teamService;

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
