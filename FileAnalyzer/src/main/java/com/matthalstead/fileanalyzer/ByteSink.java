package com.matthalstead.fileanalyzer;

import java.io.IOException;

public interface ByteSink {

	void writeBytes(byte[] ary) throws IOException;
	void writeBytes(byte[] ary, int start, int len) throws IOException;
	void closeSink();
	void cancelSink();
	
	int getEnqueuedBytesEstimate();
}
