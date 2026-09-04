package com.github.naofum.thinreports;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ListTest {

    @TempDir
    File tempDir;

    private String resource(String name) {
        return new File("src/test/resources/" + name).getPath();
    }

    @Test
    void basicListWithManyRowsBreaksToMultiplePages() throws Exception {
        TRGenerator generator = new TRGenerator();
        Map<String, Object> map = new HashMap<>();

        generator.newDocument(resource("basic_list.tlf"), map);
        for (int i = 0; i < 60; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("detail", "row#" + i);
            generator.addRow(row);
        }

        File out = new File(tempDir, "basic_list.pdf");
        generator.save(out.getPath());
        generator.close();

        assertTrue(out.exists() && out.length() > 0, "list PDF should be created");

        try (PDDocument doc = Loader.loadPDF(out)) {
            // 60 rows at ~31pt each within a ~707pt content area should span
            // more than one page once auto-page-break kicks in.
            assertTrue(doc.getNumberOfPages() >= 2,
                    "60 detail rows should break across multiple pages, got "
                            + doc.getNumberOfPages());
        }
    }

    @Test
    void advancedListRendersPageFooterAndFooter() throws Exception {
        TRGenerator generator = new TRGenerator();
        Map<String, Object> map = new HashMap<>();

        generator.newDocument(resource("advanced_list.tlf"), map);
        for (int i = 0; i < 60; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("detail", "row#" + i);
            generator.addRow(row);
        }
        Map<String, Object> pf = new HashMap<>();
        pf.put("page_footer", "PAGE-SUBTOTAL");
        generator.setPageFooterValues(pf);
        Map<String, Object> f = new HashMap<>();
        f.put("footer", "GRAND-TOTAL");
        generator.setFooterValues(f);

        File out = new File(tempDir, "advanced_list.pdf");
        generator.save(out.getPath());
        generator.close();

        try (PDDocument doc = Loader.loadPDF(out)) {
            int pages = doc.getNumberOfPages();
            assertTrue(pages >= 2, "should span multiple pages, got " + pages);
            org.apache.pdfbox.text.PDFTextStripper stripper =
                    new org.apache.pdfbox.text.PDFTextStripper();
            String text = stripper.getText(doc);
            assertTrue(text.contains("PAGE-SUBTOTAL"), "page-footer value should be rendered");
            assertTrue(text.contains("GRAND-TOTAL"), "footer value should be rendered");
        }
    }

    @Test
    void perPagePageFooterCallbackProducesDistinctValues() throws Exception {
        TRGenerator generator = new TRGenerator();
        Map<String, Object> map = new HashMap<>();

        generator.newDocument(resource("advanced_list.tlf"), map);
        int rowCount = 60;
        for (int i = 0; i < rowCount; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("detail", "row#" + i);
            generator.addRow(row);
        }

        // Per-page callback: emit the page number and that page's row count so
        // each page-footer carries a value distinct from the others.
        generator.setPageFooterCallback((pageNumber, pageRows) -> {
            Map<String, Object> values = new HashMap<>();
            values.put("page_footer", "SUBTOTAL-p" + pageNumber + "-n" + pageRows.size());
            return values;
        });

        File out = new File(tempDir, "advanced_list_cb.pdf");
        generator.save(out.getPath());
        generator.close();

        try (PDDocument doc = Loader.loadPDF(out)) {
            int pages = doc.getNumberOfPages();
            assertTrue(pages >= 2, "should span multiple pages, got " + pages);

            org.apache.pdfbox.text.PDFTextStripper stripper =
                    new org.apache.pdfbox.text.PDFTextStripper();

            // Page 1 and page 2 must show different per-page subtotals.
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            String p1 = stripper.getText(doc);
            stripper.setStartPage(2);
            stripper.setEndPage(2);
            String p2 = stripper.getText(doc);

            assertTrue(p1.contains("SUBTOTAL-p1-"),
                    "page 1 should carry its own page-footer value, got: " + p1.trim());
            assertTrue(p2.contains("SUBTOTAL-p2-"),
                    "page 2 should carry its own page-footer value, got: " + p2.trim());
        }
    }

    @Test
    void footerCallbackAggregatesAllRows() throws Exception {
        TRGenerator generator = new TRGenerator();
        Map<String, Object> map = new HashMap<>();

        generator.newDocument(resource("advanced_list.tlf"), map);
        int rowCount = 60;
        for (int i = 0; i < rowCount; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("detail", "row#" + i);
            row.put("amount", i); // 0 + 1 + ... + 59 = 1770
            generator.addRow(row);
        }

        // Footer callback: aggregate a grand total from all rows.
        generator.setFooterCallback(allRows -> {
            long sum = 0;
            for (Map<String, Object> r : allRows) {
                Object a = r.get("amount");
                if (a instanceof Number n) {
                    sum += n.longValue();
                }
            }
            Map<String, Object> values = new HashMap<>();
            values.put("footer", "TOTAL-" + sum + "-n" + allRows.size());
            return values;
        });

        File out = new File(tempDir, "advanced_list_footer_cb.pdf");
        generator.save(out.getPath());
        generator.close();

        try (PDDocument doc = Loader.loadPDF(out)) {
            org.apache.pdfbox.text.PDFTextStripper stripper =
                    new org.apache.pdfbox.text.PDFTextStripper();
            String text = stripper.getText(doc);
            // Sum 0..59 = 1770, over 60 rows.
            assertTrue(text.contains("TOTAL-1770-n60"),
                    "footer should carry the aggregated grand total, got: " + text.trim());
        }
    }
}
