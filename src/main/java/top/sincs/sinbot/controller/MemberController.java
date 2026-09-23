package top.sincs.sinbot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import top.sincs.sinbot.constant.ErrorCode;
import top.sincs.sinbot.constant.HeaderConstant;
import top.sincs.sinbot.dto.member.QuitTeamDTO;
import top.sincs.sinbot.dto.member.SignUpDTO;
import top.sincs.sinbot.exception.BusinessException;
import top.sincs.sinbot.service.MemberService;
import top.sincs.sinbot.vo.member.MemberInfoVo;

/**
 * 报名 / 退团。
 *
 * <p>群号与操作人一律走请求头（DR-04 / DR-08），业务入参里不出现这两项。</p>
 */
@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
@Slf4j
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/signUp")
    public MemberInfoVo signUp(@RequestHeader(value = HeaderConstant.GROUP_NUMBER, required = false) String groupNumber,
                               @RequestHeader(value = HeaderConstant.OPERATOR_QQ, required = false) String operatorQq,
                               @Valid @RequestBody SignUpDTO dto) {
        String group = requireGroupNumber(groupNumber);
        String qq = requireOperatorQq(operatorQq);
        log.info("报名，群{} 操作人{} 团队{} 游戏ID{}", group, qq, dto.getTeamId(), dto.getMemberName());
        return memberService.signUp(group, qq, dto);
    }

    @DeleteMapping("/quitTeam")
    public boolean quitTeam(@RequestHeader(value = HeaderConstant.GROUP_NUMBER, required = false) String groupNumber,
                            @RequestHeader(value = HeaderConstant.OPERATOR_QQ, required = false) String operatorQq,
                            @Valid @RequestBody QuitTeamDTO dto) {
        String group = requireGroupNumber(groupNumber);
        String qq = requireOperatorQq(operatorQq);
        log.info("退团，群{} 操作人{} 团队{}", group, qq, dto.getTeamId());
        memberService.quitTeam(group, qq, dto);
        return true;
    }

    private String requireGroupNumber(String groupNumber) {
        if (!StringUtils.hasText(groupNumber)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "缺少请求头 " + HeaderConstant.GROUP_NUMBER);
        }
        return groupNumber.trim();
    }

    private String requireOperatorQq(String operatorQq) {
        if (!StringUtils.hasText(operatorQq)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "缺少请求头 " + HeaderConstant.OPERATOR_QQ);
        }
        return operatorQq.trim();
    }
}
