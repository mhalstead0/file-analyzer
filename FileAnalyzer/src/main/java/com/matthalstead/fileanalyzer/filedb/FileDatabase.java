package com.matthalstead.fileanalyzer.filedb;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXB;

import com.matthalstead.fileanalyzer.AppSettings;

public class FileDatabase {

	private Map<String, FileDatabaseRecord> map;
	
	public static FileDatabase load() throws IOException {
		File descriptorFile = AppSettings.getFileDatabaseDescriptor();
		FileDatabase result;
		if (descriptorFile.exists()) {
			result = JAXB.unmarshal(descriptorFile, FileDatabase.class);
		} else {
			result = new FileDatabase();
		}
		
		File dbFilesDir = AppSettings.getFileDatabaseFilesDir();
		Map<String, FileDatabaseRecord> map = result.getMap();
		if (map == null) {
			map = new HashMap<String, FileDatabaseRecord>();
			result.setMap(map);
		} else {
			Iterator<Map.Entry<String, FileDatabaseRecord>> entryIter = map.entrySet().iterator();
			while (entryIter.hasNext()) {
				Map.Entry<String, FileDatabaseRecord> entry = entryIter.next();
				String filename = entry.getKey();
				FileDatabaseRecord fdr = entry.getValue();
				try {
					String actualFilename = fdr.getAbsoluteFilename();
					File f = new File(actualFilename);
					filename = f.getName();
				} catch (Exception e) {
					//ignore
				}
				if (filename == null || fdr == null) {
					entryIter.remove();
				} else {
					File f = new File(dbFilesDir, filename);
					if (f.exists()) {
						fdr.setAbsoluteFilename(f.getCanonicalPath());
					} else {
						entryIter.remove();
					}
				}
			}
			
		}
		
		Set<String> lowercaseNames = new HashSet<String>();
		for (FileDatabaseRecord fdr : map.values()) {
			lowercaseNames.add(fdr.getFile().getName().toLowerCase());
		}
		File[] files = dbFilesDir.listFiles();
		for (int i=0; i<files.length; i++) {
			File f = files[i];
			if (!f.isDirectory()) {
				String name = f.getName();
				if (!lowercaseNames.contains(name.toLowerCase())) {
					FileDatabaseRecord fdr = new FileDatabaseRecord();
					fdr.setAbsoluteFilename(f.getCanonicalPath());
					fdr.setDescription(name);
					fdr.setType("unknown");
					fdr.setModifyTimestamp(f.lastModified());
					map.put(name, fdr);
				}
			}
		}
		return result;
	}

	public static void save(FileDatabase db) throws IOException {
		if (db != null) {
			File descriptorFile = AppSettings.getFileDatabaseDescriptor();
			JAXB.marshal(db, descriptorFile);
		}
		
	}
	
	public Map<String, FileDatabaseRecord> getMap() {
		return map;
	}
	public void setMap(Map<String, FileDatabaseRecord> map) {
		this.map = map;
	}
	
	
	
	public static class FileDatabaseRecord {
		private String absoluteFilename;
		private String type;
		private String description;
		private long modifyTimestamp;
		@Override
		public String toString() {
			return "[" + type + "]: \"" + description + "\" (" + absoluteFilename + ")";
		}
		public String getAbsoluteFilename() {
			return absoluteFilename;
		}
		public void setAbsoluteFilename(String absoluteFilename) {
			this.absoluteFilename = absoluteFilename;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public File getFile() {
			return new File(absoluteFilename);
		}
		public long getModifyTimestamp() {
			return modifyTimestamp;
		}
		public void setModifyTimestamp(long modifyTimestamp) {
			this.modifyTimestamp = modifyTimestamp;
		}
		
	}
}
