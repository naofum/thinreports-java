package com.github.naofum.thinreports.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.naofum.thinreports.TRGenerator;

/**
 * Java port of the Ruby {@code image-block/image_block.rb} example. It places a
 * local image and a base64-encoded image into image-block elements.
 *
 * <p>The Ruby example also fetches a remote logo over HTTP; that network access
 * is intentionally omitted here to keep the test hermetic. Instead a small
 * generated in-memory image stands in for the remote one.</p>
 */
class ImageBlockExampleTest {

    @TempDir
    File tempDir;

    private String resource(String name) {
        return new File("src/test/resources/" + name).getPath();
    }

    // 1x1 red-dot PNG, same bytes used by the Ruby example.
    private static final String RED_DOT =
            "iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4"
            + "//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg==";

    @Test
    void generatesImageBlockPdf() throws Exception {
        Map<String, Object> map = new HashMap<>();

        // Local image loaded from the test resources.
        BufferedImage local = ImageIO.read(new File(resource("file/image.png")));
        assertTrue(local != null && local.getWidth() > 0, "local image should load");
        map.put("local_image", local);

        // Stand-in for the remote logo (network access omitted).
        BufferedImage remote = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
        map.put("remote_image", remote);

        // Base64-encoded image (String is decoded by ImageRenderer).
        map.put("base64_image", RED_DOT);

        TRGenerator generator = new TRGenerator();
        generator.newDocument(resource("image_block.tlf"), map);
        File out = new File(tempDir, "image_block.pdf");
        generator.save(out.getPath());
        generator.close();

        assertTrue(out.exists() && out.length() > 0, "image-block PDF should be created");
        try (PDDocument doc = Loader.loadPDF(out)) {
            assertEquals(1, doc.getNumberOfPages());
        }
    }
}
