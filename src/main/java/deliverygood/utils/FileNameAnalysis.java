package deliverygood.utils;

import javafx.util.Pair;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 发货图获取图片尺寸
 *
 * @author chenzengsen
 * @date 2024/3/22 9:42 上午
 */
public class FileNameAnalysis {

    /**
     * 计算图片面积
     *
     * @param fileName 文件名
     * @return 图片面积, 单位 cm
     */
    public static Integer calculateArea(String fileName) {
        Pair<Integer, Integer> pair = getWidthAndHeight(fileName);
        if (Objects.isNull(pair) || Objects.isNull(pair.getKey()) || Objects.isNull(pair.getValue())) {
            return null;
        }
        return pair.getKey() * pair.getValue();
    }

    /**
     * 获取图片尺寸
     *
     * @param fileName
     * @return key:width value:height
     */
    public static Pair<Integer, Integer> getWidthAndHeight(String fileName) {
        String[] sizeArray = fileNameCheck(fileName);
        if (Objects.isNull(sizeArray)) {
            return null;
        }
        Integer width = Integer.parseInt(sizeArray[0]);
        Integer height = Integer.parseInt(sizeArray[1]);
        return new Pair<>(width, height);
    }

    /**
     * 判断文件名是否合格
     *
     * @param fileName
     * @return true 合格 false 不合格
     */
    private static String[] fileNameCheck(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return null;
        }
        String[] splitStr = fileName.split(" ");
        if (splitStr.length < 2) {
            return null;
        }
        String sizeStr = splitStr[1];
        String[] sizeSplitStr = sizeStr.split("-");
        if (sizeSplitStr.length < 2) {
            return null;
        }
        return sizeSplitStr;
    }
}
