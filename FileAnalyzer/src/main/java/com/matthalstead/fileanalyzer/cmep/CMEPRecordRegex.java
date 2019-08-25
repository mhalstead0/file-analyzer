package com.matthalstead.fileanalyzer.cmep;

import java.util.regex.Pattern;

/**
 * 
 * @author Matt
 *
 */
class CMEPRecordRegex {
	
	private static Pattern cmepRecordPattern = null;
	private static Pattern identifyingInfoPattern = null;
	
	private CMEPRecordRegex() { }
	
	public static Pattern getCMEPRecordPattern() {
		if (cmepRecordPattern == null) {
			String oneFieldRegex = "([^,]*)(,|\\Z)";
			String okFieldRegex = "OK,";
			String digitsFieldRegex = "([0-9]*),";
			
			String yearRegex = "[12][0-9]{3}";
			String monthRegex = "[01][0-9]";
			String dayRegex = "[0123][0-9]";
			String hourRegex = "[012][0-9]";
			String minuteRegex = "[0-5][0-9]";
			
			String timestampRegex = yearRegex + monthRegex + dayRegex + hourRegex + minuteRegex;
			
			String dqfRegex = "[A-Z][0-9]+";
			String intervalValueRegex = "-?([0-9]+)|([0-9]*\\.[0-9]+)";
			String intervalRegex = "(" + timestampRegex + "),(" + dqfRegex + "),(" + intervalValueRegex + ")(,|\\Z)";
			String recordRegex =
					oneFieldRegex // recordType
					+ oneFieldRegex // formatDate
					+ oneFieldRegex // vendor
					+ oneFieldRegex // utility
					+ "(.*)," // meter-identifying info
					+ okFieldRegex
					+ oneFieldRegex // meter type
					+ oneFieldRegex // units
					+ oneFieldRegex // meter multiplier
					+ digitsFieldRegex // interval length code
					+ digitsFieldRegex // listed interval count
					+ "((" + intervalRegex + ")*)"; // intervals
			cmepRecordPattern = Pattern.compile(recordRegex);
		}
		return cmepRecordPattern;
	}
	
	
	public static Pattern getIdentifyingInfoPattern() {
		if (identifyingInfoPattern == null) {
			String yearRegex = "[12][0-9]{3}";
			String monthRegex = "[01][0-9]";
			String dayRegex = "[0123][0-9]";
			String hourRegex = "[012][0-9]";
			String minuteRegex = "[0-5][0-9]";
			
			String timestampRegex = yearRegex + monthRegex + dayRegex + hourRegex + minuteRegex;
			String identifyingInfoRegex = "([^,]*),(.*),(" + timestampRegex + "),(.*)";
			identifyingInfoPattern = Pattern.compile(identifyingInfoRegex);
		}
		return identifyingInfoPattern;
		
	}
}
