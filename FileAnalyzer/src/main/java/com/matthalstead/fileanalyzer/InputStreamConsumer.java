package com.matthalstead.fileanalyzer;

import java.io.IOException;
import java.io.InputStream;

public interface InputStreamConsumer {
	
	int DEFAULT_BUFFER_SIZE = 8192;
	
	void consume(InputStream is) throws IOException;
	
	void reset();
	void cancel();
	void condense();
	
	int getAvailableBytesEstimate();
}
