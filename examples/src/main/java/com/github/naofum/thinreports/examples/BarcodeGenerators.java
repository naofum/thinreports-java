/*
 * Copyright 2015 Naofumi Fukue
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.naofum.thinreports.examples;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.krysalis.barcode4j.BarcodeGenerator;
import org.krysalis.barcode4j.BarcodeUtil;
import org.krysalis.barcode4j.configuration.Configuration;
import org.krysalis.barcode4j.output.bitmap.BitmapCanvasProvider;
import org.krysalis.barcode4j.tools.ConfigurationUtil;
import org.w3c.dom.Document;

/**
 * Generates barcode / QR images with barcode4j, returning {@link BufferedImage}
 * suitable to be placed into an {@code image-block} element via the data map.
 *
 * <p>This lives in the examples module: barcode support is not part of the core
 * library, which only knows how to draw an image once one is supplied.</p>
 */
public final class BarcodeGenerators {

    private BarcodeGenerators() {
    }

    /** Generate a JAN/EAN-13 barcode. */
    public static BufferedImage ean13(String value, int dpi) throws Exception {
        return generate("<barcode><ean-13><module-width>0.4mm</module-width></ean-13></barcode>", value, dpi);
    }

    /** Generate a JAN/EAN-8 barcode. */
    public static BufferedImage ean8(String value, int dpi) throws Exception {
        return generate("<barcode><ean-8><module-width>0.4mm</module-width></ean-8></barcode>", value, dpi);
    }

    /** Generate a QR code. */
    public static BufferedImage qr(String value, int dpi) throws Exception {
        return generate("<barcode><qrcode><module-width>0.4mm</module-width></qrcode></barcode>", value, dpi);
    }

    private static BufferedImage generate(String cfgXml, String value, int dpi) throws Exception {
        Document document = DocumentBuilderFactory.newNSInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(cfgXml.getBytes()));
        Configuration cfg = ConfigurationUtil.buildConfiguration(document);
        BarcodeGenerator gen = BarcodeUtil.getInstance().createBarcodeGenerator(cfg);
        BitmapCanvasProvider provider =
                new BitmapCanvasProvider(dpi, BufferedImage.TYPE_BYTE_BINARY, false, 0);
        gen.generateBarcode(provider, value);
        provider.finish();
        return provider.getBufferedImage();
    }
}
