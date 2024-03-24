package deliverygood.model;

import lombok.Data;

/**
 * @author chenzengsen
 * @date 2024/3/22 9:34 上午
 */
@Data
public class MyImageInfo {

    /**
     * 图片宽度
     */
    private int widthCm;

    /**
     * 图片高度
     */
    private int heightCm;

    /**
     * 图片宽度(像素)
     */
    private int widthPx;

    /**
     * 图片高度(像素)
     */
    private int heightPx;

    /**
     * 图片绝对路径
     */
    private String absolutePath;

    /**
     * 图片名
     */
    private String name;

    /**
     * 无后缀的图片名
     */
    private String nameNoSuffix;
}
