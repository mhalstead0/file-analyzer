package com.matthalstead.gasami.cmep;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import com.matthalstead.fileanalyzer.cmep.CMEPRecord;
import com.matthalstead.fileanalyzer.cmep.CMEPRecord.IntervalTriplet;

public class BillingGasCSVCreator {
	
	private static final Logger log = Logger.getLogger(BillingGasCSVCreator.class);

	private final PrintWriter pw;
	private final SimpleDateFormat _outputSDF;
	private final DecimalFormat _valueDF;
	private final Object FORMAT_LOCK = new Object();
	
	private int numLinesWritten = 0;
	
	public BillingGasCSVCreator(OutputStream os) throws IOException {
		this.pw = new PrintWriter(os);
		this._outputSDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		this._outputSDF.setTimeZone(TimeZone.getTimeZone("America/New_York"));
		this._valueDF = new DecimalFormat("0.###");
		this._valueDF.setRoundingMode(RoundingMode.HALF_UP);
		writeHeader();
	}
	
	private void writeHeader() {
		writeLine(new String[] {
				"FileTimestampET",
				"MeterId",
				"Premise",
				"ServicePoint",
				"MeterPoint",
				"RegisterTimestampET",
				"Units",
				"Value",
				"Flags"
		});
	}
	
	public void handleCMEPRecord(CMEPRecord rec) {
		String units = rec.getUnits();
		if (units != null && units.endsWith("REG")) {
			handleCMEPRegRecord(rec);
		}
	}
	
	private String formatTimestamp(Date dt) {
		if (dt == null) {
			return null;
		}
		synchronized(FORMAT_LOCK) {
			return _outputSDF.format(dt);
		}
	}
	
	private String formatValue(double d) {
		synchronized(FORMAT_LOCK) {
			return _valueDF.format(d);
		}
	}
	
	private void handleCMEPRegRecord(CMEPRecord rec) {
		List<IntervalTriplet> triplets = rec.getTriplets();
		if (triplets == null) {
			triplets = new ArrayList<IntervalTriplet>(0);
		}
		if (triplets.size() != 2) {
			log.warn("Register record found (meter " + rec.getMeterId() + ") with an invalid number of triplets (" + triplets.size() + ")");
		} else {
			IntervalTriplet t = triplets.get(1);
			String ccfValue = getCCFValue(rec.getUnits(), t.getValueString());
			if (ccfValue == null) {
				log.warn("For meter " + rec.getMeterId() + ", could not parse CCF from " + t.getValueString() + " in units " + rec.getUnits());
			} else {
				String[] podSplit = parsePOD(rec);
				String[] fields = {
						formatTimestamp(rec.getFileTimestamp()),
						rec.getMeterId(),
						podSplit[0],
						podSplit[1],
						podSplit[2],
						formatTimestamp(t.getTimestamp()),
						"CCF",
						ccfValue,
						t.getDataQualityLetter() + t.getDataQualityFlags()
				};
				writeLine(fields);
			}
		}
	}
	
	private String getCCFValue(String units, String inputValueString) {
		units = (units == null) ? "" : units.trim().toUpperCase();
		inputValueString = (inputValueString == null) ? "" : inputValueString.trim().toUpperCase();
		if (units.length() == 0 || inputValueString.length() == 0) {
			return null;
		} else {
			if (units.endsWith("REG")) {
				units = units.substring(0, units.length() - "REG".length());
			}
			double factor;
			if (units.equals("CCF")) {
				factor = 1.0;
			} else if (units.equals("CF")) {
				factor = 0.01;
			} else if (units.equals("MCF")) {
				factor = 10.0;
			} else if (units.equals("DCF")) {
				factor = 0.1;
			} else {
				return null;
			}
			try {
				double inputValue = Double.parseDouble(inputValueString);
				double outputValue = inputValue * factor;
				return formatValue(outputValue);
			} catch (Exception e) {
				log.warn(e);
				return null;
			}
		}
	}
	
	private String[] parsePOD(CMEPRecord rec) {
		String[] result = new String[3];
		String pod = rec.getMeterLocation();
		if (pod != null && !pod.trim().equalsIgnoreCase("null")) {
			pod = pod.trim();
			String[] split = pod.split("_");
			int minLength = Math.min(result.length, split.length);
			System.arraycopy(split, 0, result, 0, minLength);
		}
		return result;
	}
	
	private void writeLine(String[] fields) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<fields.length; i++) {
			if (i > 0) {
				sb.append(",");
			}
			if (fields[i] != null) {
				sb.append(fields[i]);
			}
		}
		pw.println(sb);
		numLinesWritten++;
	}
	
	public int getNumLinesWritten() {
		return numLinesWritten;
	}
	
	public void finish() {
		pw.flush();
		pw.close();
	}
	
	
	
}
