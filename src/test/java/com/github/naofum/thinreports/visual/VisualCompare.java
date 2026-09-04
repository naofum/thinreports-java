package com.github.naofum.thinreports.visual;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 * Test-only helper for visual regression: rasterizes PDF pages and compares
 * them against stored golden PNG images by fraction of differing pixels.
 */
public final class VisualCompare {

    private VisualCompare() {
    }

    /** Render one page of a PDF to a BufferedImage at the given DPI. */
    public static BufferedImage renderPage(File pdf, int pageIndex, float dpi) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            return renderer.renderImageWithDPI(pageIndex, dpi);
        }
    }

    /**
     * Fraction (0..1) of pixels that differ by more than {@code tolerance} per
     * channel. Images of differing size are reported as fully different (1.0).
     */
    public static double diffRatio(BufferedImage a, BufferedImage b, int tolerance) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            return 1.0;
        }
        long differing = 0;
        long total = (long) a.getWidth() * a.getHeight();
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (pixelDiffers(a.getRGB(x, y), b.getRGB(x, y), tolerance)) {
                    differing++;
                }
            }
        }
        return (double) differing / total;
    }

    private static boolean pixelDiffers(int rgb1, int rgb2, int tolerance) {
        int r1 = (rgb1 >> 16) & 0xff;
        int g1 = (rgb1 >> 8) & 0xff;
        int b1 = rgb1 & 0xff;
        int r2 = (rgb2 >> 16) & 0xff;
        int g2 = (rgb2 >> 8) & 0xff;
        int b2 = rgb2 & 0xff;
        return Math.abs(r1 - r2) > tolerance
                || Math.abs(g1 - g2) > tolerance
                || Math.abs(b1 - b2) > tolerance;
    }

    public static void writePng(BufferedImage image, File out) throws IOException {
        out.getParentFile().mkdirs();
        ImageIO.write(image, "png", out);
    }

    public static BufferedImage readPng(File in) throws IOException {
        return ImageIO.read(in);
    }
}
