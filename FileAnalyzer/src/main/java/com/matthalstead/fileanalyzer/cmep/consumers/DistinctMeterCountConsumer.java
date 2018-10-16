package com.matthalstead.fileanalyzer.cmep.consumers;

import java.util.HashSet;
import java.util.Set;

import com.matthalstead.fileanalyzer.cmep.CMEPRecord;
import com.matthalstead.fileanalyzer.cmep.CMEPRecordSet;

public class DistinctMeterCountConsumer extends AbstractMeterGroupCMEPConsumer {
	
	private final Set<String> meterIds = new HashSet<String>();
	private int meterCount = -1;
	
	private final Object LOCK = new Object();

	@Override
	protected void recordSetReceived(CMEPRecordSet recordSet) {
		synchronized(LOCK) {
			CMEPRecord record = recordSet.getFirstRecordWithNoParseException();
			if (record != null) {
				String meterId = record.getMeterId();
				if (meterId != null) {
					meterIds.add(meterId);
				}
			}
		}
	}

	@Override
	protected void lastRecordSetReceived() { }

	@Override
	protected void doReset() {
		synchronized(LOCK) {
			meterIds.clear();
			meterCount = -1;
		}
	}

	public void condense() {
		synchronized(LOCK) {
			meterCount = meterIds.size();
			meterIds.clear();
		}
	}
	
	public int getMeterCount() {
		synchronized(LOCK) {
			if (meterCount < 0) {
				return meterIds.size();
			} else {
				return meterCount;
			}
		}
	}

}
