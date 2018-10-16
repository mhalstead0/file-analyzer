package com.matthalstead.fileanalyzer.consumers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.matthalstead.fileanalyzer.AbstractInputStreamConsumer;

public abstract class AbstractLineConsumer extends AbstractInputStreamConsumer {

	private int lastCheckAvailableBytes = 0;
	
	public final void consume(InputStream is) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		int count = 0;
		String str;
		while ((str = br.readLine()) != null) {
			consumeLine(str, count++);
			lastCheckAvailableBytes = is.available();
		}
		lastLineReceived();
	}
	
	public abstract void consumeLine(String str, int lineIndex);
	public abstract void lastLineReceived();

	public int getAvailableBytesEstimate() {
		return lastCheckAvailableBytes;
	}
}
