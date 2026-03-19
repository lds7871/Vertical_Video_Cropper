package com.image.processor;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 图像宽高比处理器
 * 将16:9的图像拉伸1.33倍以成为宽荧幕比例
 * 支持格式：JPG, JPEG, PNG, BMP, GIF
 */
public class ImageAspectRatioProcessor {

    private static final Set<String> SUPPORTED_FORMATS = new HashSet<>(
            Arrays.asList("jpg", "jpeg", "png", "bmp", "gif")
    );
    
    private static final double TARGET_ASPECT_RATIO = 2.37; // 16:9 * 1.33
    private static final double SOURCE_ASPECT_RATIO = 16.0 / 9; // 1.777...
    private static final double ASPECT_RATIO_TOLERANCE = 0.05; // 5% 容差
    private static final double STRETCH_FACTOR = 1.33;

    public static void main(String[] args) throws IOException {
        String inputDir = "FImgIn_133";
        String outputDir = "FImgOut_133";

        processImages(inputDir, outputDir);
    }

    public static void processImages(String inputDirPath, String outputDirPath) throws IOException {
        Path inputPath = Paths.get(inputDirPath);
        Path outputPath = Paths.get(outputDirPath);

        // 创建输出目录
        Files.createDirectories(outputPath);

        // 遍历输入目录中的所有文件
        Files.list(inputPath)
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    String filename = file.getFileName().toString();
                    String extension = getFileExtension(filename).toLowerCase();

                    if (SUPPORTED_FORMATS.contains(extension)) {
                        try {
                            processImage(file, outputPath, filename);
                        } catch (IOException e) {
                            System.err.println("处理文件失败: " + filename + " - " + e.getMessage());
                        }
                    }
                });

        System.out.println("处理完成!");
    }

    private static void processImage(Path inputFile, Path outputDir, String filename) throws IOException {
        try {
            BufferedImage image = ImageIO.read(inputFile.toFile());
            
            if (image == null) {
                System.out.println("跳过: " + filename + " (无法读取)");
                return;
            }

            int width = image.getWidth();
            int height = image.getHeight();
            double aspectRatio = (double) width / height;

            // 检查是否为16:9宽高比
            if (isAspectRatioMatch(aspectRatio, SOURCE_ASPECT_RATIO)) {
                System.out.println("处理: " + filename + " (" + width + "x" + height + ")");
                
                // 计算新的宽度（拉伸1.33倍）
                int newWidth = (int) (width * STRETCH_FACTOR);
                int newHeight = height;

                // 创建新的图像
                BufferedImage stretchedImage = new BufferedImage(
                        newWidth, newHeight, BufferedImage.TYPE_INT_RGB
                );

                // 使用Graphics2D绘制拉伸后的图像
                var g2d = stretchedImage.createGraphics();
                g2d.drawImage(image, 0, 0, newWidth, newHeight, null);
                g2d.dispose();

                // 读取源图像的DPI信息
                double[] sourceDPI = extractDPIFromImage(inputFile);

                // 保存到输出目录，并保留DPI信息
                String outputFilename = removeExtension(filename) + ".jpg";
                File outputFile = outputDir.resolve(outputFilename).toFile();
                saveImageWithDPI(stretchedImage, outputFile, sourceDPI[0], sourceDPI[1]);
                
                // 自动修复输出文件的JFIF DPI标记（确保DPI被正确保存）
                try {
                    fixJFIFDPI(outputFile, (int) sourceDPI[0], (int) sourceDPI[1]);
                } catch (Exception e) {
                    System.err.println("警告: 无法修复DPI标记: " + e.getMessage());
                }
                
                System.out.println("  → 输出: " + outputFilename + " (" + newWidth + "x" + newHeight + ")" + 
                                   String.format(" [DPI: %.0f×%.0f]", sourceDPI[0], sourceDPI[1]));
            } else {
                System.out.println("跳过: " + filename + " (宽高比不是16:9, 实际: " + String.format("%.2f", aspectRatio) + ":1)");
            }
        } catch (IOException e) {
            throw new IOException("处理文件 " + filename + " 时出错: " + e.getMessage(), e);
        }
    }
    
    /**
     * 修复JPEG文件的JFIF DPI标记
     */
    private static void fixJFIFDPI(File jpegFile, int dpiX, int dpiY) throws IOException {
        byte[] fileContent = Files.readAllBytes(jpegFile.toPath());
        
        // 查找APP0标记
        int app0Pos = -1;
        for (int i = 0; i < fileContent.length - 1; i++) {
            if ((fileContent[i] & 0xFF) == 0xFF && (fileContent[i + 1] & 0xFF) == 0xE0) {
                app0Pos = i;
                break;
            }
        }
        
        if (app0Pos == -1) return;  // 无法找到APP0标记
        
        // 检查JFIF标记
        if (app0Pos + 8 < fileContent.length && 
            fileContent[app0Pos + 4] == 'J' && fileContent[app0Pos + 5] == 'F' &&
            fileContent[app0Pos + 6] == 'I' && fileContent[app0Pos + 7] == 'F' &&
            fileContent[app0Pos + 8] == 0) {
            
            // 设置单位为DPI
            fileContent[app0Pos + 11] = 1;
            
            // 设置X DPI
            fileContent[app0Pos + 12] = (byte) ((dpiX >> 8) & 0xFF);
            fileContent[app0Pos + 13] = (byte) (dpiX & 0xFF);
            
            // 设置Y DPI
            fileContent[app0Pos + 14] = (byte) ((dpiY >> 8) & 0xFF);
            fileContent[app0Pos + 15] = (byte) (dpiY & 0xFF);
            
            // 写回文件
            Files.write(jpegFile.toPath(), fileContent);
        }
    }

    /**
     * 从源图像文件中提取DPI信息（支持多种元数据格式）
     */
    private static double[] extractDPIFromImage(Path imageFile) {
        double dpiX = 300.0; // 默认DPI为300
        double dpiY = 300.0;
        
        try {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("JPEG");
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(ImageIO.createImageInputStream(imageFile.toFile()));
                
                if (reader.getNumImages(true) > 0) {
                    IIOMetadata metadata = reader.getImageMetadata(0);
                    if (metadata != null) {
                        // 尝试标准元数据格式
                        try {
                            if (metadata.isStandardMetadataFormatSupported()) {
                                org.w3c.dom.Node root = metadata.getAsTree("javax_imageio_1.0");
                                org.w3c.dom.NodeList elements = root.getChildNodes();
                                
                                for (int i = 0; i < elements.getLength(); i++) {
                                    org.w3c.dom.Node node = elements.item(i);
                                    if (node.getNodeName().equals("HorizontalPixelSize")) {
                                        double value = Double.parseDouble(node.getAttributes()
                                                .getNamedItem("value").getNodeValue());
                                        dpiX = 25.4 / value;
                                    }
                                    if (node.getNodeName().equals("VerticalPixelSize")) {
                                        double value = Double.parseDouble(node.getAttributes()
                                                .getNamedItem("value").getNodeValue());
                                        dpiY = 25.4 / value;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // 尝试其他格式
                        }
                    }
                }
                reader.dispose();
            }
        } catch (Exception e) {
            // 使用默认DPI
        }
        
        return new double[]{dpiX, dpiY};
    }

    /**
     * 以高质量和指定DPI保存图像
     * 注意：Java ImageIO对JFIF DPI标记的支持有限，我们尽可能设置元数据
     */
    private static void saveImageWithDPI(BufferedImage image, File outputFile, double dpiX, double dpiY) 
            throws IOException {
        // 确保使用高DPI（至少300）
        if (dpiX < 150) dpiX = 300.0;
        if (dpiY < 150) dpiY = 300.0;
        
        try {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) {
                // 如果没有ImageWriter，直接使用标准方法
                ImageIO.write(image, "jpg", outputFile);
                return;
            }
            
            ImageWriter writer = writers.next();
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            
            // 设置JPEG压缩品质为95%（非常高）
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionType("JPEG");
                writeParam.setCompressionQuality(0.95f);
            }
            
            // 获取默认元数据
            IIOMetadata metadata = writer.getDefaultImageMetadata(
                    new javax.imageio.ImageTypeSpecifier(image), writeParam);
            
            // 尝试设置分辨率元数据
            if (metadata != null) {
                // 尝试在标准格式中设置分辨率信息
                if (metadata.isStandardMetadataFormatSupported()) {
                    try {
                        // 方法A: 通过标准XML树设置
                        org.w3c.dom.Element root = (org.w3c.dom.Element)
                                metadata.getAsTree("javax_imageio_1.0");
                        
                        // 移除旧的分辨率信息（如果存在）
                        org.w3c.dom.NodeList existingRes = root.getElementsByTagName("ResolutionUnit");
                        for (int i = existingRes.getLength() - 1; i >= 0; i--) {
                            existingRes.item(i).getParentNode().removeChild(existingRes.item(i));
                        }
                        
                        // 添加分辨率信息
                        // JFIF要求分辨率以1/72英寸为单位，所以300 DPI = 300/72 = 4.166...单位
                        // 但我们在这里使用PixelSize（毫米），将DPI转换为mm: 25.4/300 ≈ 0.0847
                        org.w3c.dom.Document doc = root.getOwnerDocument();
                        
                        // 水平分辨率
                        org.w3c.dom.Element hRes = doc.createElement("HorizontalPixelSize");
                        hRes.setAttribute("value", String.format("%.6f", 25.4 / dpiX));
                        root.appendChild(hRes);
                        
                        // 垂直分辨率
                        org.w3c.dom.Element vRes = doc.createElement("VerticalPixelSize");
                        vRes.setAttribute("value", String.format("%.6f", 25.4 / dpiY));
                        root.appendChild(vRes);
                        
                        // 应用元数据
                        metadata.mergeTree("javax_imageio_1.0", root);
                    } catch (Exception metadataError) {
                        // System.err.println("警告: 无法设置图像元数据: " + metadataError.getMessage());
                    }
                }
            }
            
            // 写入图像
            try (FileImageOutputStream fios = new FileImageOutputStream(outputFile)) {
                writer.setOutput(fios);
                writer.write(metadata, new javax.imageio.IIOImage(image, null, metadata), writeParam);
            } finally {
                writer.dispose();
            }
        } catch (Exception e) {
            // 如果高级方法完全失败，回退到简单方法
            System.err.println("警告: ImageWriter失败，使用标准ImageIO: " + e.getMessage());
            ImageIO.write(image, "jpg", outputFile);
        }
    }

    private static boolean isAspectRatioMatch(double actual, double expected) {
        double ratio = actual / expected;
        return ratio >= (1 - ASPECT_RATIO_TOLERANCE) && ratio <= (1 + ASPECT_RATIO_TOLERANCE);
    }

    private static String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }

    private static String removeExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(0, lastDot) : filename;
    }
}
