package com.matthalstead.fileanalyzer.cmep;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;

import com.matthalstead.fileanalyzer.cmep.CMEPRecordRegex.CMEPRecordPattern;
import com.matthalstead.fileanalyzer.cmep.CMEPRecordRegex.IdentifyingInfoPattern;
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
	
	private CMEPRecord() { }
	
	public static CMEPRecord parse(String line) {
		
		CMEPRecord result = new CMEPRecord();
		try {
			CMEPRecordPattern cmepRecordPattern = CMEPRecordRegex.getCMEPRecordPattern();
			Matcher m = cmepRecordPattern.getPattern().matcher(line);
			if (m.matches()) {

				result.recordType = internString(m.group(cmepRecordPattern.getRecordTypeIndex()));
				result.units = internString(m.group(cmepRecordPattern.getUnitsIndex()));
				result.intervalLengthCode = internString(m.group(cmepRecordPattern.getIntervalLengthCodeIndex()));
				result.listedIntervalCount = Integer.parseInt(m.group(cmepRecordPattern.getListedIntervalCountIndex()));
				
				String identifyingInfo = m.group(cmepRecordPattern.getIdentifyingInfoIndex());
				String intervalsString = m.group(cmepRecordPattern.getIntervalsStringIndex());
				IdentifyingInfoPattern identifyingInfoPattern = CMEPRecordRegex.getIdentifyingInfoPattern();
				Matcher m2 = identifyingInfoPattern.getPattern().matcher(identifyingInfo);
				if (m2.matches()) {
					result.moduleId = internString(m2.group(identifyingInfoPattern.getModuleIdIndex()));
					result.meterId = internString(m2.group(identifyingInfoPattern.getMeterIdIndex()));
					String timestampString = m2.group(identifyingInfoPattern.getTimestampIndex());
					Date date = DateUtils.parseCMEPTimestamp(timestampString);
					result.fileTimestamp = dateToLong(date);
					result.meterLocation = internString(m2.group(identifyingInfoPattern.getMeterLocationIndex()));
				} else {
					System.out.println("Identifying info did not match regex /" + identifyingInfoPattern.getPattern().pattern() + "/");
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
				result.parseException = "CMEPRecord did not match regex /" + cmepRecordPattern.getPattern().pattern() + "/";
			}
		} catch (Exception e) {
			result.parseException = "Error parsing record: " + e.getMessage();
		}

		return result;
	}
	
	
	public String getParseException() {
		return parseException;
	}
	
	private static String internString(String str) {
		if (str == null) {
			return null;
		} else {
			return str.intern();
		}
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
