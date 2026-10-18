package top.sincs.sinbot.utils;

import com.xkzhangsan.time.nlp.TimeNLP;
import com.xkzhangsan.time.nlp.TimeNLPUtil;
import top.sincs.sinbot.constant.ErrorCode;
import top.sincs.sinbot.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.List;

public class DateUtils {
    public static LocalDateTime parseChineseDateToLocalDateTime(String timeStr) {
        try {
            List<TimeNLP> parseResult = TimeNLPUtil.parse(timeStr);
            if (parseResult != null && !parseResult.isEmpty()) {
                return parseResult.getFirst().getLocalDateTime();
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.TIME_FORMAT_ERROR, "时间格式化错误");
        }
        throw new BusinessException(ErrorCode.TIME_FORMAT_ERROR, "未识别到时间");
    }
}
