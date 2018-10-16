package com.matthalstead.fileanalyzer.filefetch;

import java.io.InputStream;
import java.util.Calendar;

import javax.xml.bind.JAXB;

public class FileFetchManager {

	public static LoggedCMEPFileFetcher getCMEPFileFetcher(String profileName, Calendar c) {
		try {
			String dirName = "" + FileFetchManager.class.getPackage().getName().replaceAll("\\.", "/") + "/profiles";
			String fileName = profileName + ".xml";
			String fqn = dirName + "/" + fileName;
			InputStream is = FileFetchManager.class.getClassLoader().getResourceAsStream(fqn);
			if (is == null) {
				throw new RuntimeException("No profile \"" + profileName + "\" found (could not find file " + fqn + ")");
			}
			LoggedCMEPFileFetcher result = (LoggedCMEPFileFetcher) JAXB.unmarshal(is, LoggedCMEPFileFetcher.class);

			result.setFileYear(c.get(Calendar.YEAR));
			result.setFileMonth((c.get(Calendar.MONTH) - Calendar.JANUARY) + 1);
			result.setFileDay(c.get(Calendar.DAY_OF_MONTH));
			return result;
			
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
