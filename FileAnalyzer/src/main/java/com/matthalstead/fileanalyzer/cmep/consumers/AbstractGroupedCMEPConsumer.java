package com.matthalstead.fileanalyzer.cmep.consumers;

import java.util.ArrayList;
import java.util.List;

import com.matthalstead.fileanalyzer.cmep.CMEPRecord;
import com.matthalstead.fileanalyzer.cmep.CMEPRecordSet;

public abstract class AbstractGroupedCMEPConsumer extends AbstractCMEPConsumer {
	
	private List<CMEPRecord> currentSet = new ArrayList<CMEPRecord>();

	@Override
	protected final void consumeRecord(CMEPRecord record, int lineIndex) {
		if (currentSet.isEmpty()) {
			currentSet.add(record);
		} else {
			if (inSameSet(currentSet.get(currentSet.size() - 1), record)) {
				currentSet.add(record);
			} else {
				recordSetReceived(new CMEPRecordSet(currentSet));
				currentSet.clear();
				currentSet.add(record);
			}
		}
	}

	@Override
	public final void lastLineReceived() {
		if (!currentSet.isEmpty()) {
			recordSetReceived(new CMEPRecordSet(currentSet));
			currentSet.clear();
		}
		lastRecordSetReceived();
	}
	
	protected static boolean areStringsEqual(String s1, String s2) {
		if (s1 == null) {
			return s2 == null;
		} else {
			return s1.equals(s2);
		}
	}
	
	protected abstract boolean inSameSet(CMEPRecord r1, CMEPRecord r2);
	protected abstract void recordSetReceived(CMEPRecordSet recordSet);
	protected abstract void lastRecordSetReceived();

}
