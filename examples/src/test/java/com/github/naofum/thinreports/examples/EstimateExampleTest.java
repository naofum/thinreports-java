package com.github.naofum.thinreports.examples;

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
 * Java port of the Ruby {@code estimate/estimate.rb} example. It builds an
 * estimate document with a header, a repeating detail list, a per-page subtotal
 * in the list page-footer and a grand total in the list footer.
 */
class EstimateExampleTest {

    @TempDir
    File tempDir;

    private String resource(String name) {
        return new File("src/test/resources/" + name).getPath();
    }

    private Map<String, Object> detail(int no, String name, double rate, int qty, double amount) {
        Map<String, Object> row = new HashMap<>();
        row.put("no", no);
        row.put("name", name);
        row.put("rate", rate);
        row.put("qty", qty);
        row.put("amount", amount);
        return row;
    }

    @Test
    void generatesEstimatePdf() throws Exception {
        TRGenerator generator = new TRGenerator();

        Map<String, Object> header = new HashMap<>();
        header.put("no", 1234);
        header.put("customer_name", "Sample1 Co., Ltd.");
        header.put("customer_address", "1234, Sample1cho, Sample1-shi, Shimane, Japan");
        header.put("customer_post_code", "123-4567");
        header.put("my_name", "Matsukei Co., Ltd.");
        header.put("my_address", "735-211, Nogifukutomicho, Matsue-shi, Shimane, Japan");
        header.put("my_post_code", "690-0046");
        header.put("my_tel_number", "+81-854-32-1616");
        header.put("my_fax_number", "+81-852-32-1629");
        header.put("notes", "Estimate exsample1!");

        generator.newDocument(resource("estimate.tlf"), header);

        // Enough rows to force a page break and exercise page/grand totals.
        for (int i = 1; i <= 35; i++) {
            generator.addRow(detail(i, "xxxxxxxxxx", 500, 1, 500));
        }

        // Per-page subtotal in the list page-footer.
        generator.setPageFooterCallback((pageNumber, pageRows) -> {
            double subtotal = pageRows.stream()
                    .mapToDouble(r -> ((Number) r.get("amount")).doubleValue()).sum();
            Map<String, Object> v = new HashMap<>();
            v.put("sub_total", subtotal);
            return v;
        });

        // Grand total in the list footer.
        generator.setFooterCallback(allRows -> {
            double total = allRows.stream()
                    .mapToDouble(r -> ((Number) r.get("amount")).doubleValue()).sum();
            Map<String, Object> v = new HashMap<>();
            v.put("total", total);
            return v;
        });

        File out = new File(tempDir, "estimate.pdf");
        generator.save(out.getPath());
        generator.close();

        assertTrue(out.exists() && out.length() > 0, "estimate PDF should be created");
        try (PDDocument doc = Loader.loadPDF(out)) {
            assertTrue(doc.getNumberOfPages() >= 2,
                    "35 detail rows should span multiple pages, got " + doc.getNumberOfPages());
        }
    }
}
