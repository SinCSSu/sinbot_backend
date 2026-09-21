package top.sincs.sinbot.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.sincs.sinbot.constant.ErrorCode;
import top.sincs.sinbot.dto.team.CreateTeamDTO;
import top.sincs.sinbot.exception.BusinessException;
import top.sincs.sinbot.service.TeamService;
import top.sincs.sinbot.vo.team.TeamInfoVo;

import java.util.List;


@RestController
@RequestMapping("/team")
@RequiredArgsConstructor
@Slf4j
public class TeamController {

    private final TeamService teamService;

    @PostMapping("/createTeam")
    public TeamInfoVo createTeam(@Validated @RequestBody CreateTeamDTO team) {
        log.info("创建团队，来自{}:{}", team.getGroupNumber(), team.getCreatedBy());
        return teamService.createTeam(team);
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

    @GetMapping("/getAvailableTeamsByGroupNumber")
    public List<TeamInfoVo> getAvailableTeamsByGroupNumber(@NotBlank @RequestParam("groupNumber") String groupNumber) {
        return teamService.getAvailableTeamByGroup(groupNumber);
    }
}
