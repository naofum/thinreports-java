package com.github.naofum.thinreports.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.naofum.thinreports.TRGenerator;

class BarcodeExampleTest {

    @TempDir
    File tempDir;

    private String resource(String name) {
        return new File("src/test/resources/" + name).getPath();
    }

    @Test
    void generatesBarcodeAndQrIntoPdf() throws Exception {
        Map<String, Object> map = new HashMap<>();
        BufferedImage jan13 = BarcodeGenerators.ean13("2001234567893", 100);
        BufferedImage jan8 = BarcodeGenerators.ean8("20123451", 100);
        BufferedImage qr = BarcodeGenerators.qr("1234567890123", 200);

        assertTrue(jan13.getWidth() > 0 && jan8.getWidth() > 0 && qr.getWidth() > 0,
                "barcode images should be generated");

        map.put("jan_13", jan13);
        map.put("jan_8", jan8);
        map.put("qr_code", qr);

        TRGenerator generator = new TRGenerator();
        generator.newDocument(resource("barcode.tlf"), map);
        File out = new File(tempDir, "barcode.pdf");
        generator.save(out.getPath());
        generator.close();

        assertTrue(out.exists() && out.length() > 0, "barcode PDF should be created");
        try (PDDocument doc = Loader.loadPDF(out)) {
            assertEquals(1, doc.getNumberOfPages());
        }
    }
}
