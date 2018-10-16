package com.matthalstead.fileanalyzer;

import java.io.File;

public class AppSettings {

	
	private static File getFileDatabaseDir() {
		return new File("filedb");
	}
	
	public static File getFileDatabaseDescriptor() {
		return new File(getFileDatabaseDir(), "db.xml");
	}
	
	public static File getFileDatabaseFilesDir() {
		return new File(getFileDatabaseDir(), "files");
	}
	
	public static int getMaxEnqueuedBytes() {
		return 1024*100;
	}
	
	public static long getMillisToWaitWhenEnqueuedBytesExceedMax() {
		return 2000L;
	}
	
	public static File getConfDir() {
		return new File("conf");
	}
	
	public static File getAuthFile() {
		return new File(getConfDir(), "auth.properties");
	}
}
