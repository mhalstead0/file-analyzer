package com.matthalstead.fileanalyzer.cmep.consumers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.matthalstead.fileanalyzer.cmep.CMEPRecord;

public class CMEPConsumerCombiner extends AbstractCMEPConsumer {
	
	private final List<AbstractCMEPConsumer> consumers;
	
	public CMEPConsumerCombiner(AbstractCMEPConsumer ... consumers) {
		this(Arrays.asList(consumers));
	}
	
	public CMEPConsumerCombiner(List<AbstractCMEPConsumer> consumers) {
		this.consumers = (consumers == null) ? null : new ArrayList<AbstractCMEPConsumer>(consumers);
	}
	
	private void handleException(Exception e) {
		//throw (e instanceof RuntimeException) ? ((RuntimeException) e) : new RuntimeException(e);
		e.printStackTrace();
	}

	@Override
	protected void consumeRecord(CMEPRecord record, int lineIndex) {
		for (AbstractCMEPConsumer consumer : consumers) {
			try {
				consumer.consumeRecord(record, lineIndex);
			} catch (Exception e) {
				handleException(e);
			}
		}
	}

	@Override
	public void lastLineReceived() {
		for (AbstractCMEPConsumer consumer : consumers) {
			try {
				consumer.lastLineReceived();
			} catch (Exception e) {
				handleException(e);
			}
		}
	}

	@Override
	protected void doReset() {
		for (AbstractCMEPConsumer consumer : consumers) {
			try {
				consumer.reset();
			} catch (Exception e) {
				handleException(e);
			}
		}
	}

	public void condense() {
		for (AbstractCMEPConsumer consumer : consumers) {
			try {
				consumer.condense();
			} catch (Exception e) {
				handleException(e);
			}
		}
	}
	
	

}
