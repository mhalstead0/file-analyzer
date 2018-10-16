package com.matthalstead.fileanalyzer.cmep.consumers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.matthalstead.fileanalyzer.cmep.CMEPRecord;
import com.matthalstead.fileanalyzer.cmep.CMEPRecordSet;

public class CMEPRecordSetCountConsumer extends AbstractMeterGroupCMEPConsumer {

	private int recordSetCount = 0;
	private int recordCount = 0;
	private final DistinctObjectCounterWithCondense<String> meterCounter = new DistinctObjectCounterWithCondense<String>();
	private Map<String, IntegerHolder> unitToRecordCountMap = new HashMap<String, IntegerHolder>();
	private Map<String, IntegerHolder> unitToIntervalCountMap = new HashMap<String, IntegerHolder>();
	
	
	public CMEPRecordSetCountConsumer() {
		
	}
	
	public CMEPRecordSetCountConsumer(boolean b) {
		
	}
	
	@Override
	protected void recordSetReceived(CMEPRecordSet recordSet) {
		recordSetCount++;
		recordCount += recordSet.getRecordCount();
		try {
			meterCounter.addObject(recordSet.getFirstRecordWithNoParseException().getMeterId());
		} catch (Exception e) {
			// ignore
		}
		int recordCount = recordSet.getRecordCount();
		for (int i=0; i<recordCount; i++) {
			CMEPRecord record = recordSet.getRecord(i);
			String units = record.getUnits();
			if (units == null) {
				units = "null";
			}
			incrementUnitRecordCount(units);
			incrementIntervalRecordCount(units, record.getTriplets().size());
		}
		
	}
	
	private void incrementUnitRecordCount(String units) {
		IntegerHolder ih = unitToRecordCountMap.get(units);
		if (ih == null) {
			ih = new IntegerHolder();
			Map<String, IntegerHolder> newMap = new HashMap<String, IntegerHolder>(unitToRecordCountMap);
			newMap.put(units, ih);
			unitToRecordCountMap = newMap;
		}
		ih.i++;
	}
	
	
	private void incrementIntervalRecordCount(String units, int count) {
		IntegerHolder ih = unitToIntervalCountMap.get(units);
		if (ih == null) {
			ih = new IntegerHolder();
			Map<String, IntegerHolder> newMap = new HashMap<String, IntegerHolder>(unitToIntervalCountMap);
			newMap.put(units, ih);
			unitToIntervalCountMap = newMap;
		}
		ih.i += count;
	}
	
	public Map<String, Integer> getRecordCountsByUnits() {
		return getCountsByUnits(unitToRecordCountMap);
	}
	
	private Map<String, Integer> getCountsByUnits(Map<String, IntegerHolder> map) {
		Map<String, Integer> result = new TreeMap<String, Integer>();
		Iterator<Map.Entry<String, IntegerHolder>> entryIter = map.entrySet().iterator();
		while (entryIter.hasNext()) {
			Map.Entry<String, IntegerHolder> entry = entryIter.next();
			result.put(entry.getKey(), entry.getValue().i);
		}
		return result;
	}

	public Map<String, Integer> getIntervalCountsByUnits() {
		return getCountsByUnits(unitToIntervalCountMap);
	}

	@Override
	protected void lastRecordSetReceived() { }

	@Override
	protected void doReset() {
		recordSetCount = 0;
		recordCount = 0;
		meterCounter.reset();
		unitToRecordCountMap = new HashMap<String, IntegerHolder>();
		unitToIntervalCountMap = new HashMap<String, IntegerHolder>();
	}

	public void condense() {
		meterCounter.condense();
	}
	
	public int getRecordSetCount() {
		return recordSetCount;
	}
	
	public int getDistinctMeterIdCount() {
		return meterCounter.getCount();
	}
	
	public int getRecordCount() {
		return recordCount;
	}

	private static class IntegerHolder {
		private int i;
	}
}
