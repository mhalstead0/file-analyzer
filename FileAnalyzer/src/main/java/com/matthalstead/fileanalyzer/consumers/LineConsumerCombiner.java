package com.matthalstead.fileanalyzer.consumers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LineConsumerCombiner extends AbstractLineConsumer {
	
	private final List<AbstractLineConsumer> consumers;
	
	public LineConsumerCombiner(AbstractLineConsumer ... consumers) {
		this(Arrays.asList(consumers));
	}
	
	public LineConsumerCombiner(List<AbstractLineConsumer> consumers) {
		this.consumers = (consumers == null) ? null : new ArrayList<AbstractLineConsumer>(consumers);
	}
	
	private void handleException(Exception e) {
		throw new RuntimeException(e);
	}
	
	@Override
	protected void doReset() {
		for (AbstractLineConsumer c : consumers) {
			try {
				c.reset();
			} catch (Exception e) {
				handleException(e);
			}
		}
	}

	public void condense() {
		for (AbstractLineConsumer c : consumers) {
			try {
				c.condense();
			} catch (Exception e) {
				handleException(e);
			}
		}
	}

	@Override
	public void consumeLine(String str, int lineIndex) {
		for (AbstractLineConsumer c : consumers) {
			try {
				c.consumeLine(str, lineIndex);
			} catch (Exception e) {
				handleException(e);
			}
		}
	}

	@Override
	public void lastLineReceived() {
		for (AbstractLineConsumer c : consumers) {
			try {
				c.lastLineReceived();
			} catch (Exception e) {
				handleException(e);
			}
		}
	}

	

}
