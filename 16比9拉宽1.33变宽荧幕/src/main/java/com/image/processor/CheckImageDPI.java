package com.image.processor;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Iterator;

/**
 * 简单的DPI检查工具
 */
public class CheckImageDPI {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("用法: java -cp target/image-aspect-ratio-processor-1.0.0.jar com.image.processor.CheckImageDPI <图像文件路径>");
            System.exit(1);
        }
        
        File imageFile = new File(args[0]);
        if (!imageFile.exists()) {
            System.out.println("错误: 找不到文件 " + imageFile);
            System.exit(1);
        }
        
        BufferedImage img = ImageIO.read(imageFile);
        System.out.println("文件: " + imageFile.getName());
        System.out.println("  - 分辨率: " + img.getWidth() + " x " + img.getHeight() + " 像素");
        
        // 尝试读取DPI信息
        double[] dpi = extractDPI(imageFile);
        System.out.println(String.format("  - DPI: %.0f x %.0f", dpi[0], dpi[1]));
    }
    
    private static double[] extractDPI(File imageFile) {
        double dpiX = 96.0;
        double dpiY = 96.0;
        
        try {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("JPEG");
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(ImageIO.createImageInputStream(imageFile));
                
                if (reader.getNumImages(true) > 0) {
                    var metadata = reader.getImageMetadata(0);
                    if (metadata != null && metadata.isStandardMetadataFormatSupported()) {
                        var root = metadata.getAsTree("javax_imageio_1.0");
                        var elements = root.getChildNodes();
                        
                        for (int i = 0; i < elements.getLength(); i++) {
                            var node = elements.item(i);
                            if (node.getNodeName().equals("HorizontalPixelSize")) {
                                double value = Double.parseDouble(
                                        node.getAttributes().getNamedItem("value").getNodeValue());
                                dpiX = 25.4 / value;
                            }
                            if (node.getNodeName().equals("VerticalPixelSize")) {
                                double value = Double.parseDouble(
                                        node.getAttributes().getNamedItem("value").getNodeValue());
                                dpiY = 25.4 / value;
                            }
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
}
