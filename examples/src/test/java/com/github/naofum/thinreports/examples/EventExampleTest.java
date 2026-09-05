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
 * Java port of the Ruby {@code event/event.rb} example. The Ruby version uses
 * {@code on_page_create} / iterate-pages callbacks to stamp each page with its
 * page number, the total page count, and event marker texts.
 *
 * <p>thinreports-java resolves values per page via the data map passed to
 * {@code addPage}, so the equivalent here is to build three pages and provide
 * the page number, total, and event texts for each.</p>
 */
class EventExampleTest {

    @TempDir
    File tempDir;

    private String resource(String name) {
        return new File("src/test/resources/" + name).getPath();
    }

    @Test
    void generatesEventPdfWithThreePages() throws Exception {
        int pageCount = 3;

        TRGenerator generator = new TRGenerator();
        for (int i = 1; i <= pageCount; i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("event_page_create", "Dispatched at before page creating.");
            map.put("event_generate", "Dispatch at before report generating.");
            map.put("page", i);
            map.put("total", pageCount);
            if (i == 1) {
                generator.newDocument(resource("event.tlf"), map);
            } else {
                generator.addPage(resource("event.tlf"), map);
            }
        }

        File out = new File(tempDir, "event.pdf");
        generator.save(out.getPath());
        generator.close();

        assertTrue(out.exists() && out.length() > 0, "event PDF should be created");
        try (PDDocument doc = Loader.loadPDF(out)) {
            assertEquals(pageCount, doc.getNumberOfPages());
        }
    }
}
