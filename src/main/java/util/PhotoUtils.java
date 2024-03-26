package util;

import deliverygood.model.BasicImageInfo;
import deliverygood.model.CommonConstant;
import javafx.util.Pair;
import org.apache.commons.math3.linear.IllConditionedOperatorException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * @author chenzengsen
 * @date 2024/3/21 2:31 下午
 */
public class PhotoUtils {

    private static final Double ONE_INCH = 2.54;

    /**
     * 通过像素和dpi算出以cm为单位的长度
     *
     * @param pixel 像素
     * @param dpi   dpi（分辨率）
     * @return cm为单位的长度
     */
    public static int getCmWh(int pixel, int dpi) {
        double v = pixel / dpi * ONE_INCH;
        return (int) v;
    }

    public static boolean isImage(String fileName) {
        // 这里只是一个简单的示例，你可以根据需要修改
        return fileName.endsWith(".jpg") || fileName.endsWith(".png");
    }

    /**
     * 从文件夹内获取图片绝对目录(区分大小图)
     *
     * @param directory    文件夹目录
     * @param multipleSize 是否获取多种尺寸文件夹 true：是 false：否
     * @return 所有图片的绝对目录
     */
    public static List<BasicImageInfo> getAbsolutePathListFromFolder(File directory, boolean multipleSize) {
        List<BasicImageInfo> result = new ArrayList<>();
        if (!directory.exists() || !directory.isDirectory()) {
            return result;
        }
        findImages(directory, result, multipleSize);
        return result;
    }

    private static void findImages(File directory, List<BasicImageInfo> result, boolean multipleSize) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        List<File> fileList = Arrays.asList(files);
        Arrays.sort(files, Comparator.comparing(File::getName));
        // 如果需要，可以再次将List转换回数组
        files = fileList.toArray(new File[fileList.size()]);
        for (File file : files) {
            if (file.isDirectory()) {
                // 不是多张图片但是文件夹是多种尺寸，则过滤
                if (!multipleSize && Objects.equals(file.getName(), CommonConstant.SINGLE_HANDLER_FOLDER_NAME)) {
                    continue;
                }
                // 是多张图片但是文件夹不是多种尺寸，则过滤
                if (multipleSize && !Objects.equals(file.getName(), CommonConstant.SINGLE_HANDLER_FOLDER_NAME)) {
                    continue;
                }
                // 如果是子目录，则递归调用
                findImages(file, result, multipleSize);
                continue;
            }
            // 判断文件是否为图片
            if (PhotoUtils.isImage(file.getName())) {
                BasicImageInfo build = BasicImageInfo.builder()
                        .absolutePath(file.getAbsolutePath())
                        .name(file.getName()).build();
                result.add(build);
            }
        }
    }

    public static File imageCompressor(String absolutePath) {
        try {
            // 读取原始图片
            BufferedImage originalImage = ImageIO.read(new File(absolutePath));

            // 获取JPEG图片写入器
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();

            // 使用ByteArrayOutputStream来保存压缩后的图片数据
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // 创建图片输出流
            ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
            writer.setOutput(ios);

            // 设置压缩参数
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.1f); // 设置压缩质量，0.1表示10%的质量，这个值可以根据需要调整

            // 写入压缩后的图片
            writer.write(null, new IIOImage(originalImage, null, null), param);

            // 关闭流
            ios.close();
            writer.dispose();

            // 将压缩后的图片数据转换为File对象
            byte[] imageBytes = baos.toByteArray();
            File compressedImageFile = new File("compressedImage.jpg");
            try (FileOutputStream fos = new FileOutputStream(compressedImageFile)) {
                fos.write(imageBytes);
            }
            return compressedImageFile;
        } catch (IOException e) {
            return new File(absolutePath);
        }
    }
}
