package top.sincs.sinbot.converter;

import org.mapstruct.Mapper;
import top.sincs.sinbot.entity.Member;
import top.sincs.sinbot.vo.member.MemberInfoVo;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MemberToMemberInfoVoConverter {

    MemberInfoVo converter(Member member);

    List<MemberInfoVo> listConverter(List<Member> members);
}
