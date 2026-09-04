package com.github.naofum.thinreports.visual;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.naofum.thinreports.TRGenerator;

/**
 * Visual regression test for the text-block layout.
 *
 * <p>Golden images live under {@code src/test/resources/golden/}. Run with
 * {@code -DupdateGolden=true} to (re)generate them after an intended change,
 * then commit the PNGs. Without a golden image present the test writes the
 * baseline on first run so a fresh checkout still builds.</p>
 */
class TextBlockVisualTest {

    private static final float DPI = 100f;
    private static final int TOLERANCE = 20;
    private static final double MAX_DIFF_RATIO = 0.01; // 1% of pixels

    @TempDir
    File tempDir;

    private String resource(String name) {
        return new File("src/test/resources/" + name).getPath();
    }

    private File goldenFile(String name) {
        return new File("src/test/resources/golden/" + name);
    }

    @Test
    void textBlockMatchesGolden() throws Exception {
        File out = renderTextBlock();
        BufferedImage actual = VisualCompare.renderPage(out, 0, DPI);

        File golden = goldenFile("text_block_p0.png");
        boolean update = Boolean.getBoolean("updateGolden");
        if (update || !golden.exists()) {
            VisualCompare.writePng(actual, golden);
            assertTrue(golden.exists(), "golden image should have been written");
            return;
        }

        BufferedImage expected = VisualCompare.readPng(golden);
        double ratio = VisualCompare.diffRatio(expected, actual, TOLERANCE);
        assertTrue(ratio <= MAX_DIFF_RATIO,
                String.format("visual diff %.4f exceeds threshold %.4f", ratio, MAX_DIFF_RATIO));
    }

    private File renderTextBlock() throws Exception {
        TRGenerator generator = new TRGenerator();
        generator.getSettings().setIpamincho(resource("ipam.ttf"));
        generator.getSettings().setIpagochic(resource("ipag.ttf"));

        Map<String, Object> map = new HashMap<>();
        map.put("single_line_left", "Left(Default)");
        map.put("single_line_center", "Center");
        map.put("single_line_right", "Right");
        map.put("multi_line", "ThinReports Text Block Tool.\nThinReports Text Block Tool.");
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2015, Calendar.JULY, 7);
        map.put("datetime_format", cal.getTime());
        map.put("number_format", 99999.9999);
        map.put("padding_format", 999);
        map.put("basic_format", 1980);
        map.put("bold_and_italic", "*To bold*\n_To italic_");
        map.put("underline_and_linethrough", "__To underline__\n__{0.25:}To line-through__");
        map.put("font_size_12", "To 18 from 12");
        map.put("font_color_black", "{color:#ff0000}To red from black.");
        map.put("text_align_and_vertical_align", "To right from left.\nTo bottom from top.");

        File out = new File(tempDir, "text_block.pdf");
        generator.newDocument(resource("text_block.tlf"), map);
        generator.save(out.getPath());
        generator.close();
        return out;
    }
}
