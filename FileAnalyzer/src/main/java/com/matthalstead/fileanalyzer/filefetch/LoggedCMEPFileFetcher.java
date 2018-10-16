package com.matthalstead.fileanalyzer.filefetch;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.bind.JAXB;

import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.matthalstead.fileanalyzer.AppSettings;
import com.matthalstead.fileanalyzer.filedb.FileDatabase.FileDatabaseRecord;

public class LoggedCMEPFileFetcher extends SFTPFileFetcher {
	
	private int fileYear;
	private int fileMonth;
	private int fileDay;
	
	private boolean includeEarlyFile = true;
	private boolean includeLateFile = true;
	
	private String getIntAsString(int i, int len) {
		String str = "" + i;
		if (str.length() > len) {
			return str.substring(str.length() - len);
		}
		while (str.length() < len) {
			str = "0" + str;
		}
		return str;
	}
	private String getDateAsString() {
		return getIntAsString(fileYear, 4) + "-" + getIntAsString(fileMonth, 2) + "-" + getIntAsString(fileDay, 2);
	}
	private static final String PREFIX = "CMEPInterval.";
	@Override
	protected String getOutputRecordName() {
		return getOutputRecordNameNoSuffix() + " (from " + getSystemName() + ")";
	}
	protected String getOutputRecordNameNoSuffix() {
		String result = PREFIX + getDateAsString();
		if (includeEarlyFile) {
			if (includeLateFile) {

			} else {
				result += ".1st";
			}
		} else if (includeLateFile) {
			result += ".2nd";
		} else {
			result += ".none";
		}
		result += ".csv";
		return result;
	}
	@Override
	protected FileDatabaseRecord getOutputRecord() {
		FileDatabaseRecord fdr = new FileDatabaseRecord();
		fdr.setAbsoluteFilename(new File(AppSettings.getFileDatabaseFilesDir(), getOutputRecordNameNoSuffix() + ".gz.dat").getAbsolutePath());
		fdr.setType("CMEP");
		String desc = "CMEP file for " + getDateAsString();
		if (includeEarlyFile && !includeLateFile) {
			desc += " [early file only]";
		} else if (!includeEarlyFile && includeLateFile) {
			desc += " [late file only]";
		}
		desc += ", from " + getSystemName() + " (" + getServerName() + ")";
		fdr.setDescription(desc);
		return fdr;
	}
	@Override
	protected String getOutputRecordName(LsEntry entry) {
		return entry.getFilename() + " (from " + getSystemName() + ")";
	}
	@Override
	protected FileDatabaseRecord getOutputRecord(LsEntry entry) {
		String recName = entry.getFilename() + ".dat";
		FileDatabaseRecord fdr = new FileDatabaseRecord();
		fdr.setAbsoluteFilename(new File(AppSettings.getFileDatabaseFilesDir(), recName).getAbsolutePath());
		fdr.setType("CMEP");
		String desc = "CMEP file for " + getDateAsString() + ", from " + getSystemName() + " (" + getServerName() + ")";
		if (includeEarlyFile && !includeLateFile) {
			desc += " [early file only]";
		} else if (!includeEarlyFile && includeLateFile) {
			desc += " [late file only]";
		}
		desc += " (" + recName + ")";
		fdr.setDescription(desc);
		return fdr;
	}
	@Override
	protected void trimList(List<LsEntry> entries) {
		String dateWithoutDashes = getDateAsString().replace("-", "");
		String filePatternStr = "CMEPIntervalReadRpt.*\\." + dateWithoutDashes + ".*";
		Pattern p = Pattern.compile(filePatternStr);
		
		Set<String> alreadyProcessedLengthAndFileTimestamp = new HashSet<String>();
		
		
		Iterator<LsEntry> entryIter = entries.iterator();
		while (entryIter.hasNext()) {
			LsEntry entry = entryIter.next();
			String filename = entry.getFilename();
			boolean keep = true;
			if (p.matcher(filename).matches()) {
				
				if (!includeEarlyFile || !includeLateFile) {
					int hourStartIndex = filename.indexOf(".") + 1;
					int hourEndIndex = hourStartIndex + 2;
					int hour = Integer.parseInt(filename.substring(hourStartIndex, hourEndIndex));
					boolean isEarly = hour < 7;
					if (isEarly && !includeEarlyFile) {
						keep = false;
					} else if (!isEarly && !includeLateFile) {
						keep = false;
					}
				}
				
				String fileTimestamp = filename.split("[\\.\\_]")[1];
				long len = entry.getAttrs().getSize();
				String key = len + "_" + fileTimestamp;
				if (alreadyProcessedLengthAndFileTimestamp.contains(key)) {
					keep = false;
				} else {
					alreadyProcessedLengthAndFileTimestamp.add(key);
				}
			} else {
				keep = false;
			}
			if (!keep) {
				entryIter.remove();
			}
		}
	}
	
	
	public static void main(String[] args) {
		try {
			LoggedCMEPFileFetcher ff = new LoggedCMEPFileFetcher();
			ff.setSystemName("AMIBUS.TEST1");
			ff.setServerName("cccunmrv0217");
			ff.setDirectory("/appdata/peco_ami/Reads/HeadEnd/from/archive");
			ff.setFileYear(2011);
			ff.setFileMonth(12);
			ff.setFileDay(9);
			ff.setFilenamePattern("CMEPIntervalReadRpt.*");
			ff.setRetainMultipleFiles(false);
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			JAXB.marshal(ff, baos);
			System.out.println(baos);
			ff.fetch();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public int getFileDay() {
		return fileDay;
	}
	public void setFileDay(int fileDay) {
		this.fileDay = fileDay;
	}
	public int getFileMonth() {
		return fileMonth;
	}
	public void setFileMonth(int fileMonth) {
		this.fileMonth = fileMonth;
	}
	public int getFileYear() {
		return fileYear;
	}
	public void setFileYear(int fileYear) {
		this.fileYear = fileYear;
	}
	public boolean isIncludeEarlyFile() {
		return includeEarlyFile;
	}
	public void setIncludeEarlyFile(boolean includeEarlyFile) {
		this.includeEarlyFile = includeEarlyFile;
	}
	public boolean isIncludeLateFile() {
		return includeLateFile;
	}
	public void setIncludeLateFile(boolean includeLateFile) {
		this.includeLateFile = includeLateFile;
	}
	
	
}
