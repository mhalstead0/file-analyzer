package com.matthalstead.fileanalyzer.cmep.consumers;

import com.matthalstead.fileanalyzer.cmep.CMEPRecord;

public abstract class AbstractMeterGroupCMEPConsumer extends AbstractGroupedCMEPConsumer {

	@Override
	protected boolean inSameSet(CMEPRecord r1, CMEPRecord r2) {
		return areStringsEqual(r1.getMeterId(), r2.getMeterId())
				&& (r1.getIntervalLengthMinutes() == r2.getIntervalLengthMinutes())
				&& doesUnitBaseMatch(r1.getUnits(), r2.getUnits());
	}
	
	private boolean doesUnitBaseMatch(String str1, String str2) {
		if (str1 == null || str2 == null) {
			return str1 == str2;
		} else {
			str1 = str1.toUpperCase();
			str2 = str2.toUpperCase();
			final String REG = "REG";
			if (!str1.endsWith(REG)) {
				str1 += REG;
			}
			if (!str2.endsWith(REG)) {
				str2 += REG;
			}
			return str1.equals(str2);
		}
	}

}
