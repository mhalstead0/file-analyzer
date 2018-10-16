package com.matthalstead.fileanalyzer;

import java.io.IOException;
import java.io.InputStream;

public interface InputStreamHolder {

	InputStream getInputStream() throws IOException;
}
