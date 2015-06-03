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

package examples;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.krysalis.barcode4j.BarcodeException;
import org.krysalis.barcode4j.BarcodeGenerator;
import org.krysalis.barcode4j.BarcodeUtil;
import org.krysalis.barcode4j.output.svg.SVGCanvasProvider;
import org.xml.sax.SAXException;

import com.github.naofum.thinreports.ThinReportsGenerator;

/**
 * Barcode example
 * 
 * @author Naofumi Fukue
 * 
 */
public class Barcode {

	public static void main(String[] args) {
		ThinReportsGenerator generator = new ThinReportsGenerator();
		Map<String, Object> map = new HashMap<String, Object>();

		DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();
		try {
			Configuration cfg = builder.build(new ByteArrayInputStream(
					"<barcode><ean-13><module-width>0.4mm</module-width></ean-13></barcode>"
							.getBytes()));
			BarcodeGenerator gen = BarcodeUtil.getInstance()
					.createBarcodeGenerator(cfg);
			SVGCanvasProvider provider = new SVGCanvasProvider(false, 0);
			gen.generateBarcode(provider, "2001234567893");
			org.w3c.dom.DocumentFragment jan_13 = provider.getDOMFragment();
			map.put("jan_13", jan_13);

			cfg = builder.build(new ByteArrayInputStream(
					"<barcode><ean-8><module-width>0.4mm</module-width></ean-8></barcode>"
							.getBytes()));
			gen = BarcodeUtil.getInstance().createBarcodeGenerator(cfg);
			SVGCanvasProvider provider2 = new SVGCanvasProvider(false, 0);
			gen.generateBarcode(provider2, "20123451");
			org.w3c.dom.DocumentFragment jan_8 = provider2.getDOMFragment();
			map.put("jan_8", jan_8);

			cfg = builder.build(new ByteArrayInputStream(
					"<barcode><qrcode><module-width>0.4mm</module-width></qrcode></barcode>"
							.getBytes()));
			gen = BarcodeUtil.getInstance().createBarcodeGenerator(cfg);
			SVGCanvasProvider provider3 = new SVGCanvasProvider(false, 0);
			gen.generateBarcode(provider3, "1234567890123");
			org.w3c.dom.DocumentFragment qr_code = provider3.getDOMFragment();
			map.put("qr_code", qr_code);
		} catch (ConfigurationException e) {
			e.printStackTrace();
		} catch (BarcodeException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}

		try {
			generator.addPage("barcode.tlf", map);
			generator.generate("barcode.pdf");
			generator.generateXml("barcode.xml");
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(-1);
		}

	}
}
