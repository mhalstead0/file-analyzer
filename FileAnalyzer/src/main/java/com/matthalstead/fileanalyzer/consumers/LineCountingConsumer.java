package com.matthalstead.fileanalyzer.consumers;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class LineCountingConsumer extends AbstractLineConsumer {
	
	private int lineCount = 0;

	@Override
	protected void doReset() {
		lineCount = 0;
	}

	public void condense() {
		
	}
	@Override
	public void consumeLine(String str, int lineIndex) {
		lineCount++;
		if (lineCount % 10000 == 0) {
			System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + ": Processed " + new DecimalFormat("#,##0").format(lineCount) + " lines.");
		}
	}
	@Override
	public void lastLineReceived() {
		
	}
	
	public int getLineCount() {
		return lineCount;
	}

}
