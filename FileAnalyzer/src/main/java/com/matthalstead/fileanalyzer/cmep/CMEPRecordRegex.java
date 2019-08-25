package com.matthalstead.fileanalyzer.cmep;

import java.util.regex.Pattern;

/**
 * 
 * @author Matt
 *
 */
class CMEPRecordRegex {
	
	private static CMEPRecordPattern cmepRecordPattern = null;
	private static IdentifyingInfoPattern identifyingInfoPattern = null;
	
	private CMEPRecordRegex() { }
	
	public static CMEPRecordPattern getCMEPRecordPattern() {
		CMEPRecordPattern result = cmepRecordPattern;
		if (result == null) {
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
			result = new CMEPRecordPattern();
			result.pattern = Pattern.compile(recordRegex);
			cmepRecordPattern = result;
		}
		return result;
	}
	
	/**
	 * 
	 * @author Matt
	 *
	 */
	public static class CMEPRecordPattern {
		private Pattern pattern;
		
		public Pattern getPattern() {
			return pattern;
		}
		
		public int getRecordTypeIndex() {
			return 1;
		}
		
		public int getUnitsIndex() {
			return 12;
		}
		
		public int getIntervalLengthCodeIndex() {
			return 16;
		}
		
		public int getListedIntervalCountIndex() {
			return 17;
		}
		
		public int getIdentifyingInfoIndex() {
			return 9;
		}
		
		public int getIntervalsStringIndex() {
			return 18;
		}
		
	}
	
	
	public static IdentifyingInfoPattern getIdentifyingInfoPattern() {
		IdentifyingInfoPattern result = identifyingInfoPattern;
		if (result == null) {
			String yearRegex = "[12][0-9]{3}";
			String monthRegex = "[01][0-9]";
			String dayRegex = "[0123][0-9]";
			String hourRegex = "[012][0-9]";
			String minuteRegex = "[0-5][0-9]";
			
			String timestampRegex = yearRegex + monthRegex + dayRegex + hourRegex + minuteRegex;
			String identifyingInfoRegex = "([^,]*),(.*),(" + timestampRegex + "),(.*)";
			
			result = new IdentifyingInfoPattern();
			result.pattern = Pattern.compile(identifyingInfoRegex);
			identifyingInfoPattern = result;
		}
		return identifyingInfoPattern;
	}
	

	public static class IdentifyingInfoPattern {
		private Pattern pattern;
		
		public Pattern getPattern() {
			return pattern;
		}
		
		public int getModuleIdIndex() {
			return 1;
		}
		
		public int getMeterIdIndex() {
			return 2;
		}
		
		public int getTimestampIndex() {
			return 3;
		}
		
		public int getMeterLocationIndex() {
			return 4;
		}
	}
}
