package top.sincs.sinbot.service;

import top.sincs.sinbot.dto.member.QuitTeamDTO;
import top.sincs.sinbot.dto.member.SignUpDTO;
import top.sincs.sinbot.vo.member.MemberInfoVo;

public interface MemberService {

    /** 报名（DR-18 / DR-20 / DR-21） */
    MemberInfoVo signUp(String groupNumber, String operatorQq, SignUpDTO dto);

    /** 退团，逻辑删除（DR-22 / DR-23 / DR-24 / DR-25） */
    void quitTeam(String groupNumber, String operatorQq, QuitTeamDTO dto);
}
