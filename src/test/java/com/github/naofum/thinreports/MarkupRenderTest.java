package com.github.naofum.thinreports;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MarkupRenderTest {

    @TempDir
    File tempDir;

    private String resource(String name) {
        return new File("src/test/resources/" + name).getPath();
    }

    @Test
    void markupMarkersAreStrippedFromRenderedText() throws Exception {
        TRGenerator generator = new TRGenerator();
        generator.getSettings().setIpamincho(resource("ipam.ttf"));

        Map<String, Object> map = new HashMap<>();
        // Colored + bold markup: the markers must not appear in the output text,
        // only the visible content "RedBold".
        map.put("font_color_black", "{color:#ff0000}*RedBold*");

        generator.newDocument(resource("text_block.tlf"), map);
        File out = new File(tempDir, "markup.pdf");
        generator.save(out.getPath());
        generator.close();

        try (PDDocument doc = Loader.loadPDF(out)) {
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("RedBold"),
                    "visible markup content should be rendered, got: " + text.trim());
            assertTrue(!text.contains("{color:") && !text.contains("*RedBold*"),
                    "markup markers should be stripped, got: " + text.trim());
        }
    }

    @Test
    void markupLineWrapsAndKeepsAllVisibleText() throws Exception {
        TRGenerator generator = new TRGenerator();
        generator.getSettings().setIpamincho(resource("ipam.ttf"));

        // A long markup string, far wider than font_color_black's 274.7pt box at
        // 12pt, so it must wrap onto several visual lines. Wrapping used to be
        // skipped for markup lines; this verifies the content survives the break.
        StringBuilder sb = new StringBuilder("{color:#0000ff}");
        for (int i = 0; i < 12; i++) {
            sb.append("*Bold").append(i).append("* ");
        }
        Map<String, Object> map = new HashMap<>();
        map.put("font_color_black", sb.toString());

        generator.newDocument(resource("text_block.tlf"), map);
        File out = new File(tempDir, "markup_wrap.pdf");
        generator.save(out.getPath());
        generator.close();

        try (PDDocument doc = Loader.loadPDF(out)) {
            String text = new PDFTextStripper().getText(doc);
            // Every visible token must be present despite wrapping...
            for (int i = 0; i < 12; i++) {
                assertTrue(text.contains("Bold" + i),
                        "token Bold" + i + " should be present after wrapping, got: " + text.trim());
            }
            // ...and markers must be stripped.
            assertTrue(!text.contains("{color:") && !text.contains("*Bold"),
                    "markup markers should be stripped, got: " + text.trim());
        }
    }

    @Test
    void markupLineWrapsAcrossMultipleLines() throws Exception {
        TRGenerator generator = new TRGenerator();
        generator.getSettings().setIpamincho(resource("ipam.ttf"));

        String longText = "*WrapWrapWrapWrapWrapWrapWrapWrap "
                + "WrapWrapWrapWrapWrapWrapWrapWrap "
                + "WrapWrapWrapWrapWrapWrapWrapWrap*";
        Map<String, Object> map = new HashMap<>();
        map.put("multi_line", longText);

        generator.newDocument(resource("text_block.tlf"), map);
        File out = new File(tempDir, "markup_wrap.pdf");
        generator.save(out.getPath());
        generator.close();

        try (PDDocument doc = Loader.loadPDF(out)) {
            String text = new PDFTextStripper().getText(doc);
            assertTrue(!text.contains("*"), "bold markers should be stripped");
            String flattened = text.replaceAll("\\s+", "");
            assertTrue(flattened.contains("WrapWrapWrapWrapWrapWrapWrapWrap"),
                    "wrapped bold content should be present, got: " + text.trim());
            long lineCount = text.lines().filter(l -> l.contains("Wrap")).count();
            assertTrue(lineCount >= 2,
                    "long markup line should wrap to >= 2 lines, got " + lineCount);
        }
    }
}
