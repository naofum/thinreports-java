package com.github.naofum.thinreports.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.naofum.thinreports.TRGenerator;

/**
 * Java port of the Ruby {@code multiple-layout/multiple_layout.rb} example. It
 * mixes three layouts in a single document: a cover page, five body pages (each
 * showing an incrementing {@code content} value) and a back-cover page.
 *
 * <p>In thinreports-java, mixing layouts is done by passing a different .tlf to
 * each {@code addPage} call.</p>
 */
class MultipleLayoutExampleTest {

    @TempDir
    File tempDir;

    private String resource(String name) {
        return new File("src/test/resources/" + name).getPath();
    }

    @Test
    void generatesMixedLayoutPdf() throws Exception {
        TRGenerator generator = new TRGenerator();

        // Cover page.
        generator.newDocument(resource("multiple_layout_cover.tlf"), new HashMap<>());

        // Five body pages using the default layout.
        for (int t = 1; t <= 5; t++) {
            Map<String, Object> map = new HashMap<>();
            map.put("content", t);
            generator.addPage(resource("multiple_layout_default.tlf"), map);
        }

        // Back cover.
        generator.addPage(resource("multiple_layout_back_cover.tlf"), new HashMap<>());

        File out = new File(tempDir, "multiple_layout.pdf");
        generator.save(out.getPath());
        generator.close();

        assertTrue(out.exists() && out.length() > 0, "multiple-layout PDF should be created");
        try (PDDocument doc = Loader.loadPDF(out)) {
            // 1 cover + 5 body + 1 back cover = 7 pages.
            assertEquals(7, doc.getNumberOfPages());
        }
    }
}
