package com.sismics.docs.core.util.format;

import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closer;
import com.sismics.util.io.InputStreamReaderThread;
import com.sismics.util.mime.MimeType;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Video format handler.
 *
 * @author bgamard
 */
public class VideoFormatHandler implements FormatHandler {
    @Override
    public boolean accept(String mimeType) {
        return mimeType.equals(MimeType.VIDEO_MP4) || mimeType.equals(MimeType.VIDEO_WEBM);
    }

    @Override
    public BufferedImage generateThumbnail(Path file) throws IOException {
        List<String> result = Lists.newLinkedList(Arrays.asList("ffmpeg", "-i"));
        result.add(file.toAbsolutePath().toString());
        result.addAll(Arrays.asList("-vf", "thumbnail", "-frames:v", "1", "-f", "mjpeg", "-"));
        ProcessBuilder pb = new ProcessBuilder(result);
        Process process = pb.start();

        // Consume the process error stream
        final String commandName = pb.command().get(0);
        new InputStreamReaderThread(process.getErrorStream(), commandName).start();

        // Consume the data as an image
        try (InputStream is = process.getInputStream()) {
            return ImageIO.read(is);
        }
    }

    @Override
    public String extractContent(String language, Path file) {
        String content = extractContent(Lists.newLinkedList(Arrays.asList("mediainfo", file.toAbsolutePath().toString())), false);
        if (content != null) {
            return content;
        }

        return extractContent(Lists.newLinkedList(Arrays.asList("ffmpeg", "-i", file.toAbsolutePath().toString())), true);
    }

    private String extractContent(List<String> command, boolean readErrorStream) {
        ProcessBuilder pb = new ProcessBuilder(command);
        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            return null;
        }

        try (InputStream is = process.getInputStream()) {
            String content = new String(ByteStreams.toByteArray(is), StandardCharsets.UTF_8);
            if (readErrorStream) {
                content += new String(ByteStreams.toByteArray(process.getErrorStream()), StandardCharsets.UTF_8);
            } else {
                final String commandName = pb.command().get(0);
                new InputStreamReaderThread(process.getErrorStream(), commandName).start();
            }
            return content.trim().isEmpty() ? null : content;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void appendToPdf(Path file, PDDocument doc, boolean fitImageToPage, int margin, MemoryUsageSetting memUsageSettings, Closer closer) {
        // Video cannot be appended to PDF files
    }
}
