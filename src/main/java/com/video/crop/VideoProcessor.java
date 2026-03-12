package com.video.crop;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 视频处理器
 * 负责MP4视频的信息获取和9:16比例的中心裁切
 */
public class VideoProcessor {

    private String outputDir;

    public VideoProcessor(String outputDir) {
        this.outputDir = outputDir;
    }

    /**
     * 将MP4视频裁切为中心9:16的比例
     *
     * @param inputPath 输入视频文件路径
     * @return 输出视频文件路径
     * @throws Exception 处理异常
     */
    public Path cropVideoTo9by16(String inputPath) throws Exception {
        return cropVideoTo9by16(inputPath, 0);  // 默认正中心
    }

    /**
     * 将MP4视频裁切为中心9:16的比例（带偏移量）
     *
     * @param inputPath 输入视频文件路径
     * @param offsetPercentage 裁切位置偏移量 (-100=最左, 0=正中心, 100=最右)
     * @return 输出视频文件路径
     * @throws Exception 处理异常
     */
    public Path cropVideoTo9by16(String inputPath, int offsetPercentage) throws Exception {
        File inputFile = new File(inputPath);

        // 获取视频信息
        System.out.println("正在读取视频信息...");
        VideoInfo info = getVideoInfo(inputPath);

        System.out.println("原始分辨率: " + info.width + "x" + info.height);
        // System.out.println("视频时长: " + formatDuration(info.duration));
        System.out.println("");

        // 计算裁切参数（9:16比例，中心裁切）
        CropParams cropParams = calculateCropParams(info.width, info.height, offsetPercentage);

        System.out.println("目标比例: 9:16");
        System.out.println("裁切后分辨率: " + cropParams.cropWidth + "x" + cropParams.cropHeight);
        System.out.println("裁切位置: x=" + cropParams.cropX + ", y=" + cropParams.cropY);
        if (offsetPercentage != 0) {
            System.out.println("位置偏移: " + (offsetPercentage > 0 ? "向右" : "向左") + " " + Math.abs(offsetPercentage) + "%");
        }
        System.out.println("");

        // 生成输出文件路径
        Path outputPath = generateOutputPath(inputPath);

        // 使用FFmpeg命令进行裁切
        System.out.println("开始裁切视频...");
        cropVideoUsingFFmpeg(inputPath, outputPath.toString(), cropParams);

        return outputPath;
    }

    /**
     * 使用FFprobe获取视频信息
     */
    private VideoInfo getVideoInfo(String inputPath) throws IOException, InterruptedException {
        // 使用 ffprobe 命令获取视频信息
        String[] command = {
                "ffprobe",
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=width,height,duration",
                "-of", "csv=p=0",
                inputPath
        };

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String infoline = reader.readLine();
            reader.close();

            int exitCode = process.waitFor();
            if (exitCode != 0 || infoline == null || infoline.isEmpty()) {
                // 如果ffprobe失败，尝试用ffmpeg
                return getVideoInfoUsingFFmpeg(inputPath);
            }

            // 解析输出: width,height,duration
            String[] parts = infoline.split(",");
            VideoInfo info = new VideoInfo();
            try {
                info.width = Integer.parseInt(parts[0].trim());
                info.height = Integer.parseInt(parts[1].trim());
                // duration可能为小数
                String durationStr = parts[2].trim();
                info.duration = (int) Double.parseDouble(durationStr);
            } catch (Exception e) {
                return getVideoInfoUsingFFmpeg(inputPath);
            }

            return info;
        } catch (IOException e) {
            // ffprobe 找不到，尝试 ffmpeg
            if (e.getMessage() != null && e.getMessage().contains("找不到指定的文件")) {
                System.out.println("[提示] ffprobe 未安装或未在系统 PATH 中");
                return getVideoInfoUsingFFmpeg(inputPath);
            }
            throw e;
        }
    }

    /**
     * 使用FFmpeg获取视频信息（备选方案）
     */
    private VideoInfo getVideoInfoUsingFFmpeg(String inputPath) throws IOException, InterruptedException {
        String[] command = {
                "ffmpeg",
                "-i", inputPath
        };

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            VideoInfo info = new VideoInfo();
            
            // 正则表达式用于提取分辨率和时长
            Pattern resolutionPattern = Pattern.compile("(\\d+)x(\\d+)");
            Pattern durationPattern = Pattern.compile("Duration: (\\d+):(\\d+):(\\d+)");
            
            String line;
            while ((line = reader.readLine()) != null) {
                // 提取分辨率
                Matcher resMatcher = resolutionPattern.matcher(line);
                if (resMatcher.find()) {
                    info.width = Integer.parseInt(resMatcher.group(1));
                    info.height = Integer.parseInt(resMatcher.group(2));
                }
                
                // 提取时长
                Matcher durationMatcher = durationPattern.matcher(line);
                if (durationMatcher.find()) {
                    int hours = Integer.parseInt(durationMatcher.group(1));
                    int minutes = Integer.parseInt(durationMatcher.group(2));
                    int seconds = Integer.parseInt(durationMatcher.group(3));
                    info.duration = hours * 3600 + minutes * 60 + seconds;
                }
            }
            
            reader.close();
            process.waitFor();
            
            if (info.width == 0 || info.height == 0) {
                throw new RuntimeException("无法获取视频信息，请检查文件是否为有效的视频文件");
            }
            
            return info;
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("找不到指定的文件")) {
                System.err.println("");
                System.err.println("========================================");
                System.err.println("错误: FFmpeg 未安装或未在系统 PATH 中");
                System.err.println("========================================");
                System.err.println("");
                System.err.println("请按以下步骤安装 FFmpeg:");
                System.err.println("");
                System.err.println("方式1: 使用 Chocolatey (推荐)");
                System.err.println("  命令: choco install ffmpeg");
                System.err.println("");
                System.err.println("方式2: 使用 Winget (Windows 11)");
                System.err.println("  命令: winget install FFmpeg");
                System.err.println("");
                System.err.println("方式3: 手动安装");
                System.err.println("  1. 访问 https://ffmpeg.org/download.html");
                System.err.println("  2. 下载 Windows build");
                System.err.println("  3. 解压到 C:\\ffmpeg");
                System.err.println("  4. 添加 C:\\ffmpeg\\bin 到系统 PATH");
                System.err.println("  5. 重启命令行窗口");
                System.err.println("");
                System.err.println("详细步骤请参考: install_ffmpeg.bat");
                System.err.println("========================================");
                System.err.println("");
                throw new RuntimeException("FFmpeg 未安装。请先安装 FFmpeg，然后重试。", e);
            }
            throw e;
        }
    }

    /**
     * 使用FFmpeg命令进行视频裁切
     */
    private void cropVideoUsingFFmpeg(String inputPath, String outputPath, CropParams cropParams) throws IOException {
        // FFmpeg crop filter 格式: crop=width:height:x:y
        String cropFilter = String.format("crop=%d:%d:%d:%d",
                cropParams.cropWidth,
                cropParams.cropHeight,
                cropParams.cropX,
                cropParams.cropY
        );

        // 构建FFmpeg命令
        String[] command = {
                "ffmpeg",
                "-i", inputPath,
                "-vf", cropFilter,
                "-c:a", "aac",
                "-b:a", "128k",
                "-c:v", "libx264",
                "-preset", "medium",
                "-crf", "23",
                "-y",  // 覆盖输出文件（如果存在）
                outputPath
        };

        System.out.print("执行命令: ");
        for (String s : command) {
            if (s.contains(" ")) {
                System.out.print("\"" + s + "\" ");
            } else {
                System.out.print(s + " ");
            }
        }
        System.out.println();
        System.out.println("");

        // 执行FFmpeg命令
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            // 读取输出信息并显示进度条
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            Pattern framePattern = Pattern.compile("frame=\\s*(\\d+)");
            int totalFrames = 0;  // 需要从 Duration 中计算
            int currentFrame = 0;
            
            while ((line = reader.readLine()) != null) {
                // 提取帧数信息
                if (line.contains("frame=")) {
                    Matcher matcher = framePattern.matcher(line);
                    if (matcher.find()) {
                        currentFrame = Integer.parseInt(matcher.group(1));
                        // 显示进度条
                        displayProgressBar(currentFrame);
                    }
                }
            }

            int exitCode = process.waitFor();
            reader.close();

            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg 执行失败，退出码: " + exitCode);
            }

            System.out.println("");
            System.out.println("✓ 视频裁切完成!");

        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("找不到指定的文件")) {
                System.err.println("");
                System.err.println("========================================");
                System.err.println("错误: FFmpeg 未安装或未在系统 PATH 中");
                System.err.println("========================================");
                System.err.println("");
                System.err.println("请按以下步骤安装 FFmpeg:");
                System.err.println("");
                System.err.println("方式1: 使用 Winget (Windows 11，最快)");
                System.err.println("  命令: winget install FFmpeg");
                System.err.println("");
                System.err.println("方式2: 使用 Chocolatey (推荐)");
                System.err.println("  前置: 需要安装 Chocolatey");
                System.err.println("  命令: choco install ffmpeg");
                System.err.println("");
                System.err.println("方式3: 手动安装");
                System.err.println("  1. 访问 https://ffmpeg.org/download.html");
            
                System.err.println("  2. 下载 Windows builds from gyan.dev");
                System.err.println("  3. 解压到 C:\\ffmpeg");
                System.err.println("  4. 添加 C:\\ffmpeg\\bin 到系统 PATH 环境变量");
                System.err.println("  5. 重新启动命令行窗口");
                System.err.println("");
                System.err.println("验证安装: ffmpeg -version");
                System.err.println("详细步骤: 运行 install_ffmpeg.bat");
                System.err.println("========================================");
                System.err.println("");
                throw new RuntimeException("FFmpeg 未安装。请先安装 FFmpeg，然后重试。", e);
            }
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("FFmpeg 被中断", e);
        }
    }

    /**
     * 计算裁切参数
     * 计算中心裁切的坐标和尺寸，以保持9:16的比例
     * 
     * @param width 原始视频宽度
     * @param height 原始视频高度
     * @param offsetPercentage 裁切位置偏移量 (-100=最左, 0=正中心, 100=最右)
     */
    private CropParams calculateCropParams(int width, int height, int offsetPercentage) {
        // 目标比例: 9:16
        // 计算应该裁切的宽度和高度

        // 获得当前宽高比
        double currentRatio = (double) width / height;
        double targetRatio = 9.0 / 16.0;  // 约 0.5625

        int cropWidth, cropHeight, cropX, cropY;

        if (currentRatio > targetRatio) {
            // 视频太宽，需要按高度来确定宽度
            cropHeight = height;
            cropWidth = (int) (height * targetRatio);
            
            // 根据偏移量计算水平位置
            int maxOffset = width - cropWidth;  // 最大偏移范围
            int baseX = maxOffset / 2;           // 正中心位置
            cropX = baseX + (int) (baseX * offsetPercentage / 100.0);
            cropX = Math.max(0, Math.min(cropX, maxOffset));  // 限制在有效范围内
            
            cropY = 0;
        } else {
            // 视频太窄或正好，需要按宽度来确定高度
            cropWidth = width;
            cropHeight = (int) (width / targetRatio);
            cropX = 0;
            cropY = (height - cropHeight) / 2;  // 垂直居中
        }

        return new CropParams(cropWidth, cropHeight, cropX, cropY);
    }

    /**
     * 生成输出文件路径
     */
    private Path generateOutputPath(String inputPath) {
        Path input = Paths.get(inputPath);
        String fileName = input.getFileName().toString();
        String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));

        // 在文件名后添加 _crop_9-16
        String outputFileName = nameWithoutExt + "_crop_9-16.mp4";

        return Paths.get(outputDir).resolve(outputFileName);
    }

    /**
     * 格式化视频时长
     */
    private String formatDuration(int milliseconds) {
        int seconds = milliseconds / 1000;
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;

        if (hours > 0) {
            return String.format("%d小时%d分%d秒", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%d分%d秒", minutes, secs);
        } else {
            return String.format("%d秒", secs);
        }
    }

    /**
     * 显示处理进度条
     */
    private void displayProgressBar(int frameCount) {
        // 每处理100帧显示一次进度
        if (frameCount % 100 == 0) {
            System.out.print(".");
            System.out.flush();
        }
    }

    /**
     * 裁切参数
     */
    private static class CropParams {
        int cropWidth;
        int cropHeight;
        int cropX;
        int cropY;

        CropParams(int cropWidth, int cropHeight, int cropX, int cropY) {
            this.cropWidth = cropWidth;
            this.cropHeight = cropHeight;
            this.cropX = cropX;
            this.cropY = cropY;
        }
    }

    /**
     * 视频信息
     */
    private static class VideoInfo {
        int width;
        int height;
        int duration;

        VideoInfo() {
            this.width = 0;
            this.height = 0;
            this.duration = 0;
        }
    }
}
