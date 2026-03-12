package com.video.crop;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * MP4视频中心9:16比例裁切工具
 * 功能: 从 VideoIn 文件夹读取MP4文件，选择序号进行裁切，输出到 VideoOut 文件夹
 */
public class VideoCropTool {

    private static final String INPUT_DIR = "VideoIn";
    private static final String OUTPUT_DIR = "VideoOut";

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   MP4视频中心9:16比例裁切工具");
        System.out.println("========================================");
        System.out.println();

        Scanner scanner = new Scanner(System.in);

        try {
            // 创建必要的文件夹
            ensureDirectories();

            // 列出 VideoIn 文件夹下的所有MP4文件
            List<File> mp4Files = listMP4Files(INPUT_DIR);

            if (mp4Files.isEmpty()) {
                System.out.println("错误: VideoIn 文件夹中没有找到 MP4 文件！");
                System.out.println("请将 MP4 文件放在: " + new File(INPUT_DIR).getAbsolutePath());
                return;
            }

            System.out.println("发现的 MP4 文件 (" + mp4Files.size() + " 个):");
            System.out.println();
            for (int i = 0; i < mp4Files.size(); i++) {
                System.out.println((i + 1) + ". " + mp4Files.get(i).getName());
            }
            System.out.println();

            // 获取用户输入
            int selectedIndex = -1;
            while (selectedIndex < 1 || selectedIndex > mp4Files.size()) {
                System.out.print("请输入要裁切的视频序号 (1-" + mp4Files.size() + "): ");
                try {
                    String input = scanner.nextLine().trim();
                    selectedIndex = Integer.parseInt(input);
                    if (selectedIndex < 1 || selectedIndex > mp4Files.size()) {
                        System.out.println("错误: 请输入 1 到 " + mp4Files.size() + " 之间的数字");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("错误: 请输入有效的数字");
                }
            }

            File selectedFile = mp4Files.get(selectedIndex - 1);
            String inputFilePath = selectedFile.getAbsolutePath();

            System.out.println();
            System.out.println("选择了: " + selectedFile.getName());
            System.out.println();
            
            // 获取裁切位置偏移量
            int offsetPercentage = 101;  // 初始值必须是无效值，以便while循环执行
            while (offsetPercentage < -100 || offsetPercentage > 100) {
                System.out.print("请输入裁切位置偏移量 (-100=最左, 0=正中心, 100=最右): ");
                try {
                    String input = scanner.nextLine().trim();
                    offsetPercentage = Integer.parseInt(input);
                    if (offsetPercentage < -100 || offsetPercentage > 100) {
                        System.out.println("错误: 请输入 -100 到 100 之间的数字");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("错误: 请输入有效的数字");
                }
            }
            
            System.out.println();
            System.out.println("正在处理视频...");
            System.out.println();

            // 创建视频处理器
            VideoProcessor processor = new VideoProcessor(OUTPUT_DIR);

            // 执行裁切操作
            java.nio.file.Path outputFilePath = processor.cropVideoTo9by16(inputFilePath, offsetPercentage);

            System.out.println();
            System.out.println("========================================");
            System.out.println("✓ 裁切成功!");
            System.out.println("输出文件: " + outputFilePath.getFileName());
            System.out.println("完整路径: " + outputFilePath.toAbsolutePath());
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
            if (!e.getMessage().contains("FFmpeg")) {
                e.printStackTrace();
            }
        } finally {
            scanner.close();
        }
    }

    /**
     * 确保输入和输出文件夹存在
     */
    private static void ensureDirectories() {
        File inputDir = new File(INPUT_DIR);
        File outputDir = new File(OUTPUT_DIR);

        if (!inputDir.exists()) {
            inputDir.mkdirs();
        }

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
    }

    /**
     * 列出指定目录下的所有MP4文件
     */
    private static List<File> listMP4Files(String directory) {
        List<File> mp4Files = new ArrayList<>();
        File dir = new File(directory);

        if (!dir.exists() || !dir.isDirectory()) {
            return mp4Files;
        }

        File[] files = dir.listFiles((d, name) ->
                name.toLowerCase().endsWith(".mp4") &&
                new File(d, name).isFile()
        );

        if (files != null) {
            for (File file : files) {
                mp4Files.add(file);
            }
        }

        return mp4Files;
    }
}
