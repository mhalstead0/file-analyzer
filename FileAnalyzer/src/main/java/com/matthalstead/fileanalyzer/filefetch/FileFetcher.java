package com.matthalstead.fileanalyzer.filefetch;

public abstract class FileFetcher {

	public final void fetch() {
		fetch((FetchController) null);
	}
	
	public abstract void fetch(FetchController fetchController);
	
	public static enum FetchStatus {
		STARTED,
		CONNECTION_STEP,
		SFTP_STEP,
		STARTED_FILE,
		BYTES_RECEIVED,
		FINISHED_FILE,
		FINISHED,
		ERROR_ENCOUNTERED
	}
	
	public static interface FetchController {
		void statusUpdate(FetchStatus status, String message, long bytesFetched, Long totalBytes);
	}
	
	public static class DefaultFetchController implements FetchController {
		public void statusUpdate(FetchStatus status, String message, long bytesFetched, Long totalBytes) {
			
		}
	}
	
}
