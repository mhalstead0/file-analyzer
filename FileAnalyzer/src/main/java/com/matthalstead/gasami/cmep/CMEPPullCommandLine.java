package com.matthalstead.gasami.cmep;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import com.matthalstead.gasami.cmep.tasks.TaskFactory;

public class CMEPPullCommandLine {
	
	private final CommandLine cl;
	
	private static final char OPT_CONF_FILE = 'c';
	private static final char OPT_DATE = 'd';
	
	private CMEPPullCommandLine(CommandLine cl) {
		this.cl = cl;
	}
	
	public String getTaskName() {
		List<?> argsList = cl.getArgList();
		if (argsList == null || argsList.isEmpty()) {
			return null;
		} else {
			return (String) argsList.get(0);
		}
	}
	
	public File getConfigFile() {
		String filename = cl.getOptionValue(OPT_CONF_FILE);
		if (filename == null) {
			return null;
		} else {
			return new File(filename);
		}
	}
	
	public Date getDate() {
		String dateStr = cl.getOptionValue(OPT_DATE);
		if (dateStr == null && cl.getArgList() != null && cl.getArgList().size() >= 2) {
			dateStr = (String) cl.getArgList().get(1);
		}
		if (dateStr == null) {
			return null;
		} else {
			Date result = parseSpecialDateValue(dateStr);
			if (result == null) {
				result = attemptToParseDate(dateStr, 
						"yyyy-MM-dd",
						"M/d/yyyy",
						"M/d/yy"
				);
			}
			return result;
		}
	}
	
	private Date parseSpecialDateValue(String str) {
		if (str == null) {
			return null;
		}
		
		Calendar c = Calendar.getInstance();
		int todayYear = c.get(Calendar.YEAR);
		int todayMonth = c.get(Calendar.MONTH);
		int todayDay = c.get(Calendar.DAY_OF_MONTH);
		c.clear();
		c.set(Calendar.YEAR, todayYear);
		c.set(Calendar.MONTH, todayMonth);
		c.set(Calendar.DAY_OF_MONTH, todayDay);
		
		if ("today".equalsIgnoreCase(str)) {
			return c.getTime();
		} else if ("yesterday".equalsIgnoreCase(str)) {
			c.add(Calendar.DAY_OF_MONTH, -1);
			return c.getTime();
		}
		
		int targetDayOfWeek;
		if ("sunday".equalsIgnoreCase(str) || "sun".equalsIgnoreCase(str)) {
			targetDayOfWeek = Calendar.SUNDAY;
		} else if ("monday".equalsIgnoreCase(str) || "mon".equalsIgnoreCase(str)) {
			targetDayOfWeek = Calendar.MONDAY;
		} else if ("tuesday".equalsIgnoreCase(str) || "tue".equalsIgnoreCase(str) || "tues".equalsIgnoreCase(str)) {
			targetDayOfWeek = Calendar.TUESDAY;
		} else if ("wednesday".equalsIgnoreCase(str) || "wed".equalsIgnoreCase(str)) {
			targetDayOfWeek = Calendar.WEDNESDAY;
		} else if ("thursday".equalsIgnoreCase(str) || "thu".equalsIgnoreCase(str) || "thurs".equalsIgnoreCase(str)) {
			targetDayOfWeek = Calendar.THURSDAY;
		} else if ("friday".equalsIgnoreCase(str) || "fri".equalsIgnoreCase(str)) {
			targetDayOfWeek = Calendar.FRIDAY;
		} else if ("saturday".equalsIgnoreCase(str) || "sat".equalsIgnoreCase(str)) {
			targetDayOfWeek = Calendar.SATURDAY;
		} else {
			return null;
		}
		
		c.add(Calendar.DAY_OF_MONTH, -1);
		while (c.get(Calendar.DAY_OF_WEEK) != targetDayOfWeek) {
			c.add(Calendar.DAY_OF_MONTH, -1);
		}
		return c.getTime();
	}
	
	private static Date attemptToParseDate(String dateStr, String ... formats) {
		for (int i=0; i<formats.length; i++) {
			SimpleDateFormat sdf = new SimpleDateFormat(formats[i]);
			sdf.setLenient(false);
			try {
				return sdf.parse(dateStr);
			} catch (Exception e) {
				// ignore
			}
		}
		throw new RuntimeException("Could not parse \"" + dateStr + "\" with any of the formats: " + Arrays.asList(formats));
	}
	
	public boolean isValid() {
		List<String> errors = getErrors();
		return (errors == null) || (errors.isEmpty());
	}
	
	public List<String> getErrors() {
		List<String> result = new ArrayList<String>();
		String taskName = getTaskName();
		if (taskName == null) {
			result.add("Task name not specified");
		} else if (TaskFactory.getTaskDef(taskName) == null) {
			result.add("No task named \"" + taskName + "\" found");
		} else {
			
		}
		return result.isEmpty() ? null : result;
	}

	public static CMEPPullCommandLine parseCommandLine(String[] args) throws ParseException {
		CommandLine cl = new PosixParser().parse(buildOptions(), args);
		return new CMEPPullCommandLine(cl);
	}
	
	public static Options buildOptions() {
		Options opts = new Options();
		opts.addOption(buildOption(OPT_CONF_FILE, "conf", true, "Configuration properties file"));
		opts.addOption(buildOption(OPT_DATE, "date", true, "Date to pull (date as yyyy-MM-dd or M/d/yyyy; other acceptable values include today, yesterday, sunday, monday, etc.)"));
		return opts;
	}
	
	private static Option buildOption(char shortName, String longName, boolean hasOption, String description) {
		return new Option("" + shortName, longName, hasOption, description);
	}
}
