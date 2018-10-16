package com.matthalstead.fileanalyzer.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;

public class StreamUtils {
	
	private static final Logger log = Logger.getLogger(StreamUtils.class);

	public static InputStream getInputStream(File f) throws IOException {
		log.debug("Opening file " + f.getAbsolutePath() + "...");
		FileInputStream fis_MagicNumberTest = new FileInputStream(f);
		byte[] firstTwoBytes = new byte[2];
		int read = fis_MagicNumberTest.read(firstTwoBytes);
		if (read == 0) {
			log.debug("File " + f.getAbsolutePath() + " appears to be empty");
			fis_MagicNumberTest.close();
			return new FileInputStream(f);
		} else if (read == 1) {
			read = fis_MagicNumberTest.read(firstTwoBytes, 1, 1);
			if (read == 0) {
				log.debug("File " + f.getAbsolutePath() + " appears to only have 1 byte of content");
				fis_MagicNumberTest.close();
				return new FileInputStream(f);
			}
		}
		fis_MagicNumberTest.close();

		byte b0 = firstTwoBytes[0];
		byte b1 = firstTwoBytes[1];
		log.debug("First two bytes of " + f.getAbsolutePath() + ": " + getByteAsHex(b0) + " " + getByteAsHex(b1));
		
		// firstTwoBytes populated
		FileInputStream fis = new FileInputStream(f);
		if (b0 == (byte) 0x1f && b1 == (byte) 0x8b) {
			log.debug("File " + f.getAbsolutePath() + " is in gzip format");
			return new GZIPInputStream(fis);
		} else {
			log.debug("File " + f.getAbsolutePath() + " is NOT in gzip format");
			return fis;
		}
	}
	
	private static String getByteAsHex(byte b) {
		int i = ((int) b) & 0x00FF;
		String str = Integer.toHexString(i).toUpperCase();
		if (str.length() == 1) {
			return "0" + str;
		} else {
			return str;
		}
	}
}
