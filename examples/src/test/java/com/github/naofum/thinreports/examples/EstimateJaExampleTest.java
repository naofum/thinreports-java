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
 * Java port of the Ruby {@code estimate-ja/estimate.rb} example: the Japanese
 * variant of the estimate layout. It uses the {@code IPAMincho}/{@code IPAGothic}
 * families, so CJK fonts must be embedded for the text to render; if the IPA
 * TTFs are not present the generator falls back to Helvetica.
 */
class EstimateJaExampleTest {

    @TempDir
    File tempDir;

    private String resource(String name) {
        return new File("src/test/resources/" + name).getPath();
    }

    private Map<String, Object> detail(int no, String title, double unitPrice, double amount,
            double price, String note) {
        Map<String, Object> row = new HashMap<>();
        row.put("no", no);
        row.put("title", title);
        row.put("unit_price", unitPrice);
        row.put("amount", amount);
        row.put("price", price);
        row.put("note", note);
        return row;
    }

    @Test
    void generatesEstimateJaPdf() throws Exception {
        TRGenerator generator = new TRGenerator();

        // The estimate-ja layout uses IPAMincho. Point it at a Japanese TTF if
        // one is available so CJK glyphs embed correctly; otherwise fall back to
        // ASCII data so the example still renders on machines without a CJK font.
        boolean cjk = false;
        for (String candidate : new String[] {
                "C:\\Windows\\Fonts\\yumin.ttf",
                "C:\\Windows\\Fonts\\yuminl.ttf",
                "/usr/share/fonts/truetype/ipafont/ipam.ttf",
                "ipam.ttf" }) {
            if (new File(candidate).exists()) {
                generator.getSettings().setIpamincho(candidate);
                cjk = true;
                break;
            }
        }

        Map<String, Object> header = new HashMap<>();
        if (cjk) {
            header.put("customer", "\u30b5\u30f3\u30d7\u30eb\u682a\u5f0f\u4f1a\u793e");
            header.put("title", "\u304a\u898b\u7a4d\u66f8");
            header.put("note", "\u3053\u308c\u306f\u30b5\u30f3\u30d7\u30eb\u3067\u3059\u3002");
        } else {
            header.put("customer", "Sample Co., Ltd.");
            header.put("title", "Estimate");
            header.put("note", "This is a sample.");
        }
        header.put("no", 1234);
        header.put("price", 1500);
        header.put("tax", 150);
        header.put("total_price", 1650);

        generator.newDocument(resource("estimate_ja.tlf"), header);

        String itemName = cjk ? "\u5546\u54c1" : "Item";
        for (int i = 1; i <= 20; i++) {
            generator.addRow(detail(i, itemName + i, 500, 1, 500, ""));
        }

        File out = new File(tempDir, "estimate_ja.pdf");
        generator.save(out.getPath());
        generator.close();

        assertTrue(out.exists() && out.length() > 0, "estimate-ja PDF should be created");
        try (PDDocument doc = Loader.loadPDF(out)) {
            assertTrue(doc.getNumberOfPages() >= 1,
                    "estimate-ja should produce at least one page");
        }
    }
}
