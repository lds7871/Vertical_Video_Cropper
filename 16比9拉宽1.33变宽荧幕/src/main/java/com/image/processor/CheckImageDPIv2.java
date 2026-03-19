package com.image.processor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;

/**
 * 改进的DPI检查工具 - 直接从JFIF标记读取
 */
public class CheckImageDPIv2 {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("用法: java CheckImageDPIv2 <图像文件>");
            return;
        }
        
        for (String arg : args) {
            checkImage(new File(arg));
        }
    }
    
    private static void checkImage(File file) throws Exception {
        if (!file.exists()) {
            System.out.println("文件不存在: " + file.getName());
            return;
        }
        
        BufferedImage img = ImageIO.read(file);
        System.out.println("文件: " + file.getName());
        System.out.println("  - 像素尺寸: " + img.getWidth() + " x " + img.getHeight());
        
        // 尝试从JFIF标记读取DPI
        int[] jfifDPI = readJFIFDPI(file);
        System.out.printf("  - JFIF DPI: %d x %d%n", jfifDPI[0], jfifDPI[1]);
    }
    
    /**
     * 从JPEG文件的JFIF标记直接读取DPI
     */
    private static int[] readJFIFDPI(File jpegFile) {
        int[] dpi = {96, 96};  // 默认值
        
        try (FileInputStream fis = new FileInputStream(jpegFile)) {
            byte[] buffer = new byte[65536];
            int bytesRead = fis.read(buffer);
            
            // 查找APP0标记 (FFE0)
            for (int i = 0; i < bytesRead - 1; i++) {
                if ((buffer[i] & 0xFF) == 0xFF && (buffer[i + 1] & 0xFF) == 0xE0) {
                    // 找到APP0标记，检查是否为JFIF
                    if (i + 9 < bytesRead) {
                        // 检查"JFIF"标识符
                        if (buffer[i + 4] == 'J' && buffer[i + 5] == 'F' && 
                            buffer[i + 6] == 'I' && buffer[i + 7] == 'F' && 
                            buffer[i + 8] == 0) {
                            
                            // JFIF结构：
                            // i+0-1: FFE0 marker
                            // i+2-3: Length
                            // i+4-8: "JFIF\0"
                            // i+9: Version
                            // i+10: Version
                            // i+11: Units (0=none, 1=DPI, 2=DPI x DPI)
                            // i+12-13: X density (big-endian)
                            // i+14-15: Y density (big-endian)
                            
                            int units = buffer[i + 11] & 0xFF;
                            int xDensity = ((buffer[i + 12] & 0xFF) << 8) | (buffer[i + 13] & 0xFF);
                            int yDensity = ((buffer[i + 14] & 0xFF) << 8) | (buffer[i + 15] & 0xFF);
                            
                            // 如果单位不是DPI，就不返回这些值
                            if (units == 1) {
                                dpi[0] = xDensity;
                                dpi[1] = yDensity;
                            } else if (units == 0) {
                                // 没有单位时使用默认值
                                if (xDensity > 0) dpi[0] = xDensity;
                                if (yDensity > 0) dpi[1] = yDensity;
                            }
                            
                            return dpi;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 使用默认值
        }
        
        return dpi;
    }
}
