package util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author chenzengsen
 * @date 2024/3/24 11:07 上午
 */
public class DataUtils {
    /**
     * 根据指定的时间格式，返回格式化后的时间字符串。
     *
     * @param format 时间格式字符串。
     * @return 格式化后的时间字符串。
     */
    public static String formatDateTime(String format) {
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        return simpleDateFormat.format(date);
    }
}
