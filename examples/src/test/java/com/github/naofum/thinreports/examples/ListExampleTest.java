package com.github.naofum.thinreports.examples;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.naofum.thinreports.TRGenerator;
import com.github.naofum.thinreports.render.ListRenderer;

/**
 * Java ports of the Ruby list examples:
 * <ul>
 *   <li>{@code list/basic/basic_list.rb} — repeats a detail row, auto page-break;</li>
 *   <li>{@code list/advanced/advanced_list.rb} — per-page footer row count and a
 *       grand total footer;</li>
 *   <li>{@code list/group-rows/group_rows.rb} — a group heading shown once per
 *       group (blood type), continuation rows hiding the heading.</li>
 * </ul>
 */
class ListExampleTest {

    @TempDir
    File tempDir;

    private String resource(String name) {
        return new File("src/test/resources/" + name).getPath();
    }

    @Test
    void basicList() throws Exception {
        TRGenerator generator = new TRGenerator();
        generator.newDocument(resource("basic_list.tlf"), new HashMap<>());
        for (int t = 0; t < 30; t++) {
            Map<String, Object> row = new HashMap<>();
            row.put("detail", "row#" + t);
            generator.addRow(row);
        }
        File out = new File(tempDir, "basic_list.pdf");
        generator.save(out.getPath());
        generator.close();

        assertTrue(out.exists() && out.length() > 0, "basic_list PDF should be created");
        try (PDDocument doc = Loader.loadPDF(out)) {
            assertTrue(doc.getNumberOfPages() >= 1, "basic_list should produce pages");
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("row#0"), "detail rows should render");
        }
    }

    @Test
    void advancedList() throws Exception {
        TRGenerator generator = new TRGenerator();
        generator.newDocument(resource("advanced_list.tlf"), new HashMap<>());
        for (int t = 0; t < 30; t++) {
            Map<String, Object> row = new HashMap<>();
            row.put("detail", "Detail#" + t);
            generator.addRow(row);
        }

        // Per-page footer: number of rows on this page.
        generator.setPageFooterCallback((pageNumber, pageRows) -> {
            Map<String, Object> v = new HashMap<>();
            v.put("page_footer", "Page row count: " + pageRows.size());
            return v;
        });
        // Footer: total number of rows across all pages.
        generator.setFooterCallback(allRows -> {
            Map<String, Object> v = new HashMap<>();
            v.put("footer", "Row count: " + allRows.size());
            return v;
        });

        File out = new File(tempDir, "advanced_list.pdf");
        generator.save(out.getPath());
        generator.close();

        assertTrue(out.exists() && out.length() > 0, "advanced_list PDF should be created");
        try (PDDocument doc = Loader.loadPDF(out)) {
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("Page row count:"), "page-footer should render");
            assertTrue(text.contains("Row count: 30"), "footer grand total should render");
        }
    }

    @Test
    void groupRows() throws Exception {
        // People sorted by blood type: A, A, B, B, O, O.
        String[][] people = {
            {"James", "18", "A"},
            {"Smith", "21", "A"},
            {"Robert", "24", "B"},
            {"Johnson", "35", "B"},
            {"Linda", "25", "O"},
            {"Mary", "39", "O"},
        };

        TRGenerator generator = new TRGenerator();
        generator.newDocument(resource("group_rows.tlf"), new HashMap<>());

        String currentGroup = null;
        for (String[] p : people) {
            String bloodType = p[2];
            boolean groupHead = !bloodType.equals(currentGroup);
            currentGroup = bloodType;

            Map<String, Object> row = new HashMap<>();
            row.put("name", p[0]);
            row.put("age", p[1]);
            if (groupHead) {
                // First row of a group: show the blood_group heading. The tlf's
                // format is "Blood type: {value}", so pass the raw blood type.
                row.put("blood_group", bloodType);
            } else {
                // Continuation row: hide the group heading.
                row.put("blood_group", "");
                row.put(ListRenderer.HIDDEN_IDS_KEY, Set.of("blood_group"));
            }
            generator.addRow(row);
        }

        File out = new File(tempDir, "group_rows.pdf");
        generator.save(out.getPath());
        generator.close();

        assertTrue(out.exists() && out.length() > 0, "group_rows PDF should be created");
        try (PDDocument doc = Loader.loadPDF(out)) {
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("Smith") && text.contains("Mary"),
                    "all names should render, got: " + text.trim());
            int headings = countOccurrences(text, "Blood type:");
            assertTrue(headings == 3,
                    "group heading should appear once per group (3), got " + headings
                            + " in: " + text.trim());
        }
    }

    private int countOccurrences(String haystack, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) >= 0) {
            count++;
            idx += needle.length();
        }
        return count;
    }
}
