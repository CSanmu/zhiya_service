package deliverygood.model;

import lombok.Builder;
import lombok.Data;

/**
 * @author chenzengsen
 * @date 2024/3/22 6:54 下午
 */
@Data
@Builder
public class BasicImageInfo {

    /**
     * 图片绝对路径
     */
    private String absolutePath;

    /**
     * 图片名
     */
    private String name;
}
