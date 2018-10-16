package com.matthalstead.fileanalyzer.swing;

import javax.swing.SwingUtilities;

import com.matthalstead.fileanalyzer.InputStreamConsumer;

public abstract class ConsumerPanelWithUpdate<T extends InputStreamConsumer> extends ConsumerPanel {
	private static final long serialVersionUID = 8741650499269900219L;

	private final Object PENDING_UPDATE_LOCK = new Object();
	private boolean pendingUpdate = false;
	
	private Runnable updateRunner = null;
	
	@Override
	public final void handleConsumerUpdate() {
		synchronized(PENDING_UPDATE_LOCK) {
			if (!pendingUpdate) {
				pendingUpdate = true;
				if (updateRunner == null) {
					updateRunner = new Runnable() {
						@SuppressWarnings("unchecked")
						public void run() {
							synchronized(PENDING_UPDATE_LOCK) {
								if (pendingUpdate) {
									T consumer = (T) ConsumerPanelWithUpdate.super.consumer;
									handleUpdate(consumer);
									pendingUpdate = false;
								}
							}
						}
					};
				}
				SwingUtilities.invokeLater(updateRunner);
			}
		}
	}
	
	protected abstract void handleUpdate(T consumer);
}
