package com.matthalstead.fileanalyzer.cmep.consumers;

import com.matthalstead.fileanalyzer.cmep.CMEPRecord;
import com.matthalstead.fileanalyzer.consumers.AbstractLineConsumer;

public abstract class AbstractCMEPConsumer extends AbstractLineConsumer {

	@Override
	public final void consumeLine(String str, int lineIndex) {
		CMEPRecord record = new CMEPRecord(str);
		consumeRecord(record, lineIndex);
	}
	
	protected abstract void consumeRecord(CMEPRecord record, int lineIndex);

}
