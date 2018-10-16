package com.matthalstead.fileanalyzer.cmep.consumers;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.matthalstead.fileanalyzer.cmep.CMEPRecord;
import com.matthalstead.fileanalyzer.cmep.CMEPRecord.IntervalTriplet;
import com.matthalstead.fileanalyzer.cmep.CMEPRecordSet;
import com.matthalstead.fileanalyzer.util.DateUtils;

public class CMEPDataIssuesConsumer extends AbstractMeterGroupCMEPConsumer {

	
	private final DistinctStringCounter multipleRegRecordMeters = new DistinctStringCounter();
	private final DistinctStringCounter regDropMeters = new DistinctStringCounter();
	private final DistinctStringCounter negativeIntervalMeters = new DistinctStringCounter();
	private final DistinctStringCounter highIntervalMeters = new DistinctStringCounter();
	private final DistinctStringCounter misalignedTimestampMeters = new DistinctStringCounter();
	private final DistinctStringCounter sumCheckMeters = new DistinctStringCounter();
	private final DistinctStringCounter intervalsExceedRegisterDiffMeters = new DistinctStringCounter();
	private final DistinctStringCounter sameRegistersMeters = new DistinctStringCounter();
	
	private final List<DistinctStringCounter> counters = Arrays.asList(new DistinctStringCounter[] {
			multipleRegRecordMeters,
			regDropMeters,
			negativeIntervalMeters,
			highIntervalMeters,
			misalignedTimestampMeters,
			sumCheckMeters,
			intervalsExceedRegisterDiffMeters,
			sameRegistersMeters
	});
	
	private int numDecimalDigits(double d) {
		long l = (long) d;
		int count = 0;
		while (l != 0) {
			l /= 10;
			count++;
		}
		return count;
	}
	
	private double calculateRegisterDifference(double startRegVal, double stopRegVal) {
		if (stopRegVal >= startRegVal) {
			return stopRegVal - startRegVal;
		} else {
			int startNumDigits = numDecimalDigits(startRegVal);
			double maxAtThisDigitSize = Math.pow(10, startNumDigits);
			if (startRegVal - stopRegVal > maxAtThisDigitSize / 2.0) {
				return (stopRegVal + maxAtThisDigitSize) - startRegVal;
			} else {
				return stopRegVal - startRegVal;
			}
		}
	}
	
	
	@Override
	protected void recordSetReceived(CMEPRecordSet recordSet) {
		CMEPRecord sampleRecord = recordSet.getFirstRecordWithNoParseException();
		if (sampleRecord != null) {
			String meterId = sampleRecord.getMeterId();
			int regRecordCount = countMultipleRegisterRecords(recordSet);
			if (regRecordCount > 1) {
				multipleRegRecordMeters.addObject(meterId);
			} else if (regRecordCount == 1) {
				CMEPRecord regRecord = getFirstRegisterRecord(recordSet);
				List<IntervalTriplet> regTriplets = regRecord.getTriplets();
				if (regTriplets != null && regTriplets.size() == 2) {
					IntervalTriplet startRegTriplet = regTriplets.get(0);
					IntervalTriplet stopRegTriplet = regTriplets.get(1);
					if (startRegTriplet.getTimestamp().equals(stopRegTriplet.getTimestamp())) {
						sameRegistersMeters.addObject(meterId);
					}
					
					Date startRegTimestamp = startRegTriplet.getTimestamp();
					Date stopRegTimestamp = stopRegTriplet.getTimestamp();
					double startRegVal = getValue(startRegTriplet);
					double stopRegVal = getValue(stopRegTriplet);
					int intervalLengthMinutes = regRecord.getIntervalLengthMinutes();
					double regDifference = calculateRegisterDifference(startRegVal, stopRegVal);
					if (regDifference < 0) {
						regDropMeters.addObject(meterId);
					}
					
					List<IntervalTriplet> intTriplets = new ArrayList<IntervalTriplet>();
					for (int i=0; i<recordSet.getRecordCount(); i++) {
						CMEPRecord rec = recordSet.getRecord(i);
						if (rec != null && !isRegisterRecord(rec)) {
							if (rec.getTriplets() != null) {
								intTriplets.addAll(rec.getTriplets());
							}
							
						}
					}
					
					for (IntervalTriplet t : intTriplets) {
						//if ("R".equals(t.getDataQualityLetter())) {
							double val = getValue(t);
							if (val < 0.0) {
								negativeIntervalMeters.addObject(meterId + "|" + t.getDataQualityLetter() + t.getDataQualityFlags() + "|" + t.getValueString());
							} else if (val > 99.0) {
								highIntervalMeters.addObject(meterId + "|" + t.getDataQualityLetter() + t.getDataQualityFlags() + "|" + t.getValueString());
							}
						//}
					}
					
					boolean[] mismatchAry = new boolean[] { false };
					List<IntervalTriplet> combinedTriplets = syncIntervalsToRegisterTimestamps(startRegTriplet.getTimestamp(), stopRegTriplet.getTimestamp(), intervalLengthMinutes, intTriplets, mismatchAry);
					if (mismatchAry[0]) {
						misalignedTimestampMeters.addObject(meterId);
					}
					double total = 0.0;
					boolean foundMissingInterval = false;
					Iterator<IntervalTriplet> tIter = combinedTriplets.iterator();
					while (!foundMissingInterval && tIter.hasNext()) {
						IntervalTriplet t = tIter.next();
						if ("R".equals(t.getDataQualityLetter())) {
							total += getValue(t);
						} else {
							foundMissingInterval = true;
						}
					}
					
					if (!foundMissingInterval) {
						if (Math.abs(total - regDifference) > 0.0001) {
							DecimalFormat df = new DecimalFormat("0.###");
							String units = regRecord.getUnits();
							if (units != null && units.endsWith("REG")) {
								units = units.substring(0, units.length() - 3);
							}
							sumCheckMeters.addObject(meterId 
									+ "|" + DateUtils.formatCMEPTimestamp(startRegTimestamp) 
									+ "|" + DateUtils.formatCMEPTimestamp(stopRegTimestamp) 
									+ "|" + df.format(total - regDifference)
									+ "|" + df.format(total)
									+ "|" + df.format(regDifference)
									+ "|" + units
									);
						}
					} else if (total - regDifference > 0.0001) {
						intervalsExceedRegisterDiffMeters.addObject(meterId + "|" + DateUtils.formatCMEPTimestamp(startRegTimestamp) + "|" + DateUtils.formatCMEPTimestamp(stopRegTimestamp) + "|" + new DecimalFormat("0.###").format((total - regDifference)));
					}
					
				}
			}
		}
	}
	
	
	private List<IntervalTriplet> syncIntervalsToRegisterTimestamps(Date startRegTimestamp, Date stopRegTimestamp, int intervalLengthMinutes, List<IntervalTriplet> inputTriplets, boolean[] bFoundMismatch) {
		Map<Date, IntervalTriplet> realIntervalMap = new HashMap<Date, IntervalTriplet>();
		for (IntervalTriplet t : inputTriplets) {
			realIntervalMap.put(t.getTimestamp(), t);
		}
		
		List<IntervalTriplet> result = new ArrayList<IntervalTriplet>();
		
		Calendar c = Calendar.getInstance();
		c.setTime(startRegTimestamp);
		c.add(Calendar.MINUTE, intervalLengthMinutes);
		Date intervalEndTimestamp = c.getTime();
		while (!intervalEndTimestamp.after(stopRegTimestamp)) {
			IntervalTriplet t = realIntervalMap.remove(intervalEndTimestamp);
			if (t == null) {
				t = new IntervalTriplet(intervalEndTimestamp, "N32", "0");
				bFoundMismatch[0] = true;
			}
			result.add(t);
			c.add(Calendar.MINUTE, intervalLengthMinutes);
			intervalEndTimestamp = c.getTime();
		}
		if (!realIntervalMap.isEmpty()) {
			bFoundMismatch[0] = true;
		}
		return result;
	}
	
	
	private double getValue(IntervalTriplet triplet) {
		return Double.parseDouble(triplet.getValueString());
	}
	
	private int countMultipleRegisterRecords(CMEPRecordSet recordSet) {
		int regRecordCount = 0;
		for (int i=0; i<recordSet.getRecordCount(); i++) {
			CMEPRecord rec = recordSet.getRecord(i);
			if (isRegisterRecord(rec)) {
				regRecordCount++;
			}
		}
		return regRecordCount;
	}
	
	private CMEPRecord getFirstRegisterRecord(CMEPRecordSet recordSet) {
		for (int i=0; i<recordSet.getRecordCount(); i++) {
			CMEPRecord rec = recordSet.getRecord(i);
			if (isRegisterRecord(rec)) {
				return rec;
			}
		}
		return null;
	}
	
	private boolean isRegisterRecord(CMEPRecord rec) {
		String units = (rec == null) ? null : rec.getUnits();
		if (units == null) {
			return false;
		} else {
			return units.toUpperCase().endsWith("REG");
		}
	}

	@Override
	protected void lastRecordSetReceived() { }
	
	
	public DistinctStringCounter getMultipleRegRecordsCounter() {
		return multipleRegRecordMeters;
	}
	
	public DistinctStringCounter getRegisterDropCounter() {
		return regDropMeters;
	}
	
	public DistinctStringCounter getMisalignedTimestampCounter() {
		return misalignedTimestampMeters;
	}
	
	public DistinctStringCounter getSumCheckCounter() {
		return sumCheckMeters;
	}
	public DistinctStringCounter getIntervalsExceedRegisterDiffCounter() {
		return intervalsExceedRegisterDiffMeters;
	}
	
	public DistinctStringCounter getNegativeIntervalCounter() {
		return negativeIntervalMeters;
	}
	
	public DistinctStringCounter getHighIntervalCounter() {
		return highIntervalMeters;
	}
	
	public DistinctStringCounter getSameRegistersCounter() {
		return sameRegistersMeters;
	}
	
	
	@Override
	protected void doReset() {
		for (DistinctStringCounter counter : counters) {
			counter.reset();
		}
	}

	public void condense() {
		for (DistinctStringCounter counter : counters) {
			counter.condense();
		}
	}
	
	
	
	
	
}
