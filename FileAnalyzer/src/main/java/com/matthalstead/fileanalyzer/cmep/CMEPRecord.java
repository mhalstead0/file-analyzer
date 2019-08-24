package com.matthalstead.fileanalyzer.cmep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
		String[] fields = line.split(",");
		CMEPRecord result = new CMEPRecord(fields);
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
