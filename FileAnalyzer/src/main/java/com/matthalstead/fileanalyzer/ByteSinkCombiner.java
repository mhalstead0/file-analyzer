package com.matthalstead.fileanalyzer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ByteSinkCombiner implements ByteSink {
	
	private final List<ByteSink> sinks;
	
	public ByteSinkCombiner(ByteSink ... sinks) {
		this(Arrays.asList(sinks));
	}
	public ByteSinkCombiner(List<ByteSink> sinks) {
		this.sinks = (sinks == null) ? new ArrayList<ByteSink>(0) : new ArrayList<ByteSink>(sinks);
	}

	private void handleException(Exception e) {
		e.printStackTrace();
	}
	
	public void cancelSink() {
		for (ByteSink sink : sinks) {
			try {
				sink.cancelSink();
			} catch (Exception e) {
				handleException(e);
			}
		}
	}

	public void closeSink() {
		for (ByteSink sink : sinks) {
			try {
				sink.closeSink();
			} catch (Exception e) {
				handleException(e);
			}
		}
	}

	public void writeBytes(byte[] ary) throws IOException {
		writeBytes(ary, 0, ary.length);
	}

	public void writeBytes(byte[] ary, int start, int len) throws IOException {
		byte[] tempAry = new byte[len];
		for (ByteSink sink : sinks) {
			System.arraycopy(ary, start, tempAry, 0, len);
			try {
				sink.writeBytes(tempAry);
			} catch (Exception e) {
				handleException(e);
			}
		}
	}
	
	public int getEnqueuedBytesEstimate() {
		int result = 0;
		for (ByteSink sink : sinks) {
			int thisBytes = sink.getEnqueuedBytesEstimate();
			if (thisBytes > result) {
				result = thisBytes;
			}
		}
		return result;
	}

}
