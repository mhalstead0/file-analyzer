package com.matthalstead.fileanalyzer.consumers;

import java.io.IOException;
import java.io.InputStream;

import com.matthalstead.fileanalyzer.AbstractInputStreamConsumer;

public class ByteCountingConsumer extends AbstractInputStreamConsumer {
	
	
	private long byteCount = 0;
	
	private int lastCheckAvailableBytes = 0;
	
	public void condense() {
		
	}
	
	public void consume(InputStream is) throws IOException {
		byte[] buf = new byte[DEFAULT_BUFFER_SIZE];
		int read;
		while (!isCancelled() && (read = is.read(buf)) > 0) {
			byteCount += read;
			lastCheckAvailableBytes = is.available();
		}
	}
	@Override
	protected void doReset() {
		byteCount = 0;
	}
	public long getByteCount() {
		return byteCount;
	}
	
	public int getAvailableBytesEstimate() {
		return lastCheckAvailableBytes;
	}
}
