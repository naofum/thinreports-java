package com.github.naofum.thinreports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TRGeneratorTest {

    @TempDir
    File tempDir;

    private String resource(String name) {
        return new File("src/test/resources/" + name).getPath();
    }

    @Test
    void generatesTextBlockPdf() throws Exception {
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
        map.put("bold_and_italic", "To bold from normal.\nTo italic from normal.");
        map.put("underline_and_linethrough", "To underline.\nTo line-through.");
        map.put("font_size_12", "To 18 from 12");
        map.put("font_color_black", "To red from black.");
        map.put("text_align_and_vertical_align", "To right from left.\nTo bottom from top.");

        File out = new File(tempDir, "text_block.pdf");
        generator.newDocument(resource("text_block.tlf"), map);
        generator.save(out.getPath());
        generator.close();

        assertTrue(out.exists(), "output PDF should be created");
        assertTrue(out.length() > 0, "output PDF should not be empty");

        try (PDDocument doc = Loader.loadPDF(out)) {
            assertEquals(1, doc.getNumberOfPages());
        }
    }

    @Test
    void addPageProducesMultiplePages() throws Exception {
        TRGenerator generator = new TRGenerator();
        generator.getSettings().setIpamincho(resource("ipam.ttf"));
        Map<String, Object> map = new HashMap<>();

        generator.newDocument(resource("text_block.tlf"), map);
        generator.addPage(resource("text_block.tlf"), map);

        File out = new File(tempDir, "multi.pdf");
        generator.save(out.getPath());
        generator.close();

        try (PDDocument doc = Loader.loadPDF(out)) {
            assertEquals(2, doc.getNumberOfPages());
        }
    }
}
