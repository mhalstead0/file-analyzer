package com.matthalstead.fileanalyzer.filefetch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.matthalstead.fileanalyzer.filedb.FileDatabase;
import com.matthalstead.fileanalyzer.filedb.FileDatabase.FileDatabaseRecord;

public abstract class SFTPFileFetcher extends FileFetcher {
	
	private static final Logger log = Logger.getLogger(SFTPFileFetcher.class);
	
	private String systemName;
	private String serverName;
	
	private String directory;
	private boolean retainMultipleFiles;
	
	private String filenamePattern;
	
	
	protected abstract void trimList(List<LsEntry> entries);
	protected abstract String getOutputRecordName();
	protected abstract String getOutputRecordName(LsEntry entry);
	protected abstract FileDatabaseRecord getOutputRecord();
	protected abstract FileDatabaseRecord getOutputRecord(LsEntry entry);
	
	
	private static FetchController wrapFetchControllerWithLogging(FetchController fetchController) {
		fetchController = (fetchController == null) ? new DefaultFetchController() : fetchController;
		final FetchController fc = fetchController;
		return new FetchController() {
			public void statusUpdate(FetchStatus status, String message, long bytesFetched, Long totalBytes) {
				if (status != FetchStatus.BYTES_RECEIVED) {
					String logMessage = "" + status;
					if (message != null) {
						logMessage += ": " + message;
					}
					log.debug(logMessage);
				}
				fc.statusUpdate(status, message, bytesFetched, totalBytes);
			}
		};
	}
	
	@Override
	public void fetch(FetchController fetchController) {
		fetchController = wrapFetchControllerWithLogging(fetchController);
		try {
			fetchController.statusUpdate(FetchStatus.STARTED, null, 0, null);
			fetchController.statusUpdate(FetchStatus.CONNECTION_STEP, "Looking up LoginProvider for system " + systemName + "...", 0, null);
			LoginProvider loginProvider = LoginManager.getLoginProvider(systemName);
			if (loginProvider == null) {
				throw new RuntimeException("Could not find a LoginProvider for " + systemName);
			} else if (loginProvider.isFailed()) {
				throw new RuntimeException("Login to " + systemName + " with username " + loginProvider.getUsername() + " has previously failed.");
			}
			

			fetchController.statusUpdate(FetchStatus.CONNECTION_STEP, "Logging into " + serverName + " as " + loginProvider.getUsername() + "...", 0, null);
			Session session = getSession(loginProvider);
			
			fetchController.statusUpdate(FetchStatus.CONNECTION_STEP, "Opening SFTP channel...", 0, null);
			ChannelSftp channel = getChannel(session);
			
			
			fetchController.statusUpdate(FetchStatus.SFTP_STEP, "Changing directories to " + directory + "...", 0, null);
			channel.cd(directory);
			
			fetchController.statusUpdate(FetchStatus.SFTP_STEP, "Listing files in " + directory + "...", 0, null);
			Vector<?> v = channel.ls(directory);
			fetchController.statusUpdate(FetchStatus.SFTP_STEP, "Initial file count: " + v.size(), 0, null);
			
			Pattern p;
			if (filenamePattern == null) {
				p = null;
			} else {
				fetchController.statusUpdate(FetchStatus.SFTP_STEP, "Only considering files that match the pattern /" + filenamePattern + "/", 0, null);
				p = Pattern.compile(filenamePattern);
			}
			
			int directoryCount = 0;
			List<LsEntry> entries = new ArrayList<LsEntry>();
			for (Object o : v) {
				LsEntry entry = (LsEntry) o;
				if (entry.getAttrs().isDir()) {
					directoryCount++;
				} else {
					String filename = entry.getFilename();
					boolean keep;
					if (p == null) {
						keep = true;
					} else {
						keep = p.matcher(filename).matches();
					}
					if (keep) {
						entries.add(entry);
					}
				}
			}
			
			
			
			fetchController.statusUpdate(FetchStatus.SFTP_STEP, "After filtering on pattern/directory, file count is " + entries.size() + " (dir count=" + directoryCount + ")", 0, null);
			trimList(entries);
			fetchController.statusUpdate(FetchStatus.SFTP_STEP, "After trimming, file count is " + entries.size(), 0, null);
			
			FileDatabase fileDB = FileDatabase.load();
			Map<String, FileDatabaseRecord> dbMap = fileDB.getMap();
			if (retainMultipleFiles) {
				log.debug("Fetching files; writing to individual output files...");
				for (LsEntry e : entries) {
					Long fileSize = e.getAttrs().getSize();
					FileDatabaseRecord fdr = getOutputRecord(e);
					fetchController.statusUpdate(FetchStatus.STARTED_FILE, "Writing " + e.getFilename() + " to " + fdr.getAbsoluteFilename() + "...", 0, fileSize);
					File f = new File(fdr.getAbsoluteFilename());
					FileOutputStream fos = new FileOutputStream(f);
					if (shouldGunzip(e)) {
						channel.get(e.getFilename(), fos);
					} else {
						InputStream is = channel.get(e.getFilename());
						GZIPOutputStream gzos = new GZIPOutputStream(fos);
						transfer(is, gzos, true, false, e.getFilename(), fetchController, fileSize);
						gzos.finish();
						gzos.close();
					}
					fetchController.statusUpdate(FetchStatus.FINISHED_FILE, e.getFilename(), 0, fileSize);
					
					fos.close();
					dbMap.put(getOutputRecordName(e), fdr);
				}
			} else {
				FileDatabaseRecord fdr = getOutputRecord();
				fetchController.statusUpdate(FetchStatus.SFTP_STEP, "Fetching files; writing to " + fdr.getAbsoluteFilename() + "...", 0, null);
				File f = new File(fdr.getAbsoluteFilename());
				FileOutputStream fos = new FileOutputStream(f);
				GZIPOutputStream gzos = new GZIPOutputStream(fos);
				for (LsEntry e : entries) {
					Long fileSize = e.getAttrs().getSize();
					fetchController.statusUpdate(FetchStatus.STARTED_FILE, "Fetching " + e.getFilename() + "...", 0, null);
					InputStream is = channel.get(e.getFilename());
					if (shouldGunzip(e)) {
						is = new GZIPInputStream(is);
					}
					transfer(is, gzos, true, false, e.getFilename(), fetchController, fileSize);
					fetchController.statusUpdate(FetchStatus.FINISHED_FILE, e.getFilename(), 0, fileSize);
				}
				gzos.finish();
				gzos.close();
				fos.close();
				dbMap.put(getOutputRecordName(), fdr);
			}
			fetchController.statusUpdate(FetchStatus.SFTP_STEP, "Done fetching files.", 0, null);
			
			log.debug("Saving FileDatabase...");
			fileDB.setMap(dbMap);
			FileDatabase.save(fileDB);
			log.debug("Done saving FileDatabase.");
			
			fetchController.statusUpdate(FetchStatus.CONNECTION_STEP, "Closing down SFTP connection...", 0, null);
			channel.exit();
			fetchController.statusUpdate(FetchStatus.CONNECTION_STEP, "Channel exited (1/3)", 0, null);
			channel.disconnect();
			fetchController.statusUpdate(FetchStatus.CONNECTION_STEP, "Channel disconnected (2/3)", 0, null);
			session.disconnect();
			fetchController.statusUpdate(FetchStatus.CONNECTION_STEP, "Session disconnected (3/3)", 0, null);
			fetchController.statusUpdate(FetchStatus.FINISHED, "Done fetching files.", 0, null);
		} catch (Exception e) {
			fetchController.statusUpdate(FetchStatus.ERROR_ENCOUNTERED, e.getMessage(), 0, null);
			log.error(e);
			throw new RuntimeException(e);
		}
		
	}
	
	private void transfer(InputStream is, OutputStream os, boolean closeIS, boolean closeOS, String filename, FetchController fc, Long totalBytes) throws IOException {
		long totalRead = 0;
		long lastCheck = 0;
		long bytesBetweenStatuses = 1024L * 1024L / 4L;
		byte[] buf = new byte[8192];
		int read;
		while ((read = is.read(buf)) > 0) {
			os.write(buf, 0, read);
			totalRead += ((long) read);
			if (fc != null) {
				if (lastCheck == 0 || (totalRead - lastCheck > bytesBetweenStatuses)) {
					fc.statusUpdate(FetchStatus.BYTES_RECEIVED, filename, totalRead, totalBytes);
					lastCheck = totalRead;
				}
			}
		}
		if (fc != null && totalRead != lastCheck) {
			fc.statusUpdate(FetchStatus.BYTES_RECEIVED, null, totalRead, totalBytes);
		}
		if (closeIS) {
			is.close();
		}
		if (closeOS) {
			os.close();
		}
	}
	
	protected boolean shouldGunzip(LsEntry e) {
		return e.getFilename().toLowerCase().endsWith(".gz");
	}
	
	
	private Session getSession(LoginProvider loginProvider) throws Exception {
		JSch jsch = new JSch();
		Session session = jsch.getSession(loginProvider.getUsername(), serverName);
		session.setUserInfo(buildUserInfo(loginProvider));
		Properties config = new Properties();
		config.put("compression.s2c", "zlib,none");
		config.put("compression.c2s", "zlib,none");
		session.setConfig(config);
		session.connect();
		return session;
	}
	
	private ChannelSftp getChannel(Session session) throws Exception {
		
		ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
		channel.setExtOutputStream(System.err);
		channel.connect();
		return channel;
	}
	
	private UserInfo buildUserInfo(final LoginProvider loginProvider) {
		UserInfo ui = new UserInfo() {
			
			boolean triedOnce = false;

			public String getPassphrase() {
				return null;
			}

			public String getPassword() {
				if (triedOnce) {
					throw new RuntimeException("Already tried to log in as " + loginProvider.getUsername() + "!");
				}
				triedOnce = true;
				return loginProvider.getPassword();
			}

			public boolean promptPassphrase(String msg) {
				log.info(msg + ":");
				return false;
			}

			public boolean promptPassword(String msg) {
				log.info(msg + ":");
				return !triedOnce;
			}

			public boolean promptYesNo(String msg) {
				log.info(msg + " (y/n):");
				return true;
			}

			public void showMessage(String msg) {
				log.info(msg);
			}

		};
		return ui;
	}
	
	public String getServerName() {
		return serverName;
	}
	
	public void setServerName(String serverName) {
		this.serverName = serverName;
	}
	
	public String getSystemName() {
		return systemName;
	}
	
	public void setSystemName(String systemName) {
		this.systemName = systemName;
	}

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	public boolean isRetainMultipleFiles() {
		return retainMultipleFiles;
	}

	public void setRetainMultipleFiles(boolean retainMultipleFiles) {
		this.retainMultipleFiles = retainMultipleFiles;
	}
	
	public String getFilenamePattern() {
		return filenamePattern;
	}
	
	public void setFilenamePattern(String filenamePattern) {
		this.filenamePattern = filenamePattern;
	}
	
}
