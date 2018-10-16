package com.matthalstead.fileanalyzer.cmep;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CMEPRecordSet {

	private List<CMEPRecord> cmepRecords;
	
	public CMEPRecordSet(CMEPRecord ... records) {
		this(Arrays.asList(records));
	}
	
	public CMEPRecordSet(List<CMEPRecord> records) {
		this.cmepRecords = (records == null) ? new ArrayList<CMEPRecord>(0) : new ArrayList<CMEPRecord>(records);
	}
	
	public int getRecordCount() {
		return cmepRecords.size();
	}
	
	public CMEPRecord getRecord(int index) {
		return cmepRecords.get(index);
	}
	
	public CMEPRecord getFirstRecordWithNoParseException() {
		for (CMEPRecord record : cmepRecords) {
			if (record.getParseException() == null) {
				return record;
			}
		}
		return null;
	}
}
