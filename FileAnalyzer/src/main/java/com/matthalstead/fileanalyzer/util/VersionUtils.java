package com.matthalstead.fileanalyzer.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;

import javax.xml.bind.JAXB;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;


public class VersionUtils {

	
	public static Version getVersion() {
		try {
			InputStream is = VersionUtils.class.getClassLoader().getResourceAsStream("fileanalyzerversion.xml");
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(is);
			XPathFactory xpf = XPathFactory.newInstance();
			XPath xp = xpf.newXPath();
			final String rev = xp.evaluate("//entry/commit/@revision", doc);
			final String tms = xp.evaluate("//entry/commit/date", doc);
			System.out.println(tms);
			
			String xml = "<version>" +
					"<revision>" + rev + "</revision>" +
					"<revisionTimestamp>" + tms + "</revisionTimestamp>" +
					"</version>";
			
			Version v = JAXB.unmarshal(new ByteArrayInputStream(xml.getBytes()), VersionImpl.class);
			return v;
		} catch (Exception e) {
			return null;
		}
	}
	
	public static interface Version {
		String getRevision();
		Date getRevisionTimestamp();
	}
	
	private static class VersionImpl implements Version {
		private String revision;
		private Date revisionTimestamp;
		public String getRevision() {
			return revision;
		}
		public Date getRevisionTimestamp() {
			return revisionTimestamp;
		}
	}
}
