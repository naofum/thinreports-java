package com.github.naofum.thinreports;

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

class PageNumberTest {

    @TempDir
    File tempDir;

    private String resource(String name) {
        return new File("src/test/resources/" + name).getPath();
    }

    @Test
    void pageNumberPlaceholdersResolvedPerPage() throws Exception {
        TRGenerator generator = new TRGenerator();
        Map<String, Object> map = new HashMap<>();

        // Three pages from the same layout.
        generator.newDocument(resource("pagenumber.tlf"), map);
        generator.addPage(resource("pagenumber.tlf"), map);
        generator.addPage(resource("pagenumber.tlf"), map);

        File out = new File(tempDir, "pageno.pdf");
        generator.save(out.getPath());
        generator.close();

        try (PDDocument doc = Loader.loadPDF(out)) {
            assertEquals(3, doc.getNumberOfPages());

            PDFTextStripper stripper = new PDFTextStripper();
            for (int p = 1; p <= 3; p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                String text = stripper.getText(doc);
                assertTrue(text.contains("Page " + p + " of 3"),
                        "page " + p + " should show 'Page " + p + " of 3', got: " + text.trim());
            }
        }
    }
}
