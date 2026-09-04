package com.github.naofum.thinreports;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.github.naofum.thinreports.render.ListRenderer;

class GroupRowsTest {

    @TempDir
    File tempDir;

    private String resource(String name) {
        return new File("src/test/resources/" + name).getPath();
    }

    private void addRow(TRGenerator g, String name, String age, String bloodGroup, boolean groupHead) {
        Map<String, Object> row = new HashMap<>();
        row.put("name", name);
        row.put("age", age);
        if (groupHead) {
            row.put("blood_group", bloodGroup);
        } else {
            // Continuation row: hide the group heading (blood_group) for this row.
            row.put("blood_group", "");
            row.put(ListRenderer.HIDDEN_IDS_KEY, Set.of("blood_group"));
        }
        g.addRow(row);
    }

    @Test
    void groupHeadingShownOncePerGroup() throws Exception {
        TRGenerator generator = new TRGenerator();
        generator.newDocument(resource("group_rows.tlf"), new HashMap<>());

        // Group A: 2 rows, group B: 1 row. Heading only on the first row of a group.
        addRow(generator, "Alice", "30", "A", true);
        addRow(generator, "Bob", "25", "A", false);
        addRow(generator, "Carol", "40", "B", true);

        File out = new File(tempDir, "group_rows.pdf");
        generator.save(out.getPath());
        generator.close();

        try (PDDocument doc = Loader.loadPDF(out)) {
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("Alice") && text.contains("Bob") && text.contains("Carol"),
                    "all names should be rendered, got: " + text.trim());
            // "Blood type:" heading appears once per group (2 groups here).
            int count = countOccurrences(text, "Blood type:");
            assertEquals(2, count,
                    "group heading should appear once per group, got " + count + " in: " + text.trim());
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
