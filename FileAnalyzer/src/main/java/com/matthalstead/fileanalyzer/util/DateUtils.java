package com.matthalstead.fileanalyzer.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

public class DateUtils {

	private static final SimpleDateFormat cmepTimestampSDF = new SimpleDateFormat("yyyyMMddHHmm");
	private static final Object CMEP_SDF_LOCK = new Object();
	static {
		cmepTimestampSDF.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	private static Date doParseCMEPTimestamp(String str) {
		if (str == null) {
			return null;
		}
		try {
			synchronized(CMEP_SDF_LOCK) {
				return cmepTimestampSDF.parse(str);
			}
		} catch (ParseException e) {
			throw new RuntimeException("Could not parse CMEP timestamp \"" + str + "\"", e);
		}
	}
	private static final HashMap<String, Long> cmepTimestampMap = new HashMap<String, Long>();
	private static int cmepTimestampMapPutCount = 0;
	public static Date parseCMEPTimestamp(String str) {
		if (str == null) {
			return null;
		}
		synchronized(CMEP_SDF_LOCK) {
			Long l = cmepTimestampMap.get(str);
			Date result;
			if (l == null) {
				result = doParseCMEPTimestamp(str);
				if (result != null) {
					if ((++cmepTimestampMapPutCount) % 10000 == 0) {
						if (cmepTimestampMap.size() > 10000) {
							cmepTimestampMap.clear();
						}
					}
					cmepTimestampMap.put(str, Long.valueOf(result.getTime()));
				}
				return result;
			} else {
				return new Date(l.longValue());
			}
		}
	}
	
	public static String formatCMEPTimestamp(Date dt) {
		if (dt == null) {
			return null;
		}
		synchronized(CMEP_SDF_LOCK) {
			return cmepTimestampSDF.format(dt);
		}
	}
	
}
