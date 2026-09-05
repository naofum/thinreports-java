package com.github.naofum.thinreports.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.naofum.thinreports.TRGenerator;

/**
 * Java port of the Ruby {@code text-block/text_block.rb} example: fills a set of
 * text-block elements with plain, formatted and styled values.
 */
class TextBlockExampleTest {

    @TempDir
    File tempDir;

    private String resource(String name) {
        return new File("src/test/resources/" + name).getPath();
    }

    @Test
    void generatesTextBlockPdf() throws Exception {
        Map<String, Object> map = new HashMap<>();

        // Basic features
        map.put("single_line_left", "Left(Default)");
        map.put("single_line_center", "Center");
        map.put("single_line_right", "Right");
        map.put("multi_line", "Thinreports Text Block Tool.\nThinreports Text Block Tool.");

        // Simple format (the tlf carries the format definitions; we supply raw values)
        map.put("datetime_format", new java.util.Date());
        map.put("number_format", 99999.9999);
        map.put("padding_format", 999);
        map.put("basic_format", 1980);

        // Dynamic style values
        map.put("bold_and_italic", "To bold from normal.\nTo italic from normal.");
        map.put("underline_and_linethrough",
                "To underline from normal.\nTo line-through from normal.");
        map.put("font_size_12", "To 18 from 12");
        map.put("font_color_black", "To red from black.");
        map.put("text_align_and_vertical_align", "To right from left.\nTo bottom from top.");
        map.put("show_text_block", "To true from false.");

        TRGenerator generator = new TRGenerator();
        generator.newDocument(resource("text_block.tlf"), map);
        File out = new File(tempDir, "text_block.pdf");
        generator.save(out.getPath());
        generator.close();

        assertTrue(out.exists() && out.length() > 0, "text-block PDF should be created");
        try (PDDocument doc = Loader.loadPDF(out)) {
            assertEquals(1, doc.getNumberOfPages());
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("Left(Default)"), "left value should render, got: " + text.trim());
            assertTrue(text.contains("Center"), "center value should render");
        }
    }
}
