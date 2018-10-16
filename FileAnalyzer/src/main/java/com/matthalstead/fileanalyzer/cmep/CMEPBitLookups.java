package com.matthalstead.fileanalyzer.cmep;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class CMEPBitLookups {

	private static final Logger log = Logger.getLogger(CMEPBitLookups.class);
	
	private static Map<Integer, String> intervalFlagMap = null;
	
	private static Map<Integer, String> buildMap(String filename) {
		Map<Integer, String> result = new HashMap<Integer, String>();
		try {
			String dir = CMEPBitLookups.class.getPackage().getName().replaceAll("\\.", "/");
			String path = dir + "/" + filename;
			InputStream is = CMEPBitLookups.class.getClassLoader().getResourceAsStream(path);
			if (is == null) {
				throw new NullPointerException("Could not load resource " + path);
			}
			log.debug("Loading definitions from " + filename + "...");
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line = br.readLine(); // header
			while ((line = br.readLine()) != null) {
				String[] split = line.split(",");
				if (split.length >= 2) {
					Integer key = Integer.parseInt(split[0]);
					String val = split[1];
					result.put(key, val);
				}
			}
		} catch (Exception e) {
			log.error("Exception reading file \"" + filename + "\"", e);
		}
		return result;
	}
	
	private static void ensureIntervalFlagMapExists() {
		if (intervalFlagMap == null) {
			intervalFlagMap = buildMap("ReadingFlags.csv");
		}
	}
	
	public static String getIntervalFlagDescription(int bitIndex) {
		ensureIntervalFlagMapExists();
		String result = intervalFlagMap.get(bitIndex);
		return (result == null) ? "Unknown" : result;
	}
}
