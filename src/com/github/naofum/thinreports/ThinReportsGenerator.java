package com.github.naofum.thinreports;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.dom.util.DocumentFactory;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.fop.svg.FOPSAXSVGDocumentFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.krysalis.barcode4j.BarcodeException;
import org.krysalis.barcode4j.BarcodeGenerator;
import org.krysalis.barcode4j.BarcodeUtil;
import org.krysalis.barcode4j.output.svg.SVGCanvasProvider;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class ThinReportsGenerator {

	private ThinReport thinReport = new ThinReport();

	private int lineNo = 0;

	public ThinReport addPage(String file, Map<String, Object> map)
			throws IOException {
		readTlf(file);
//		System.out.println(thinReport.getSvg());

		Document document = null;
		String parser = XMLResourceDescriptor.getXMLParserClassName();
		SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
		// f.setValidating(true);
		// String uri = SVGDOMImplementation.SVG_NAMESPACE_URI;
		document = f.createSVGDocument(file, new ByteArrayInputStream(
				thinReport.getSvg().getBytes("UTF-8")));

		if (document != null) {
			traceNodes(document, map);
			TransformerFactory transFactory = TransformerFactory.newInstance();
			try {
				Transformer transformer = transFactory.newTransformer();
				Source src = new DOMSource(document);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				Result result = new StreamResult(out);
				transformer.transform(src, result);
				TranscoderInput input = new TranscoderInput();
				input.setInputStream(new ByteArrayInputStream(out.toByteArray()));
				thinReport.addDocument(input);
				out.close();
				// } catch (TransformerConfigurationException e) {
				// e.printStackTrace();
			} catch (TransformerException e) {
				e.printStackTrace();
			}
		}

		lineNo = 0;
		return thinReport;
	}

	protected void readTlf(String file) throws IOException {
		InputStream in = new FileInputStream(new File(file));
		try {
			int size = in.available();
			byte[] buffer = new byte[size];
			in.read(buffer);
			in.close();
			// json
			String str = new String(buffer);
			JSONObject json = new JSONObject(str);
			JSONObject config = json.getJSONObject("config");
			// title
			try {
				thinReport.setTitle(config.getString("title"));
			} catch (JSONException e) {
				System.out.println("JSONObject[\"title\"] not found.");
			}
			// option
			try {
				thinReport.setOption(json2Map(config.getJSONObject("option")));
			} catch (JSONException e) {
				System.out.println("JSONObject[\"option\"] not found.");
			}
			// page
			try {
				thinReport.setPage(json2Map(config.getJSONObject("page")));
			} catch (JSONException e) {
				System.out.println("JSONObject[\"page\"] not found.");
			}
			// svg
			try {
				thinReport.setSvg(json.getString("svg")
						.replace("<!---", "&lt;!-").replace("--->", "-&gt;"));
				// thinReport.setSvg("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">"
				// + json.getString("svg").replace("<!---",
				// "&lt;!-").replace("--->", "-&gt;"));
			} catch (JSONException e) {
				System.out.println("JSONObject[\"svg\"] not found.");
			}
		} finally {
			in.close();
		}
	}

	protected Map<String, Object> json2Map(JSONObject json) {
		Iterator<String> keys = json.keys();
		Map<String, Object> map = new HashMap<String, Object>();
		while (keys.hasNext()) {
			String key = keys.next();
			Object val = json.get(key);
			map.put(key, val);
		}
		return map;
	}

	protected void traceNodes(Node node, Map<String, Object> map) {
		Node child = node.getFirstChild();
		while (child != null) {
			if (child.getNodeName().equals("#comment")
					&& child.getNodeValue() != null
					&& child.getNodeValue().length() > 5
					&& child.getNodeValue().substring(0, 5).equals("SHAPE")) {
				String str = child.getNodeValue().substring(5);
				if (str.substring(str.length() - 5, str.length()).equals(
						"SHAPE")) {
					str = str.substring(0, str.length() - 5);
					JSONObject json = new JSONObject(str);
					generateShapes(child, map, json);
				}
			}
			traceNodes(child, map);
			child = child.getNextSibling();
		}
	}

	protected void generateShapes(Node node, Map<String, Object> map,
			JSONObject json) {
		if (json.getString("type").equals("s-tblock")) {
			generateTblock(node, map, json);
		} else if (json.getString("type").equals("s-iblock")) {
			generateIblock(node, map, json);
		} else if (json.getString("type").equals("s-line")) {
			// skip
		} else if (json.getString("type").equals("s-list")) {
			generateLine(node, map, json);
		}
	}

	protected void generateTblock(Node node, Map<String, Object> map,
			JSONObject json) {
		Iterator<String> keys = json.keys();
		Document document = node.getOwnerDocument();
		Element element = document.createElement("g");
		while (keys.hasNext()) {
			String key = keys.next();
			if (key.equals("svg")) {
				JSONObject jsonsvg = json.getJSONObject("svg");
				JSONObject jsonformat = json.getJSONObject("format");
				generateTblockSvg(element, json.getString("id"), map, jsonsvg,
						jsonformat);
				JSONObject jsonattrs = jsonsvg.getJSONObject("attrs");
				Iterator<String> keysattrs = jsonattrs.keys();
				while (keysattrs.hasNext()) {
					String keyattrs = keysattrs.next();
					if (keyattrs.equals("xml:space") || keyattrs.equals("x")
							|| keyattrs.equals("y")
							|| keyattrs.equals("kerning")) {
						// skip
					} else if (keyattrs.equals("text-anchor")
							&& (jsonattrs.get(keyattrs).equals("null"))) {
						// skip
					} else {
						element.setAttribute(keyattrs,
								String.valueOf(jsonattrs.get(keyattrs)));
					}
				}
			} else if (key.equals("type")) {
				element.setAttribute("class", json.getString(key));
			} else if (key.equals("format")) {
				//
			} else if (key.equals("box")) {
				JSONObject jsonbox = json.getJSONObject("box");
				element.setAttribute("x-width",
						String.valueOf(jsonbox.get("width")));
				element.setAttribute("x-height",
						String.valueOf(jsonbox.get("height")));
				element.setAttribute("x-left", String.valueOf(jsonbox.get("x")));
				element.setAttribute("x-top", String.valueOf(jsonbox.get("y")));
				// Iterator<String> keysbox = jsonbox.keys();
				// while (keysbox.hasNext()) {
				// String keybox = keysbox.next();
				// element.setAttribute("x-" + keybox,
				// String.valueOf(jsonbox.get(keybox)));
				// }
			} else if (key.equals("desc")) {
				//
				// } else if (key.equals("display")) {
				// element.setAttribute(key, "inline");
				// } else if (key.equals("overflow")) {
				// element.setAttribute(key, "auto");
			} else {
				element.setAttribute("x-" + key, String.valueOf(json.get(key)));
			}
		}
		Node parent = node.getParentNode();
		parent.insertBefore(element, node);
	}

	protected void generateTblockSvg(Element element, String id,
			Map<String, Object> map, JSONObject json, JSONObject format) {
		Document document = element.getOwnerDocument();
		// Element e = document.createElement(json.getString("tag"));
		Element e = document.createElement("text");
		e.setAttribute("class", "s-text-l0");
		if (map.containsKey(id)) {
			if (map.get(id) instanceof String) {
				if (((String) map.get(id)).indexOf("\n") >= 0) {
					String[] strs = ((String) map.get(id)).split("\n");
					for (int i = 0; i < strs.length; i++) {
						Element el = document.createElement("tspan");
						el.setAttribute(
								"x",
								String.valueOf(json.getJSONObject("attrs").get(
										"x")));
						el.setAttribute("dy", "1em");
						el.setTextContent(strs[i]);
						e.appendChild(el);
					}
				} else {
					if (format.has("base")) {
						e.setTextContent(format.getString("base").replace(
								"{value}", "")
								+ String.valueOf(map.get(id)));
					} else {
						e.setTextContent(String.valueOf(map.get(id)));
					}
				}
			} else {
				if (format.getString("type").equals("datetime")) {
					JSONObject jsondatetime = format.getJSONObject("datetime");
					e.setTextContent(String.format(
							jsondatetime.getString("format")
									.replace("%Y", "%1$tY")
									.replace("%m", "%1$tm")
									.replace("%d", "%1$td"), map.get(id)));
				} else if (format.getString("type").equals("number")) {
					JSONObject jsonnumber = format.getJSONObject("number");
					if (format.getString("base").equals("")) {
						if (map.get(id) instanceof Double) {
							e.setTextContent(String.format(
									"%1$" + jsonnumber.getString("delimiter")
											+ jsonnumber.getInt("precision")
											+ "g", map.get(id)));
						} else {
//							e.setTextContent(String.format(
//									"%1$" + jsonnumber.getString("delimiter")
//											+ jsonnumber.getInt("precision")
//											+ "d", map.get(id)));
							e.setTextContent(String.valueOf(map.get(id)));
						}
					} else {
						if (map.get(id) instanceof Double) {
							e.setTextContent(String.format(
									format.getString("base").charAt(0) + "%1$"
											+ jsonnumber.getString("delimiter")
											+ "3g", map.get(id)));
						} else {
							e.setTextContent(String.format(
									format.getString("base").charAt(0) + "%1$"
											+ jsonnumber.getString("delimiter")
											+ "3d", map.get(id)));
						}
					}
				} else if (format.getString("type").equals("padding")) {
					JSONObject jsonnumber = format.getJSONObject("padding");
					if (map.get(id) instanceof Double) {
						e.setTextContent(String.format(
								"%1$" + jsonnumber.getString("char")
										+ jsonnumber.getInt("length") + "g",
								map.get(id)));
					} else {
						e.setTextContent(String.format(
								"%1$" + jsonnumber.getString("char")
										+ jsonnumber.getInt("length") + "d",
								map.get(id)));
					}
				} else {
					e.setTextContent(String.valueOf(map.get(id)));
				}
			}
		}
		JSONObject jsonAttrs = json.getJSONObject("attrs");
		Iterator<String> keys = jsonAttrs.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			if (key.equals("id") || key.equals("text-anchor")
					|| key.equals("xmlns") || key.equals("font-family")) {
				// skip
			} else if (key.equals("fill")) {
				e.setAttribute(key, "inherit");
				e.setAttribute("stroke", "none");
				// } else if (key.equals("class")) {
				// e.setAttribute(key, "s-text-l0");
			} else if (jsonAttrs.get(key) instanceof String) {
				e.setAttribute(key, jsonAttrs.getString(key));
			} else {
				e.setAttribute(key, String.valueOf(jsonAttrs.get(key)));
			}
		}
		element.appendChild(e);
	}

	protected void generateLine(Node node, Map<String, Object> map,
			JSONObject json) {
		String id = json.getString("id");
		// header
		if (json.has("header-enabled") && json.getBoolean("header-enabled")) {
			JSONObject jsonLine = json.getJSONObject("header");
			generateLineBlock(node, map, jsonLine, 0, getTranslateY(jsonLine));
		}
		// detail
		if (json.has("detail")) {
			JSONObject jsonLine = json.getJSONObject("detail");
			if (map.containsKey("detail")) {
				if (map.get("detail") instanceof List
						|| map.get("detail") instanceof ArrayList) {
					List<Map<String, Object>> list = (List<Map<String, Object>>) map
							.get("detail");
					for (int i = 0; i < list.size(); i++) {
						lineNo = i;
						generateLineBlock(node, list.get(i), jsonLine, i,
								getTranslateY(jsonLine));
					}
				}
			}
		}
		// footer
		if (json.has("footer-enabled") && json.getBoolean("footer-enabled")) {
			lineNo++;
			JSONObject jsonLine = json.getJSONObject("footer");
			generateLineBlock(node, map, jsonLine, lineNo,
					getTranslateY(jsonLine));
		}
		// page footer
		if (json.has("page-footer-enabled")
				&& json.getBoolean("page-footer-enabled")) {
			lineNo++;
			JSONObject jsonLine = json.getJSONObject("page-footer");
			generateLineBlock(node, map, jsonLine, lineNo,
					getTranslateY(jsonLine));
		}
	}

	protected double getTranslateY(JSONObject json) {
		double translateX = 0;
		double translateY = 0;
		if (json.has("translate")) {
			if (json.getJSONObject("translate").has("x")) {
				if (json.getJSONObject("translate").get("x") instanceof Double) {
					translateX = json.getJSONObject("translate").getDouble("x");
				} else {
					translateX = json.getJSONObject("translate").getInt("x");
				}
			}
			if (json.getJSONObject("translate").has("y")) {
				if (json.getJSONObject("translate").get("y") instanceof Double) {
					translateY = json.getJSONObject("translate").getDouble("y");
				} else {
					translateY = json.getJSONObject("translate").getInt("y");
				}
			}
		}
		return translateY;
	}

	protected void generateLineBlock(Node node, Map<String, Object> map,
			JSONObject json, int line, double translateY) {
		double height = 0;
		if (json.get("height") instanceof Integer) {
			height = (Integer) json.get("height");
		} else {
			height = (Double) json.get("height");
		}
		JSONObject svg = json.getJSONObject("svg");
		String content = "<svg xmlns=\"http://www.w3.org/2000/svg\"><g>"
				+ svg.getString("content").replace("&lt;!-", "<!--")
						.replace("-&gt;", "-->") + "</g></svg>";
		System.out.println(content);
		String parser = XMLResourceDescriptor.getXMLParserClassName();
		try {
			SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
			Document doc = f.createSVGDocument("g", new ByteArrayInputStream(
					content.getBytes("UTF-8")));
			// DocumentBuilderFactory dbfactory =
			// DocumentBuilderFactory.newInstance();
			// DocumentBuilder docbuilder = dbfactory.newDocumentBuilder();
			// Document doc = docbuilder.parse(new
			// ByteArrayInputStream(content.getBytes("UTF-8")));
			traceNodes(doc, map);
			Document document = node.getOwnerDocument();
			Element g = document.createElement("g");
			g.setAttribute("transform", "translate(0,"
					+ (height * line + translateY) + ")");
			Node newnode = document.importNode(doc.getFirstChild(), true);
			Node parent = node.getParentNode();
			parent.insertBefore(g, node);
			g.appendChild(newnode);
		} catch (IOException e) {
			e.printStackTrace();
			// } catch (ParserConfigurationException e) {
			// e.printStackTrace();
			// } catch (SAXException e) {
			// e.printStackTrace();
		}
	}

	protected void generateIblock(Node node, Map<String, Object> map,
			JSONObject json) {
		String id = json.getString("id");
		if (map.get(id) instanceof String) {
			generateIblockImage(node, id, map, json);
		} else {
			generateIblockBarcode(node, id, map, json);
		}
	}

	protected void generateIblockImage(Node node, String id,
			Map<String, Object> map, JSONObject json) {
		Document document = node.getOwnerDocument();
		Element element = document.createElement("image");
		element.setAttribute("xlink:href", String.valueOf(map.get(id)));
		JSONObject jsonSvg = json.getJSONObject("svg");
		JSONObject jsonAttrs = jsonSvg.getJSONObject("attrs");
		Iterator<String> keys = jsonAttrs.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			if (jsonAttrs.get(key) == null) {
				// skip
			} else if (jsonAttrs.get(key) instanceof String) {
				element.setAttribute(key, jsonAttrs.getString(key));
			} else {
				element.setAttribute(key, String.valueOf(jsonAttrs.get(key)));
			}
		}
		Node parent = node.getParentNode();
		parent.insertBefore(element, node);
	}

	protected void generateIblockBarcode(Node node, String id,
			Map<String, Object> map, JSONObject json) {
		Document document = node.getOwnerDocument();
		Element element = document.createElement("g");
		JSONObject jsonSvg = json.getJSONObject("svg");
		JSONObject jsonAttrs = jsonSvg.getJSONObject("attrs");
		element.setAttribute("transform",
				"translate(" + String.valueOf(jsonAttrs.get("x")) + ","
						+ String.valueOf(jsonAttrs.get("y")) + ")");
		DocumentFragment frag = (DocumentFragment) map.get(id);
		Node newnode = document.importNode(frag, true);
		element.appendChild(newnode);
		Node parent = node.getParentNode();
		parent.insertBefore(element, node);
	}

	// protected void generateIblockSvg(Element element, String id,
	// Map<String, Object> map, JSONObject json) {
	// JSONObject jsonAttrs = json.getJSONObject("attrs");
	// Document document = element.getOwnerDocument();
	// Element e = document.createElement("image");
	// e.setAttribute("xlink:href", "file:file/rails.png");
	// Iterator<String> keys = jsonAttrs.keys();
	// while (keys.hasNext()) {
	// String key = keys.next();
	// if (key.equals("id") || key.equals("text-anchor")
	// || key.equals("xmlns") || key.equals("font-family")) {
	// // skip
	// } else if (key.equals("fill")) {
	// // skip
	// // } else if (key.equals("class")) {
	// // e.setAttribute(key, "s-text-l0");
	// } else if (jsonAttrs.get(key) instanceof String) {
	// e.setAttribute(key, jsonAttrs.getString(key));
	// } else {
	// e.setAttribute(key, String.valueOf(jsonAttrs.get(key)));
	// }
	// }
	// element.appendChild(e);
	// }

	// protected void generateIblockSvg2(Element element, String id,
	// Map<String, Object> map, JSONObject json) {
	// Iterator<String> keys = json.keys();
	// Document document = element.getOwnerDocument();
	// DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();
	// try {
	// Configuration cfg = builder
	// .build(new ByteArrayInputStream(
	// "<?xml version=\"1.0\" encoding=\"UTF-8\"?><barcode><code128><module-width>0.4mm</module-width></code128></barcode>"
	// .getBytes()));
	// BarcodeGenerator gen = BarcodeUtil.getInstance()
	// .createBarcodeGenerator(cfg);
	// SVGCanvasProvider provider = new SVGCanvasProvider(false, 0);
	// gen.generateBarcode(provider, "1234567890123");
	// org.w3c.dom.DocumentFragment frag = provider.getDOMFragment();
	// Node n = document.importNode(frag, true);
	// element.appendChild(n);
	// } catch (ConfigurationException e) {
	// e.printStackTrace();
	// } catch (BarcodeException e) {
	// e.printStackTrace();
	// } catch (IOException e) {
	// e.printStackTrace();
	// } catch (SAXException e) {
	// e.printStackTrace();
	// }
	// }

	public void generate() {
		TranscoderInput[] inputs = thinReport.getDocuments();
		TransformerFactory transFactory = TransformerFactory.newInstance();
		try {
			Transformer transformer = transFactory.newTransformer();
			Source src = new DOMSource(inputs[0].getDocument());
			Result result = new StreamResult(System.out);
			transformer.transform(src, result);
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}

	public void generateXml(String file) {
		// Document document = thinReport.getDocument(0);
		TranscoderInput[] input = thinReport.getDocuments();
		TransformerFactory transFactory = TransformerFactory.newInstance();
		try {
			OutputStream out = new FileOutputStream(new File(file));
			Transformer transformer = transFactory.newTransformer();
			// Source src = new DOMSource(document);
			Source src = new StreamSource(input[0].getInputStream());
			Result result = new StreamResult(out);
			transformer.transform(src, result);
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}

	public void generate(String file) {
		MultiPagePDFTranscoder transcoder = new MultiPagePDFTranscoder();
		try {
			OutputStream out = new FileOutputStream(new File(file));
			TranscoderOutput output = new TranscoderOutput(out);
			transcoder.transcode(thinReport.getDocuments(), output);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (TranscoderException e) {
			e.printStackTrace();
		}
	}

	public void generate(String file, Map<String, Object> params) {
		MultiPagePDFTranscoder transcoder = new MultiPagePDFTranscoder();
		transcoder.setAttributes(params);
		try {
			OutputStream out = new FileOutputStream(new File(file));
			TranscoderOutput output = new TranscoderOutput(out);
			transcoder.transcode(thinReport.getDocuments(), output);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (TranscoderException e) {
			e.printStackTrace();
		}
	}

	public void generateViaFile(String file) {
		MultiPagePDFTranscoder transcoder = new MultiPagePDFTranscoder();
		try {
			OutputStream out = new FileOutputStream(new File(file));
			TranscoderOutput output = new TranscoderOutput(out);
			InputStream in = new FileInputStream(new File("test.xml"));
			TranscoderInput[] input = { new TranscoderInput(in) };
			transcoder.transcode(input, output);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (TranscoderException e) {
			e.printStackTrace();
		}
	}

}
