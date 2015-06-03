package com.github.naofum.thinreports;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.batik.transcoder.TranscoderInput;
import org.w3c.dom.Document;

public class ThinReport {

	private String title;
	private Map<String, Object> option;
	private Map<String, Object> page;
	private String svg;
	private List<TranscoderInput> document = new ArrayList<TranscoderInput>();

	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public Map<String, Object> getOption() {
		return option;
	}
	public void setOption(Map<String, Object> option) {
		this.option = option;
	}
	public Map<String, Object> getPage() {
		return page;
	}
	public void setPage(Map<String, Object> page) {
		this.page = page;
	}
	public String getSvg() {
		return svg;
	}
	public void setSvg(String svg) {
		this.svg = svg;
	}
	public List<TranscoderInput> get() {
		return document;
	}
	public void setDocument(List<TranscoderInput> document) {
		this.document = document;
	}
	public TranscoderInput getDocument(int i) {
		return document.get(i);
	}
	public TranscoderInput[] getDocuments() {
		return document.toArray(new TranscoderInput[document.size()]);
	}
	public void addDocument(TranscoderInput document) {
		this.document.add(document);
	}
	
}
