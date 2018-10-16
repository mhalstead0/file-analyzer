package com.matthalstead.fileanalyzer.util;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XMLUtils {
	
	public static void simpleParse(InputStream is, final SimpleParseReceiver spr) throws Exception {
		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		SAXParser parser = parserFactory.newSAXParser();
		parser.parse(is, new DefaultHandler() {
			final LinkedList<String> ancestors = new LinkedList<String>();
			
			final StringBuilder charsSinceTag = new StringBuilder();
			
			boolean previousTagWasStart = false;
			
			@Override
			public void startDocument() throws SAXException {
				spr.startDocument();
			}
			
			@Override
			public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
				String name = qName;
				Map<String, String> attrs = attributesToStringMap(attributes);
				spr.startElement(name, Collections.unmodifiableList(ancestors), attrs);
				ancestors.addLast(name);
				charsSinceTag.setLength(0);
				previousTagWasStart = true;
			}
			
			
			
			@Override
			public void characters(char[] ch, int start, int length) throws SAXException {
				charsSinceTag.append(new String(ch, start, length));
			}
			
			@Override
			public void endElement(String uri, String localName, String qName) throws SAXException {
				ancestors.removeLast();
				List<String> ancestorNames = Collections.unmodifiableList(ancestors);
				
				String name = qName;
				
				if (previousTagWasStart) {
					spr.simpleElement(name, ancestorNames, charsSinceTag.toString());
				}
				
				spr.endElement(name, ancestorNames);

				charsSinceTag.setLength(0);
				previousTagWasStart = false;
			}
			
			@Override
			public void endDocument() throws SAXException {
				spr.endDocument();
			}
			
			private Map<String, String> attributesToStringMap(Attributes attributes) {
				Map<String, String> result = new HashMap<String, String>();
				for (int i=0; i<attributes.getLength(); i++) {
					result.put(attributes.getLocalName(i), attributes.getValue(i));
				}
				return result;
			}
		});
	}

	public static interface SimpleParseReceiver {
		public void startDocument();
		public void startElement(String name, List<String> ancestorNames, Map<String, String> attributes);
		public void simpleElement(String name, List<String> ancestorNames, String content);
		public void endElement(String name, List<String> ancestorNames);
		public void endDocument();
	}
	public static class DefaultSimpleParseReceiver implements SimpleParseReceiver {
		public void startDocument() {}
		public void startElement(String name, List<String> ancestorNames, Map<String, String> attributes) {}
		public void simpleElement(String name, List<String> ancestorNames, String content) {}
		public void endElement(String name, List<String> ancestorNames) {}
		public void endDocument() {}
		protected static boolean listEndsWith(List<String> list, String ... tail) {
			if (tail == null || tail.length == 0) {
				return true;
			} else if (tail.length > list.size()) {
				return false;
			} else {
				for (int i=0; i<tail.length; i++) {
					String s1 = list.get((list.size() - 1) - i);
					String s2 = tail[(tail.length - 1) - i];
					if (!areEqual(s1, s2)) {
						return false;
					}
				}
				return true;
			}
		}
		private static boolean areEqual(String s1, String s2) {
			if (s1 == null) {
				return s2 == null;
			} else {
				return s1.equals(s2);
			}
		}
	}
}
