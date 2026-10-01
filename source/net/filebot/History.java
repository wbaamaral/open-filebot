package net.filebot;

import static java.util.Collections.*;
import static net.filebot.Logging.*;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class History {

	private List<Sequence> sequences;

	public History() {
		this.sequences = new ArrayList<Sequence>();
	}

	public History(Collection<Sequence> sequences) {
		this.sequences = new ArrayList<Sequence>(sequences);
	}

	public static class Sequence {

		private Date date;
		private List<Element> elements;

		private Sequence() {
			// hide constructor
		}

		public Date date() {
			return date;
		}

		public List<Element> elements() {
			if (elements == null)
				return emptyList();

			return unmodifiableList(elements);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Sequence) {
				Sequence other = (Sequence) obj;
				return date.equals(other.date) && elements.equals(other.elements);
			}

			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hash(elements, date);
		}
	}

	public static class Element {

		private File dir;
		private String from;
		private String to;

		public Element() {
		}

		public Element(String from, String to, File dir) {
			this.from = from;
			this.to = to;
			this.dir = dir;
		}

		public File dir() {
			return dir;
		}

		public String from() {
			return from;
		}

		public String to() {
			return to;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Element) {
				Element element = (Element) obj;
				return to.equals(element.to) && from.equals(element.from) && dir.getPath().equals(element.dir.getPath());
			}

			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hash(to, from, dir);
		}
	}

	public List<Sequence> sequences() {
		return unmodifiableList(sequences);
	}

	public void add(Collection<Element> elements) {
		Sequence sequence = new Sequence();
		sequence.date = new Date();
		sequence.elements = new ArrayList<Element>(elements);

		add(sequence);
	}

	public void add(Sequence sequence) {
		this.sequences.add(sequence);
	}

	public void addAll(Collection<Sequence> sequences) {
		this.sequences.addAll(sequences);
	}

	public void merge(History history) {
		for (Sequence sequence : history.sequences()) {
			if (!sequences.contains(sequence)) {
				add(sequence);
			}
		}
	}

	public int totalSize() {
		int i = 0;
		for (Sequence it : sequences()) {
			i += it.elements.size();
		}
		return i;
	}

	public void clear() {
		sequences.clear();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof History) {
			History other = (History) obj;
			return sequences.equals(other.sequences);
		}

		return false;
	}

	@Override
	public int hashCode() {
		return sequences.hashCode();
	}

	public Map<File, File> getRenameMap() {
		Map<File, File> map = new LinkedHashMap<File, File>();
		for (History.Sequence seq : this.sequences()) {
			for (History.Element elem : seq.elements()) {
				File to = new File(elem.to());
				if (!to.isAbsolute()) {
					to = new File(elem.dir(), elem.to());
				}
				File from = new File(elem.dir(), elem.from());
				map.put(from, to);
			}
		}
		return map;
	}

	public static void exportHistory(History history, OutputStream output) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document doc = factory.newDocumentBuilder().newDocument();
			org.w3c.dom.Element root = doc.createElement("history");
			doc.appendChild(root);

			for (Sequence seq : history.sequences()) {
				org.w3c.dom.Element seqElem = doc.createElement("sequence");
				if (seq.date() != null) {
					seqElem.setAttribute("date", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(seq.date().toInstant().atZone(ZoneId.systemDefault())));
				}
				for (Element elem : seq.elements()) {
					org.w3c.dom.Element renElem = doc.createElement("rename");
					if (elem.dir() != null) {
						renElem.setAttribute("dir", elem.dir().getPath());
					}
					if (elem.from() != null) {
						renElem.setAttribute("from", elem.from());
					}
					if (elem.to() != null) {
						renElem.setAttribute("to", elem.to());
					}
					seqElem.appendChild(renElem);
				}
				root.appendChild(seqElem);
			}

			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transformer.transform(new DOMSource(doc), new StreamResult(output));
		} catch (Exception e) {
			debug.log(Level.SEVERE, "Failed to write history", e);
		}
	}

	public static History importHistory(InputStream stream) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			try {
				factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
				factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
				factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			} catch (Exception ignored) {
			}
			Document doc = factory.newDocumentBuilder().parse(stream);
			org.w3c.dom.Element root = doc.getDocumentElement();

			History history = new History();
			NodeList seqNodes = root.getElementsByTagName("sequence");
			for (int i = 0; i < seqNodes.getLength(); i++) {
				Node node = seqNodes.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					org.w3c.dom.Element seqElem = (org.w3c.dom.Element) node;
					String dateAttr = seqElem.getAttribute("date");
					Date date = parseDate(dateAttr);

					List<Element> elements = new ArrayList<Element>();
					NodeList renNodes = seqElem.getElementsByTagName("rename");
					for (int j = 0; j < renNodes.getLength(); j++) {
						Node rNode = renNodes.item(j);
						if (rNode.getNodeType() == Node.ELEMENT_NODE) {
							org.w3c.dom.Element renElem = (org.w3c.dom.Element) rNode;
							String dir = renElem.getAttribute("dir");
							String from = renElem.getAttribute("from");
							String to = renElem.getAttribute("to");
							elements.add(new Element(from, to, new File(dir)));
						}
					}
					Sequence sequence = new Sequence();
					sequence.date = date;
					sequence.elements = elements;
					history.add(sequence);
				}
			}
			return history;
		} catch (Exception e) {
			debug.log(Level.SEVERE, "Failed to read history", e);
		}

		// default to empty history
		return new History();
	}

	private static Date parseDate(String text) {
		if (text == null || text.trim().isEmpty()) {
			return new Date();
		}
		try {
			return Date.from(Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(text)));
		} catch (Exception e1) {
			try {
				return DatatypeFactory.newInstance().newXMLGregorianCalendar(text).toGregorianCalendar().getTime();
			} catch (Exception e2) {
				try {
					return new Date(Long.parseLong(text));
				} catch (Exception e3) {
					return new Date();
				}
			}
		}
	}

}
