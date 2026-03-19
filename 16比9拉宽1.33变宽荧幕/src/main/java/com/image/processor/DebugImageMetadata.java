package com.image.processor;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.io.File;

/**
 * 深度调试工具 - 显示图像中所有可用的元数据信息
 */
public class DebugImageMetadata {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("用法: java DebugImageMetadata <图像文件>");
            return;
        }
        
        File imageFile = new File(args[0]);
        if (!imageFile.exists()) {
            System.out.println("错误: 文件不存在 - " + imageFile);
            return;
        }
        
        System.out.println("=== 深度元数据调试 ===");
        System.out.println("文件: " + imageFile.getName());
        System.out.println();
        
        try {
            ImageInputStream iis = ImageIO.createImageInputStream(imageFile);
            if (iis == null) {
                System.out.println("错误: 无法创建ImageInputStream");
                return;
            }
            
            java.util.Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                System.out.println("错误: 无法找到ImageReader");
                return;
            }
            
            ImageReader reader = readers.next();
            reader.setInput(iis, true);
            
            System.out.println("ImageReader: " + reader.getClass().getName());
            System.out.println();
            
            // 读取第一张图像的元数据
            IIOMetadata metadata = reader.getImageMetadata(0);
            if (metadata == null) {
                System.out.println("警告: 无法读取元数据");
                reader.dispose();
                iis.close();
                return;
            }
            
            System.out.println("=== 可用的元数据格式 ===");
            String[] formatNames = metadata.getMetadataFormatNames();
            for (String format : formatNames) {
                System.out.println("- " + format);
            }
            System.out.println();
            
            // 尝试读取标准格式
            if (metadata.isStandardMetadataFormatSupported()) {
                System.out.println("=== 标准格式元数据树 ===");
                try {
                    org.w3c.dom.Node root = metadata.getAsTree("javax_imageio_1.0");
                    printNodeTree(root, 0);
                } catch (Exception e) {
                    System.out.println("错误读取标准格式: " + e.getMessage());
                }
            }
            
            // 尝试读取所有格式
            System.out.println();
            System.out.println("=== 所有格式的元数据树 ===");
            for (String format : formatNames) {
                if (!format.equals("javax_imageio_1.0")) {
                    System.out.println();
                    System.out.println("### 格式: " + format + " ###");
                    try {
                        org.w3c.dom.Node root = metadata.getAsTree(format);
                        printNodeTree(root, 0);
                    } catch (Exception e) {
                        System.out.println("错误: " + e.getMessage());
                    }
                }
            }
            
            reader.dispose();
            iis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void printNodeTree(org.w3c.dom.Node node, int depth) {
        String indent = "  ".repeat(depth);
        System.out.println(indent + "节点: " + node.getNodeName());
        
        // 打印属性
        org.w3c.dom.NamedNodeMap attrs = node.getAttributes();
        if (attrs != null) {
            for (int i = 0; i < attrs.getLength(); i++) {
                org.w3c.dom.Node attr = attrs.item(i);
                System.out.println(indent + "  @" + attr.getNodeName() + "=\"" + attr.getNodeValue() + "\"");
            }
        }
        
        // 打印子节点
        org.w3c.dom.NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                printNodeTree(child, depth + 1);
            }
        }
        
        // 限制递归深度以避免无限循环
        if (depth > 5) {
            System.out.println(indent + "  (递归深度限制，停止显示)");
        }
    }
}
