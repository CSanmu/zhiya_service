import deliverygood.enums.ImageTypeEnum;
import deliverygood.model.BasicImageInfo;
import deliverygood.model.CommonConstant;
import deliverygood.model.MyImageInfo;
import deliverygood.utils.FileNameAnalysis;
import javafx.util.Pair;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.Imaging;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import util.DataUtils;
import util.PhotoUtils;
import util.ThreadPools;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 分两种集合
 * 小图 50-30及以下，统一按照26：21的比例去压缩 小尺寸排序方式：一行7个 一共4行
 * 大图 50-30以上，统一按照70：30的比例去压缩   大尺寸排序方式：一行6个，一共5行
 *
 * @author chenzengsen
 * @date 2024/3/21 2:01 下午
 */
public class Main {
    private static JFrame FRAME = null;
    /**
     * 大图docx文件名编号
     */
    private static final AtomicInteger BIG_IMAGE_NUMBER = new AtomicInteger(1);

    public static void main(String[] args) {
        // 生成主窗口
        FRAME = createMainWindow();
        // 创建文本区域，用于显示文件夹名称
        JTextArea folderText = createTextArea();
        // 生成选择文件夹按钮
        JButton chooseFolderButton = createChooseButton(folderText);
        // 生成确定按钮
        JButton confirmButton = createConfirmButton(folderText);
        FRAME.add(chooseFolderButton);
        FRAME.add(folderText);
        FRAME.add(confirmButton);
        FRAME.setVisible(true);
    }

    private static JButton createConfirmButton(JTextArea folderText) {
        JButton confirmButton = new JButton("开始生成发货图");
        confirmButton.setPreferredSize(new Dimension(200, 50));
        confirmButton.addActionListener(e -> {
            // 开始生成发货图
            selectAndAction(folderText.getText());
            confirmButton.setEnabled(false);
        });
        return confirmButton;
    }

    private static JButton createChooseButton(JTextArea folderText) {
        JButton chooseFolderButton = new JButton("选择文件夹");
        chooseFolderButton.setPreferredSize(new Dimension(200, 50));
        chooseFolderButton.addActionListener(e -> {
            // 创建JFileChooser实例
            JFileChooser fileChooser = new JFileChooser();
            // 设置为选择文件夹
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            // 显示文件夹选择对话框
            int returnValue = fileChooser.showOpenDialog(FRAME);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                // 获取选择的文件夹路径
                String folderPath = fileChooser.getSelectedFile().getAbsolutePath();
                folderText.setText(folderPath);
            }
        });
        return chooseFolderButton;
    }

    private static JTextArea createTextArea() {
        JTextArea folderText = new JTextArea(1, 30);
        folderText.setEditable(false);
        folderText.setLineWrap(true);
        folderText.setWrapStyleWord(true);
        return folderText;
    }

    private static JFrame createMainWindow() {
        // 创建主窗口
        JFrame frame = new JFrame("生成发布货软件");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400); // 设置窗口大小
        frame.setLocationRelativeTo(null); // 确保窗口在屏幕中间
        // 创建布局管理器
        frame.setLayout(new GridBagLayout());
        return frame;
    }

    /**
     * 开始生成发货图
     *
     * @param folderPath
     */
    private static void selectAndAction(String folderPath) {
        File folderFile = new File(folderPath);
        List<BasicImageInfo> singleBasicImageInfoList = PhotoUtils.getAbsolutePathListFromFolder(folderFile, false);
        List<BasicImageInfo> multipleSizeBasicImageInfoList = PhotoUtils.getAbsolutePathListFromFolder(folderFile, true);
        if (CollectionUtils.isEmpty(singleBasicImageInfoList) && CollectionUtils.isEmpty(multipleSizeBasicImageInfoList)) {
            System.out.println("没有找到图片");
            return;
        }
        List<MyImageInfo> bigImageList = new ArrayList<>();
        List<MyImageInfo> smallImageList = new ArrayList<>();
        List<MyImageInfo> multipleSizeImageList = new ArrayList<>();
        // 识别大图和小图
        allocation(singleBasicImageInfoList, bigImageList, smallImageList);
        // 识别多张图
        allocationMultipleSize(multipleSizeBasicImageInfoList, multipleSizeImageList);
        // 处理小图
        smallImageHandler(folderFile, smallImageList);
        // 处理大图
        bigImageHandler(folderFile, bigImageList);
        // 处理多张图
        multipleSizeImageHandler(folderFile, multipleSizeImageList);
        System.out.println("一共" + CommonConstant.DOCX_COUNT.get() + "个文档");
    }

    /**
     * 处理大图
     *
     * @param folderFile
     * @param bigImageList
     */
    private static void bigImageHandler(File folderFile, List<MyImageInfo> bigImageList) {
        if (CollectionUtils.isEmpty(bigImageList)) {
            return;
        }
        Map<Integer, List<MyImageInfo>> bigImageListGroup = IntStream.range(0, (bigImageList.size() + 899) / 900)
                .boxed()
                .collect(Collectors.toMap(
                        i -> i,
                        i -> bigImageList.subList(i * 900, Math.min((i + 1) * 900, bigImageList.size())))
                );
        bigImageListGroup.forEach((key, value) -> {
            ThreadPools.getInstance().submitBigTask(() -> {
                action(value, folderFile.getName(), ImageTypeEnum.BIG);
            });
            CommonConstant.DOCX_COUNT.incrementAndGet();
        });
    }

    /**
     * 处理小图
     *
     * @param folderFile
     * @param smallImageList
     */
    private static void smallImageHandler(File folderFile, List<MyImageInfo> smallImageList) {
        if (CollectionUtils.isEmpty(smallImageList)) {
            return;
        }
        // 小图生成
        ThreadPools.getInstance().submitSmallTask(() -> {
            action(smallImageList, folderFile.getName(), ImageTypeEnum.SMALL);
        });
        CommonConstant.DOCX_COUNT.incrementAndGet();
    }

    /**
     * 处理多图
     *
     * @param folderFile
     * @param multipleSizeImageList
     */
    private static void multipleSizeImageHandler(File folderFile, List<MyImageInfo> multipleSizeImageList) {
        if (CollectionUtils.isEmpty(multipleSizeImageList)) {
            return;
        }
        // 多种尺寸图生成
        ThreadPools.getInstance().submitSmallTask(() -> {
            action(multipleSizeImageList, folderFile.getName(), ImageTypeEnum.MULTIPLE);
        });
        CommonConstant.DOCX_COUNT.incrementAndGet();
    }


    private static void allocation(List<BasicImageInfo> basicImageInfoList, List<MyImageInfo> bigImageList, List<MyImageInfo> smallImageList) {
        if (CollectionUtils.isEmpty(basicImageInfoList)) {
            return;
        }
        basicImageInfoList.forEach(item -> {
            Integer area = FileNameAnalysis.calculateArea(item.getName());
            if (Objects.isNull(area)) {
                return;
            }
            Pair<Integer, Integer> widthAndHeight = FileNameAnalysis.getWidthAndHeight(item.getName());
            if (Objects.isNull(widthAndHeight)) {
                return;
            }
            MyImageInfo myImageInfo = buildMyImageInfo(item.getAbsolutePath(), item.getName(), widthAndHeight);
            if (Objects.isNull(myImageInfo)) {
                return;
            }
            if (area <= CommonConstant.BOUND) {
                smallImageList.add(myImageInfo);
            } else {
                bigImageList.add(myImageInfo);
            }
        });
    }

    private static void allocationMultipleSize(List<BasicImageInfo> basicImageInfoList, List<MyImageInfo> multipleSizeImageList) {
        if (CollectionUtils.isEmpty(basicImageInfoList)) {
            return;
        }
        basicImageInfoList.forEach(item -> {
            Integer area = FileNameAnalysis.calculateArea(item.getName());
            if (Objects.isNull(area)) {
                return;
            }
            Pair<Integer, Integer> widthAndHeight = FileNameAnalysis.getWidthAndHeight(item.getName());
            if (Objects.isNull(widthAndHeight)) {
                return;
            }
            MyImageInfo myImageInfo = buildMyImageInfo(item.getAbsolutePath(), item.getName(), widthAndHeight);
            if (Objects.isNull(myImageInfo)) {
                return;
            }
            multipleSizeImageList.add(myImageInfo);
        });
    }

    private static MyImageInfo buildMyImageInfo(String key, String value, Pair<Integer, Integer> widthAndHeight) {
        MyImageInfo myImageInfo = new MyImageInfo();
        myImageInfo.setWidthCm(widthAndHeight.getKey());
        myImageInfo.setHeightCm(widthAndHeight.getValue());
        try {
            ImageInfo imageInfo = Imaging.getImageInfo(new File(key));
            myImageInfo.setWidthPx(imageInfo.getWidth());
            myImageInfo.setHeightPx(imageInfo.getHeight());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        myImageInfo.setAbsolutePath(key);
        myImageInfo.setName(value);
        myImageInfo.setNameNoSuffix(value.substring(0, value.lastIndexOf('.')));
        return myImageInfo;
    }

    public static void action(List<MyImageInfo> imageList, String folderName, ImageTypeEnum imageTypeEnum) {
        XWPFDocument document = new XWPFDocument();
        CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
        setPageSize(sectPr);
        for (int page = 0; page < getPageCount(imageList, imageTypeEnum); page++) {
            folderInfo(document, folderName);
            pageHandler(imageList, imageTypeEnum, document, page);
        }
        write(document, imageTypeEnum);
        int currentCount = CommonConstant.CURRENT_DOCX_COUNT.incrementAndGet();
        if (currentCount == CommonConstant.DOCX_COUNT.get()) {
            JOptionPane.showMessageDialog(FRAME, "执行完成，共处理:" + CommonConstant.CURRENT_IMAGE_COUNT.get() + "张图片");
        }
    }

    /**
     * 第一行设置文件夹信息
     *
     * @param document
     */
    private static void folderInfo(XWPFDocument document, String name) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setFontSize(14);
        String date = DataUtils.formatDateTime("yyyy年MM月dd天");
        run.setText(date + " " + name);
        // 设置文字对齐方式为左对齐
        CTJc jc = paragraph.getCTP().addNewPPr().addNewJc();
        jc.setVal(STJc.LEFT);
    }

    /**
     * 处理每一页
     *
     * @param imageList
     * @param imageTypeEnum
     * @param document
     * @param page
     */
    private static void pageHandler(List<MyImageInfo> imageList, ImageTypeEnum imageTypeEnum, XWPFDocument document, int page) {
        XWPFTable table = createTable(document, imageTypeEnum);
        // 遍历表格的每个单元格，并在其中插入图片
        for (int row = 0; row < getRowCount(imageTypeEnum); row++) {
            rowHandler(imageList, imageTypeEnum, page, table, row);
        }
        if (page != (getPageCount(imageList, imageTypeEnum) - 1)) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.createRun().addBreak(BreakType.PAGE);
        }
    }

    /**
     * 每行处理
     *
     * @param imageList
     * @param imageTypeEnum
     * @param page
     * @param table
     * @param row
     */
    private static void rowHandler(List<MyImageInfo> imageList, ImageTypeEnum imageTypeEnum, int page, XWPFTable table, int row) {
        // 设置行高
        XWPFTableRow tableRow = setRowHeight(imageTypeEnum, table, row);
        for (int col = 0; col < getCelCount(imageTypeEnum); col++) {
            colHandler(imageList, imageTypeEnum, page, row, tableRow, col);
        }
    }

    /**
     * 每个单元格处理
     *
     * @param imageList
     * @param imageTypeEnum
     * @param page
     * @param row
     * @param tableRow
     * @param col
     */
    private static void colHandler(List<MyImageInfo> imageList, ImageTypeEnum imageTypeEnum, int page, int row, XWPFTableRow tableRow, int col) {
        // 获取单元格
        XWPFParagraph paragraph = setTableCellWidth(tableRow, col, imageTypeEnum);
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        paragraph.setVerticalAlignment(TextAlignment.CENTER);
        // 创建一个运行来插入图片
        XWPFRun run = paragraph.createRun();
        // 计算图片的编号
        int imageNumber = (page * getCelCount(imageTypeEnum) * getRowCount(imageTypeEnum)) + (row * getCelCount(imageTypeEnum)) + col;
        // 构建图片的文件路径
        if (imageNumber >= imageList.size()) {
            return;
        }
        MyImageInfo myImageInfo = imageList.get(imageNumber);
        Pair<Integer, Integer> pair = calImageRealSize(myImageInfo);
        File file = PhotoUtils.imageCompressor(myImageInfo.getAbsolutePath());
        try (FileInputStream is = new FileInputStream(file)) {
            run.setFontSize(10);
            run.addPicture(is, XWPFDocument.PICTURE_TYPE_JPEG, "image" + imageNumber, pair.getKey(), pair.getValue());
            run.addBreak();
            run.setText(myImageInfo.getNameNoSuffix());
            System.out.println("已处理:" + CommonConstant.CURRENT_IMAGE_COUNT.incrementAndGet() + "张");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取文档表格内的图片尺寸
     *
     * @param myImageInfo
     * @return
     */
    private static Pair<Integer, Integer> calImageRealSize(MyImageInfo myImageInfo) {
        Integer area = FileNameAnalysis.calculateArea(myImageInfo.getName());
        if (area <= CommonConstant.BOUND) {
            double ratios = 21.0 / 26.0;
            double widthRatio = (26.0 / myImageInfo.getWidthCm()) * (4.1 / 26);
            double newWidth = myImageInfo.getWidthCm() * widthRatio;
            double newHeight = newWidth * ratios;
            int realWidth = (int) (newWidth * 360000);
            int realHeight = (int) (newHeight * 360000);
            return new Pair<>(realWidth, realHeight);
        }
        double ratios = 30.0 / 70.0;
        double widthRatio = (70.0 / myImageInfo.getWidthCm()) * (5.2 / 76);
        double newWidth = myImageInfo.getWidthCm() * widthRatio;
        double newHeight = newWidth * ratios;
        int realWidth = (int) (newWidth * 360000);
        int realHeight = (int) (newHeight * 360000);
        return new Pair<>(realWidth, realHeight);
    }


    /**
     * 生成word文档
     *
     * @param document
     */
    private static void write(XWPFDocument document, ImageTypeEnum imageTypeEnum) {
        String path = null;
        if (Objects.equals(imageTypeEnum, ImageTypeEnum.SMALL)) {
            path = "小图.docx";
        }
        if (Objects.equals(imageTypeEnum, ImageTypeEnum.BIG)) {
            path = "大图" + BIG_IMAGE_NUMBER.getAndIncrement() + ".docx";
        }
        if (Objects.equals(imageTypeEnum, ImageTypeEnum.MULTIPLE)) {
            path = "多种尺寸.docx";
        }
        File file = new File(path);
        // 将文档保存到文件系统
        try (FileOutputStream out = new FileOutputStream(file)) {
            document.write(out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                document.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 设置单元格宽度
     *
     * @param tableRow
     * @param col
     * @return
     */
    private static XWPFParagraph setTableCellWidth(XWPFTableRow tableRow, int col, ImageTypeEnum imageTypeEnum) {
        XWPFTableCell cell = tableRow.getCell(col);
        CTTblWidth ctTblWidth = cell.getCTTc().addNewTcPr().addNewTcW();
        ctTblWidth.setType(STTblWidth.PCT);
        if (Objects.equals(imageTypeEnum, ImageTypeEnum.SMALL)) {
            // 5000/7
            ctTblWidth.setW(BigInteger.valueOf(714));
        } else {
            // 5000/6
            ctTblWidth.setW(BigInteger.valueOf(833));
        }
        XWPFParagraph paragraph = cell.addParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        return paragraph;
    }

    /**
     * 设置行高
     *
     * @param imageTypeEnum
     * @param table
     * @param row
     * @return
     */
    private static XWPFTableRow setRowHeight(ImageTypeEnum imageTypeEnum, XWPFTable table, int row) {
        XWPFTableRow tableRow = table.getRow(row);
//        CTTrPr trPr = tableRow.getCtRow().addNewTrPr();
//        CTHeight height = trPr.addNewTrHeight();
//        height.setVal(BigInteger.valueOf(16838 / getRowCount(small) / getRowCount(small)));
        return tableRow;
    }

    /**
     * 获取列数
     *
     * @param imageTypeEnum
     * @return
     */
    private static int getCelCount(ImageTypeEnum imageTypeEnum) {
        if (Objects.equals(imageTypeEnum, ImageTypeEnum.SMALL)) {
            return 7;
        }
        return 6;
    }

    /**
     * 获取行数
     *
     * @param imageTypeEnum
     * @return
     */
    private static int getRowCount(ImageTypeEnum imageTypeEnum) {
        if (Objects.equals(imageTypeEnum, ImageTypeEnum.SMALL)) {
            return 4;
        }
        return 5;
    }

    /**
     * 创建表格
     *
     * @param document
     * @param imageTypeEnum
     * @return
     */
    private static XWPFTable createTable(XWPFDocument document, ImageTypeEnum imageTypeEnum) {
        // 创建表格
        XWPFTable table = document.createTable(getRowCount(imageTypeEnum), getCelCount(imageTypeEnum));
        // 设置表格宽度以填满页面
        CTTblWidth tblWidth = table.getCTTbl().addNewTblPr().addNewTblW();
        tblWidth.setType(STTblWidth.PCT);
        tblWidth.setW(BigInteger.valueOf(5000));
        return table;
    }

    /**
     * 获取页数
     *
     * @param imageList
     * @param imageTypeEnum
     * @return
     */
    private static int getPageCount(List<MyImageInfo> imageList, ImageTypeEnum imageTypeEnum) {
        int onePageCount = getCelCount(imageTypeEnum) * getRowCount(imageTypeEnum);
        return (int) Math.ceil(imageList.size() / (double) onePageCount);
    }

    /**
     * 设置页面大小为A4纸
     *
     * @param sectPr
     */
    private static void setPageSize(CTSectPr sectPr) {
        CTPageSz pageSize = sectPr.addNewPgSz();
        pageSize.setW(BigInteger.valueOf(16840));
        pageSize.setH(BigInteger.valueOf(11907));
        pageSize.setOrient(STPageOrientation.LANDSCAPE);
        CTPageMar pageMar = sectPr.addNewPgMar();
        // A4纸的边距通常设置为：上边距2.54cm，下边距2.54cm，左边距3.17cm，右边距3.17cm
        // 将厘米转换为磅（1厘米 = 28.35磅）
        double topMarginCm = 1;
        double bottomMarginCm = 1;
        double leftMarginCm = 1;
        double rightMarginCm = 1;
        long topMargin = (long) (topMarginCm * 28.35);
        long bottomMargin = (long) (bottomMarginCm * 28.35);
        long leftMargin = (long) (leftMarginCm * 28.35);
        long rightMargin = (long) (rightMarginCm * 28.35);

        // 设置边距
        pageMar.setTop(topMargin);
        pageMar.setBottom(bottomMargin);
        pageMar.setLeft(leftMargin);
        pageMar.setRight(rightMargin);
    }
}
