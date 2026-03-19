package com.image.processor;

import java.io.*;

/**
 * JPEG JFIF DPI修复工具
 * 直接在二进制级别修改JPEG文件中的JFIF APP0标记的DPI值
 */
public class JFIFDPIFixer {
    
    // JFIF APP0标记
    private static final int APP0_MARKER = 0xFFE0;
    private static final byte[] JFIF_IDENTIFIER = {'J', 'F', 'I', 'F', 0};
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("用法: java JFIFDPIFixer <JPEG文件> <DPI>");
            System.out.println("示例: java JFIFDPIFixer output.jpg 300");
            return;
        }
        
        File jpegFile = new File(args[0]);
        int dpi;
        try {
            dpi = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("错误: DPI必须是整数");
            return;
        }
        
        if (!jpegFile.exists()) {
            System.out.println("错误: 文件不存在 - " + jpegFile);
            return;
        }
        
        try {
            modifyJFIFDPI(jpegFile, dpi);
            System.out.println("✓ 成功修改DPI为 " + dpi + " DPI");
        } catch (Exception e) {
            System.out.println("✗ 修改失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 修改JPEG文件中的JFIF DPI值
     */
    private static void modifyJFIFDPI(File jpegFile, int densityDots) throws IOException {
        byte[] fileContent = readFile(jpegFile);
        
        // 查找APP0标记
        int app0Pos = findAPP0Marker(fileContent);
        if (app0Pos == -1) {
            System.out.println("警告: 找不到JFIF APP0标记，尝试创建...");
            // 可以尝试在SOI后添加APP0标记，但很复杂
            // 对于简化，我们只修改现有标记
            return;
        }
        
        System.out.println("  - APP0标记位置: " + app0Pos);
        
        // APP0标记结构：
        // 0-1: APP0 marker (FFE0)
        // 2-3: Length (big-endian)
        // 4-8: "JFIF\0"
        // 9: Version major
        // 10: Version minor
        // 11: Units (0=no unit, 1=DPI, 2=DPI x DPI)
        // 12-13: X density (big-endian) - DPI水平
        // 14-15: Y density (big-endian) - DPI垂直
        // ...
        
        // 检查是否为JFIF标记
        int jfifPos = app0Pos + 4;
        if (!isJFIFMarker(fileContent, jfifPos)) {
            System.out.println("警告: 找到的不是标准JFIF标记");
            // 显示实际内容
            System.out.print("  - 标记内容: ");
            for (int i = jfifPos; i < Math.min(jfifPos + 5, fileContent.length); i++) {
                System.out.print(String.format("%02X ", fileContent[i] & 0xFF));
            }
            System.out.println();
            return;
        }
        
        System.out.println("  - JFIF标记验证成功");
        
        // 显示当前DPI值
        int xDensityPos = app0Pos + 12;
        int yDensityPos = app0Pos + 14;
        int currentXDPI = ((fileContent[xDensityPos] & 0xFF) << 8) | (fileContent[xDensityPos + 1] & 0xFF);
        int currentYDPI = ((fileContent[yDensityPos] & 0xFF) << 8) | (fileContent[yDensityPos + 1] & 0xFF);
        System.out.println("  - 当前X density: " + currentXDPI);
        System.out.println("  - 当前Y density: " + currentYDPI);
        
        // 设置单位为DPI（1）
        int unitsPos = app0Pos + 11;
        fileContent[unitsPos] = 1;  // 1 = DPI
        
        // 修改X密度（DPI）
        fileContent[xDensityPos] = (byte) ((densityDots >> 8) & 0xFF);
        fileContent[xDensityPos + 1] = (byte) (densityDots & 0xFF);
        
        // 修改Y密度（DPI）
        fileContent[yDensityPos] = (byte) ((densityDots >> 8) & 0xFF);
        fileContent[yDensityPos + 1] = (byte) (densityDots & 0xFF);
        
        // 写回文件
        writeFile(jpegFile, fileContent);
        
        System.out.println("  - 新的X density: " + densityDots);
        System.out.println("  - 新的Y density: " + densityDots);
    }
    
    /**
     * 查找APP0标记位置
     */
    private static int findAPP0Marker(byte[] data) {
        for (int i = 0; i < data.length - 1; i++) {
            if ((data[i] & 0xFF) == 0xFF && (data[i + 1] & 0xFF) == 0xE0) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * 检查是否为JFIF标记
     */
    private static boolean isJFIFMarker(byte[] data, int pos) {
        if (pos + 5 > data.length) return false;
        for (int i = 0; i < 5; i++) {
            if (data[pos + i] != JFIF_IDENTIFIER[i]) return false;
        }
        return true;
    }
    
    /**
     * 读取文件到字节数组
     */
    private static byte[] readFile(File file) throws IOException {
        byte[] buffer = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(buffer);
        }
        return buffer;
    }
    
    /**
     * 将字节数组写回文件
     */
    private static void writeFile(File file, byte[] data) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
    }
}
