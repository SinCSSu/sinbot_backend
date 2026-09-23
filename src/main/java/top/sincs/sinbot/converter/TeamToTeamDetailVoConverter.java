package top.sincs.sinbot.converter;

import org.mapstruct.Mapper;
import top.sincs.sinbot.entity.Team;
import top.sincs.sinbot.vo.team.TeamDetailVo;

/**
 * 团队详情转换器。
 *
 * <p>刻意与 {@link TeamToTeamInfoVoConverter} 分开：两个 target 是父子类型，
 * 放进同一个 Mapper 会让 MapStruct 在映射集合元素时判定方法歧义。</p>
 */
@Mapper(componentModel = "spring")
public interface TeamToTeamDetailVoConverter {

    TeamDetailVo toDetail(Team team);
}
