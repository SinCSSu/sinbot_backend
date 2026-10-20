package top.sincs.sinbot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.sincs.sinbot.service.TeamService;
import top.sincs.sinbot.vo.team.TeamDetailVo;
import top.sincs.sinbot.vo.team.TeamInfoVo;

import java.util.List;

/**
 * 机器人侧的团队接口：群内开团、团队查询与结算回填。
 *
 * <p>管理端专职的删除与结算补写已拆到 sinbot_app 的 {@code AdminTeamController}。
 * 但 {@code /team/createTeam} 这条 URL 是机器人与管理 App 共用的，单进程内只能存在一份映射，
 * 因此保留在机器人侧——App 不带 {@code X-Api-Key}，会先走 App 的 token 链完成认证，
 * 再由 MVC 路由到这里的同一个方法，行为与拆分前完全一致。</p>
 *
 * <p>群号与操作人都不再出现在方法签名里：二者由 {@code RobotAuthenticationFilter} 在认证阶段
 * 校验并写入 {@code SecurityContextHolder}，取值发生在 service 层（{@code SecurityUtils.requireGroupNumber()}）。
 * controller 只负责转发消息体。注意 App 链（{@code TokenAuthenticationFilter}）目前不解析
 * {@code X-Group-Number}，若将来 App 复用这里的接口，需在那条链上补同样的解析。</p>
 */
@RestController
@RequestMapping("/team")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping("/createTeam")
    public TeamInfoVo createTeam(@RequestBody String msg) {
        return teamService.createTeam(msg);
    }

    //    /** 招募中的团队（DR-13）：群内「查看当前团队」 */
    @PostMapping("/getAvailableTeamsByGroupNumber")
    public List<TeamInfoVo> getAvailableTeamsByGroupNumber() {
        return teamService.getAvailableTeamList();
    }

    /** 结算窗口内的团队（DR-16）：群内「查询历史团队」，即刚打完待结算的团 */
    @PostMapping("/getHistoryTeamsByGroupNumber")
    public List<TeamInfoVo> getHistoryTeamsByGroupNumber() {
        return teamService.getHistoryTeamList();
    }

    /** 团队详情 + 报名名单（DR-16）：群内「查看团队 团队编号」，编号→team_id 已由机器人换算（OQ-10） */
    @PostMapping("/getTeamDetail")
    public TeamDetailVo getTeamDetail(@RequestBody String msg) {
        return teamService.getTeamDetail(msg);
    }

    @PostMapping("/removeTeam")
    public boolean removeTeam(@RequestBody String msg) {
        return teamService.removeTeam(msg);
    }
//
//    /**
//     * 机器人侧结算回填（DR-26）：服务端校验写入窗口，归档团队一律拒绝。
//     *
//     * <p>归档后的漏结算只能由管理端 {@code /team/admin/updateSettlement} 补写。</p>
//     */
//    @PutMapping("/updateSettlement")
//    public TeamInfoVo updateSettlement(@RequestParam("teamId") Long teamId,
//                                       @Validated @RequestBody UpdateSettlementDTO dto) {
//        return teamService.updateSettlement(teamId, dto);
//    }
}
