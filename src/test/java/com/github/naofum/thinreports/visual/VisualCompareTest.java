package com.github.naofum.thinreports.visual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class VisualCompareTest {

    private BufferedImage solid(int w, int h, Color c) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, c.getRGB());
            }
        }
        return img;
    }

    @Test
    void identicalImagesHaveZeroDiff() {
        BufferedImage a = solid(10, 10, Color.WHITE);
        BufferedImage b = solid(10, 10, Color.WHITE);
        assertEquals(0.0, VisualCompare.diffRatio(a, b, 0), 1e-9);
    }

    @Test
    void differentSizesReportFullyDifferent() {
        BufferedImage a = solid(10, 10, Color.WHITE);
        BufferedImage b = solid(20, 10, Color.WHITE);
        assertEquals(1.0, VisualCompare.diffRatio(a, b, 0), 1e-9);
    }

    @Test
    void halfDifferentPixels() {
        BufferedImage a = solid(10, 10, Color.WHITE);
        BufferedImage b = solid(10, 10, Color.WHITE);
        // Flip the top half to black.
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 10; x++) {
                b.setRGB(x, y, Color.BLACK.getRGB());
            }
        }
        double ratio = VisualCompare.diffRatio(a, b, 20);
        assertTrue(Math.abs(ratio - 0.5) < 1e-6, "expected ~0.5, got " + ratio);
    }

    @Test
    void toleranceMasksSmallDifferences() {
        BufferedImage a = solid(4, 4, new Color(100, 100, 100));
        BufferedImage b = solid(4, 4, new Color(110, 110, 110));
        // Within tolerance 20 -> no diff.
        assertEquals(0.0, VisualCompare.diffRatio(a, b, 20), 1e-9);
        // Below tolerance 5 -> all diff.
        assertEquals(1.0, VisualCompare.diffRatio(a, b, 5), 1e-9);
    }
}
