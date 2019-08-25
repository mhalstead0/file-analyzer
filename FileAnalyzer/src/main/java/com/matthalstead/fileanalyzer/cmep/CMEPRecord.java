package com.matthalstead.fileanalyzer.cmep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.matthalstead.fileanalyzer.util.DateUtils;

public class CMEPRecord {

	private String recordType;
	//private String versionDate;
	//private String vendor;
	//private String utility;
	private String moduleId;
	private String meterId;
	private Long fileTimestamp;
	private String meterLocation;
	//private String statusCode;
	//private String meterType;
	private String units;
	//private String meterMultiplier;
	private String intervalLengthCode;
	private int listedIntervalCount;
	private List<IntervalTriplet> triplets;
	
	private String parseException;
	
	private CMEPRecord() {
		
	}
	
	public CMEPRecord(String[] fields) {
		try {
			recordType = internString(safeGetString(fields, 0));
			moduleId = safeGetString(fields, 4);
			meterId = safeGetString(fields, 5);
			fileTimestamp = dateToLong(DateUtils.parseCMEPTimestamp(safeGetString(fields, 6)));
			meterLocation = safeGetString(fields, 7);
			units = internString(safeGetString(fields, 10));
			intervalLengthCode = internString(safeGetString(fields, 12));
			listedIntervalCount = Integer.parseInt(safeGetString(fields, 13));
			List<IntervalTriplet> triplets = new ArrayList<IntervalTriplet>(0);
			for (int i=14; i<fields.length; i+=3) {
				triplets.add(new IntervalTriplet(fields, i));
			}
			this.triplets = Collections.unmodifiableList(triplets);
		} catch (Exception e) {
			parseException = e.getMessage();
		}
	}
	
	public static CMEPRecord parse(String line) {
		//MEPMD01,20080501,VENDOR,UTIL:153000,1234567,9876543,201907091845,,9988776,OK,G,CCFREG,1.0,00000100,2,201907080500,R0,2073,201907090500,R0,2074
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
		
		CMEPRecord result = new CMEPRecord();
		try {
			Pattern pattern = Pattern.compile(recordRegex);
			Matcher m = pattern.matcher(line);
			if (m.matches()) {
				String identifyingInfo = m.group(9);
				String intervalsString = m.group(18);

				System.out.println("identifyingInfo=" + identifyingInfo);
				System.out.println("intervalsString=" + intervalsString);

				result.recordType = m.group(1);
				result.units = m.group(12);
				result.intervalLengthCode = m.group(16);
				result.listedIntervalCount = Integer.parseInt(m.group(17));
				
				String identifyingInfoRegex = "([^,]*),(.*),(" + timestampRegex + "),(.*)";
				Pattern identifyingInfoPattern = Pattern.compile(identifyingInfoRegex);
				Matcher m2 = identifyingInfoPattern.matcher(identifyingInfo);
				if (m2.matches()) {
					result.moduleId = m2.group(1);
					result.meterId = m2.group(2);
					String timestampString = m2.group(3);
					Date date = DateUtils.parseCMEPTimestamp(timestampString);
					result.fileTimestamp = dateToLong(date);
					result.meterLocation = m2.group(4);
					
				} else {
					System.out.println("Identifying info did not match regex /" + identifyingInfoRegex + "/");
					result.parseException = "Could not parse identifying info";
				}
				
				String[] intervalFields = intervalsString.split(",");
				List<IntervalTriplet> intervalTriplets = new ArrayList<>(intervalFields.length / 3);
				for (int i=0; i<intervalFields.length; i+=3) {
					IntervalTriplet intervalTriplet = new IntervalTriplet(intervalFields, i);
					intervalTriplets.add(intervalTriplet);
				}
				result.triplets = intervalTriplets;
				
			} else {
				result.parseException = "CMEPRecord did not match regex /" + recordRegex + "/";
			}
		} catch (Exception e) {
			result.parseException = "Error parsing record: " + e.getMessage();
		}

		return result;
	}
	
	
	public String getParseException() {
		return parseException;
	}
	
	private static String safeGetString(String[] fields, int index) {
		String str = (index < 0 || index >= fields.length) ? null : fields[index];
		if (str != null) {
			str = str.trim();
			if (str.length() == 0) {
				str = null;
			}
		}
		return str;
	}
	private static String internString(String str) {
		return (str == null) ? str : str.intern();
	}
	private static Date longToDate(Long l) {
		return (l == null) ? null : new Date(l.longValue());
	}
	private static Long dateToLong(Date d) {
		return (d == null) ? null : new Long(d.getTime());
	}
	
	public Date getFileTimestamp() {
		return longToDate(fileTimestamp);
	}

	public String getIntervalLengthCode() {
		return intervalLengthCode;
	}
	
	public int getIntervalLengthMinutes() {
		try {
			if (intervalLengthCode.length() != 8) {
				throw new Exception("Invalid length (" + intervalLengthCode.length() + ")");
			}
			int mins = Integer.parseInt(intervalLengthCode.substring(6, 8));
			int hours = Integer.parseInt(intervalLengthCode.substring(4, 6));
			int days = Integer.parseInt(intervalLengthCode.substring(2, 4));
			int other = Integer.parseInt(intervalLengthCode.substring(0, 2));
			if (other != 0) {
				throw new Exception("Something to the left of the hours was set");
			} else {
				int result = (((days * 24) + hours) * 60) + mins;
				if (result == 0) {
					throw new Exception("Interval length of zero");
				}
				return result;
			}
		} catch (Exception e) {
			throw new RuntimeException("Could not parse interval length code \"" + intervalLengthCode + "\"", e);
		}
	}

	public int getListedIntervalCount() {
		return listedIntervalCount;
	}

	public String getMeterId() {
		return meterId;
	}

	public String getMeterLocation() {
		return meterLocation;
	}

	public String getModuleId() {
		return moduleId;
	}

	public String getRecordType() {
		return recordType;
	}

	public List<IntervalTriplet> getTriplets() {
		return triplets;
	}

	public String getUnits() {
		return units;
	}

	public static class IntervalTriplet {
		private Long timestamp;
		private String dataQualityLetter;
		private long dataQualityFlags;
		private String valueString;
		
		public IntervalTriplet(Date ts, String dq, String val) {
			this((ts == null) ? null : Long.valueOf(ts.getTime()), dq, val);
		}
		
		public IntervalTriplet(Long ts, String dq, String val) {
			this.timestamp = ts;
			dq = (dq == null) ? "" : dq.trim();
			int dqLen = dq.length();
			if (dqLen == 0) {
				dataQualityLetter = null;
				dataQualityFlags = 0L;
			} else if (dqLen == 1) {
				dataQualityLetter = dq.intern();
				dataQualityFlags = 0L;
			} else {
				dataQualityLetter = dq.substring(0, 1).intern();
				dataQualityFlags = Long.parseLong(dq.substring(1));
			}
			this.valueString = val.intern();
		}
		
		public IntervalTriplet(String ts, String dq, String val) {
			this(DateUtils.parseCMEPTimestamp(ts), dq, val);
		}
		
		

		public IntervalTriplet(String[] fields, int offset) {
			this((offset + 0 >= fields.length) ? null : fields[offset + 0],
					(offset + 1 >= fields.length) ? null : fields[offset + 1],
					(offset + 2 >= fields.length) ? null : fields[offset + 2]);
		}

		public long getDataQualityFlags() {
			return dataQualityFlags;
		}

		public String getDataQualityLetter() {
			return dataQualityLetter;
		}
		public Date getTimestamp() {
			return longToDate(timestamp);
		}

		public String getValueString() {
			return valueString;
		}

	}

}
