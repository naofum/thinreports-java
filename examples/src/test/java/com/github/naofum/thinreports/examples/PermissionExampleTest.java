package com.github.naofum.thinreports.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashMap;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.naofum.thinreports.SecuritySettings;
import com.github.naofum.thinreports.TRGenerator;

/**
 * Java port of the Ruby {@code permission/permission.rb} example. The Ruby
 * version passes a {@code security:} option to {@code report.generate} with a
 * user password, a random owner password and print/modify/copy permissions all
 * disabled.
 *
 * <p>thinreports-java exposes the equivalent via
 * {@link TRGenerator#setSecuritySettings(SecuritySettings)}.</p>
 */
class PermissionExampleTest {

    @TempDir
    File tempDir;

    private String resource(String name) {
        return new File("src/test/resources/" + name).getPath();
    }

    @Test
    void generatesEncryptedPdf() throws Exception {
        SecuritySettings security = new SecuritySettings()
                .setUserPassword("foo")
                .setOwnerPassword("owner-secret")
                .setCanPrint(false)
                .setCanModify(false)
                .setCanExtractContent(false);

        TRGenerator generator = new TRGenerator();
        generator.setSecuritySettings(security);
        generator.newDocument(resource("permission.tlf"), new HashMap<>());
        File out = new File(tempDir, "permission.pdf");
        generator.save(out.getPath());
        generator.close();

        assertTrue(out.exists() && out.length() > 0, "permission PDF should be created");

        // Re-open with the user password and verify the document is encrypted
        // and carries the restricted permissions.
        try (PDDocument doc = Loader.loadPDF(out, "foo")) {
            assertEquals(1, doc.getNumberOfPages());
            assertTrue(doc.isEncrypted(), "document should be encrypted");
            assertFalse(doc.getCurrentAccessPermission().canPrint(), "printing should be disabled");
            assertFalse(doc.getCurrentAccessPermission().canModify(), "modifying should be disabled");
            assertFalse(doc.getCurrentAccessPermission().canExtractContent(),
                    "content extraction should be disabled");
        }
    }
}
