package com.matthalstead.fileanalyzer.cmep.consumers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.matthalstead.fileanalyzer.cmep.CMEPRecord;
import com.matthalstead.fileanalyzer.cmep.CMEPRecordSet;
import com.matthalstead.fileanalyzer.cmep.CMEPRecord.IntervalTriplet;


public class CMEPFlagConsumer extends AbstractMeterGroupCMEPConsumer {
	
	public static final int NUM_FLAGS = 64;
	
	private Map<String, FlagCounterImpl[]> unitToFlagCounterArray = new HashMap<String, FlagCounterImpl[]>();
	
	
	private FlagCounterImpl[] buildFlagCounterImplArray() {
		FlagCounterImpl[] result = new FlagCounterImpl[NUM_FLAGS];
		for (int i=0; i<NUM_FLAGS; i++) {
			long flagValue = 1L << i;
			result[i] = new FlagCounterImpl(flagValue, "Flag " + flagValue + " (0x" + Long.toHexString(flagValue) + ")");
		}
		return result;
	}

	
	@Override
	protected void recordSetReceived(CMEPRecordSet recordSet) {
		int recordCount = recordSet.getRecordCount();
		for (int i=0; i<recordCount; i++) {
			CMEPRecord rec = recordSet.getRecord(i);
			List<IntervalTriplet> triplets = rec.getTriplets();
			if (triplets != null) {
				for (IntervalTriplet triplet : triplets) {
					long flags = triplet.getDataQualityFlags();
					if (flags != 0) {
						handleTriplet(rec, flags);
					}
				}
			}
		}
	}
	
	
	private void handleTriplet(CMEPRecord rec, long flags) {
		String meterId = rec.getMeterId();
		String units = rec.getUnits();
		FlagCounterImpl[] ary = unitToFlagCounterArray.get(units);
		if (ary == null) {
			ary = buildFlagCounterImplArray();
			unitToFlagCounterArray.put(units, ary);
		}
		int i = 0;
		for (long singleFlag = 1; flags != 0 && singleFlag != 0; singleFlag <<= 1) {
			if ((flags & singleFlag) != 0) {
				FlagCounterImpl counter = ary[i];
				counter.addFlag(meterId);
				flags &= (~singleFlag);
			}
			i++;
		}
	}
	
	@Override
	protected void lastRecordSetReceived() { }
	
	@Override
	protected void doReset() {
		for (FlagCounterImpl[] counters : unitToFlagCounterArray.values()) {
			for (int i=0; i<counters.length; i++) {
				counters[i].reset();
			}
		}
	}
	public void condense() {
		for (FlagCounterImpl[] counters : unitToFlagCounterArray.values()) {
			for (int i=0; i<counters.length; i++) {
				counters[i].condense();
			}
		}
	}
	
	public Map<String, FlagCounter[]> getUnitsToFlagCounterArray() {
		Map<String, FlagCounter[]> result = new HashMap<String, FlagCounter[]>();
		for (Map.Entry<String, FlagCounterImpl[]> entry : unitToFlagCounterArray.entrySet()) {
			String units = entry.getKey();
			FlagCounterImpl[] srcAry = entry.getValue();
			FlagCounter[] dstAry = new FlagCounterImpl[srcAry.length];
			System.arraycopy(srcAry, 0, dstAry, 0, srcAry.length);
			result.put(units, dstAry);
		}
		return result;
	}
	
	public static interface FlagCounter {
		long getFlagValue();
		String getFlagName();
		int getFlagCount();
		int getDistinctMeterCount();
		Set<String> getMeters();
	}
	
	private static class FlagCounterImpl implements FlagCounter {
		
		private final long value;
		private final String name;
		
		private int flagCount = 0;
		private final DistinctObjectCounterWithCondense<String> meterCounter = new DistinctObjectCounterWithCondense<String>();
		
		private FlagCounterImpl(long value, String name) {
			this.value = value;
			this.name = name;
		}
		
		public int getDistinctMeterCount() {
			return meterCounter.getCount();
		}
		public int getFlagCount() {
			return flagCount;
		}
		public String getFlagName() {
			return name;
		}
		public long getFlagValue() {
			return value;
		}
		public Set<String> getMeters() {
			Set<String> result = meterCounter.getObjects();
			return (result == null) ? null : new HashSet<String>(result);
		}
		
		private void reset() {
			flagCount = 0;
			meterCounter.reset();
		}
		
		private void condense() {
			meterCounter.condense();
		}
		
		private void addFlag(String meterId) {
			flagCount++;
			meterCounter.addObject(meterId);
		}
	}
	
}
