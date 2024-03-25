package deliverygood.model;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author chenzengsen
 * @date 2024/3/25 9:32 上午
 */
public interface CommonConstant {

    /**
     * 总应生成文档数量
     */
    AtomicInteger DOCX_COUNT = new AtomicInteger(0);

    /**
     * 当前已生成文档数量
     */
    AtomicInteger CURRENT_DOCX_COUNT = new AtomicInteger(0);

    /**
     * 当前已处理的图片数量
     */
    AtomicInteger CURRENT_IMAGE_COUNT = new AtomicInteger(0);

    /**
     * 大图与小图的尺寸分界线，小于等于这个值就是小图，大于这个值就是大图
     */
    Integer BOUND = 50 * 30;
}
