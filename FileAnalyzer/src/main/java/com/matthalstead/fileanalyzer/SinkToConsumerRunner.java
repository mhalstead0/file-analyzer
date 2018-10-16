package com.matthalstead.fileanalyzer;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class SinkToConsumerRunner implements ByteSink {
	
	private static final int THREAD_PRIORITY = Thread.NORM_PRIORITY - 1;
	
	private final InputStreamConsumer consumer;
	private final PipedOutputStream outputStream;
	private final PipedInputStream inputStream;
	
	private final Object LOCK = new Object();
	private Runner runner = null;
	private Thread thread = null;
	
	public SinkToConsumerRunner(InputStreamConsumer consumer) {
		this.consumer = consumer;
		outputStream = new PipedOutputStream();
		try {
			inputStream = new PipedInputStream(outputStream);
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
		start();
	}

	public void writeBytes(byte[] ary) throws IOException {
		outputStream.write(ary);
	}

	public void writeBytes(byte[] ary, int start, int len) throws IOException {
		outputStream.write(ary, start, len);
	}
	
	private void finish() {
		try {
			outputStream.close();
			thread.join();
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		} catch (InterruptedException ie) {
			throw new RuntimeException(ie);
		}
	}
	
	public void cancelSink() {
		consumer.cancel();
		finish();
	}

	public void closeSink() {
		finish();
	}
	
	public int getEnqueuedBytesEstimate() {
		return consumer.getAvailableBytesEstimate();
	}

	private void start() {
		synchronized(LOCK) {
			if (runner != null) {
				throw new IllegalStateException("Runner has already been started.");
			}
			runner = new Runner();
			thread = new Thread(runner);
			thread.setPriority(THREAD_PRIORITY);
			thread.start();
		}
		
	}
	
	private class Runner implements Runnable {
		public void run() {
			try {
				consumer.consume(inputStream);
			} catch (IOException ioe) {
				throw new RuntimeException(ioe);
			}
		}
		
	}

}
