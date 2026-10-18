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
import top.sincs.sinbot.security.SecurityUtils;
import top.sincs.sinbot.service.MemberService;
import top.sincs.sinbot.vo.member.MemberInfoVo;

/**
 * 报名 / 退团。
 *
 * <p>群号走请求头 {@code X-Group-Number}（DR-04），业务入参里不出现这两项。</p>
 */
@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
@Slf4j
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/signUp")
    public MemberInfoVo signUp(@RequestHeader(value = HeaderConstant.GROUP_NUMBER, required = false) String groupNumber,
                               @Valid @RequestBody SignUpDTO dto) {
        String group = requireGroupNumber(groupNumber);
        // DR-08 / DR-18：member_qq_number 与 created_by 同源，都来自当前身份而非请求参数。
        String qq = SecurityUtils.requireOperatorQq();
        log.info("报名，群{} 操作人{} 团队{} 游戏ID{}", group, qq, dto.getTeamId(), dto.getMemberName());
        return memberService.signUp(group, qq, dto);
    }

    @DeleteMapping("/quitTeam")
    public boolean quitTeam(@RequestHeader(value = HeaderConstant.GROUP_NUMBER, required = false) String groupNumber,
                            @Valid @RequestBody QuitTeamDTO dto) {
        String group = requireGroupNumber(groupNumber);
        // DR-24：定位报名记录的 QQ 同样取自当前身份，不给调用方冒名的机会
        String qq = SecurityUtils.requireOperatorQq();
        log.info("退团，群{} 操作人{} 团队{}", group, qq, dto.getTeamId());
        memberService.quitTeam(group, qq, dto);
        return true;
    }

    /**
     * 群号是调用方显式声明的业务分区键（DR-04），没有旁证可推导，只能由请求头带，
     * 因此这条校验仍然留在 controller——与「操作人」不同：操作人已由认证通道写入身份上下文。
     */
    private String requireGroupNumber(String groupNumber) {
        if (!StringUtils.hasText(groupNumber)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "缺少请求头 " + HeaderConstant.GROUP_NUMBER);
        }
        return groupNumber.trim();
    }
}
