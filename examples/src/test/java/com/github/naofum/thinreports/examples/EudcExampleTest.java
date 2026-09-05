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
 * Java port of the Ruby {@code eudc/eudc.rb} example. The Ruby version registers
 * {@code eudc.ttf} as a fallback font so an EUDC (Gaiji / external) character can
 * be drawn.
 *
 * <p>The eudc layout uses the {@code IPAMincho} family. thinreports-java resolves
 * IPAMincho from {@link com.github.naofum.thinreports.Settings#getIpamincho()},
 * so we point that at {@code eudc.ttf} to embed the font that carries the
 * external character glyph.</p>
 *
 * <p>{@code eudc.ttf} is a Windows EUDC font whose glyphs live in the Unicode
 * Private Use Area (starting at U+E000). We render one of those external
 * characters here; full Japanese text would additionally require a complete CJK
 * font, which is out of scope for this hermetic example.</p>
 */
class EudcExampleTest {

    @TempDir
    File tempDir;

    private String resource(String name) {
        return new File("src/test/resources/" + name).getPath();
    }

    @Test
    void generatesEudcPdf() throws Exception {
        TRGenerator generator = new TRGenerator();
        // Point IPAMincho (used by eudc.tlf) at the external-character font.
        generator.getSettings().setIpamincho(resource("eudc.ttf"));

        Map<String, Object> map = new HashMap<>();
        // U+E000 is an EUDC (external / Gaiji) character present in eudc.ttf.
        map.put("eudc", "\uE000\uE001\uE002");

        generator.newDocument(resource("eudc.tlf"), map);
        File out = new File(tempDir, "eudc.pdf");
        generator.save(out.getPath());
        generator.close();

        assertTrue(out.exists() && out.length() > 0, "eudc PDF should be created");
        try (PDDocument doc = Loader.loadPDF(out)) {
            assertEquals(1, doc.getNumberOfPages());
        }
    }
}
