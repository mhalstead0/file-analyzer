package com.matthalstead.fileanalyzer;

public abstract class AbstractInputStreamConsumer implements InputStreamConsumer {
	
	private boolean cancelled = false;
	public final void cancel() {
		cancelled = true;
	}
	protected boolean isCancelled() {
		return cancelled;
	}

	public final void reset() {
		cancelled = false;
		doReset();
		cancelled = false;
	}
	protected abstract void doReset();
	
}
