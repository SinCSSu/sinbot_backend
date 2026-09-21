package top.sincs.sinbot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.sincs.sinbot.dto.team.CreateTeamDTO;
import top.sincs.sinbot.service.TeamService;
import top.sincs.sinbot.vo.team.TeamInfoVo;


@RestController
@RequestMapping("/team")
@RequiredArgsConstructor
@Slf4j
public class TeamController {

    private final TeamService teamService;

    @PostMapping("/createTeam")
    public TeamInfoVo createTeam(@Validated CreateTeamDTO team) {
        log.info("创建团队，来自{}:{}", team.getGroupNumber(), team.getCreatedBy());
        return teamService.createTeam(team);
    }
}
