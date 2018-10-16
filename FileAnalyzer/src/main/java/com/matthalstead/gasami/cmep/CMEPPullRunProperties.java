package com.matthalstead.gasami.cmep;

import java.io.File;
import java.io.FileInputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

public class CMEPPullRunProperties {
	
	private static final Logger log = Logger.getLogger(CMEPPullRunProperties.class);

	private final Properties props;
	private final long date;
	
	private CMEPPullRunProperties(Properties props, Date date) {
		this.props = props;
		this.date = (date == null) ? 0L : date.getTime();
	}
	
	public static CMEPPullRunProperties build(CMEPPullCommandLine cl) {
		File configFile = cl.getConfigFile();
		if (configFile == null || !configFile.exists()) {
			return null;
		}
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(configFile);
			log.debug("Loading properties from " + configFile.getAbsolutePath() + "...");
			Properties props = new Properties();
			props.load(fis);
			CMEPPullRunProperties result = new CMEPPullRunProperties(props, cl.getDate());
			return result;
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (fis != null) {
				try { fis.close(); } catch (Exception e) { /* ignore */ }
			}
		}
	}
	
	private Set<String> alreadyLoggedProps = new HashSet<String>();
	
	private String getProperty(String name, String defaultValue, boolean maskInLog) {
		String result = props.getProperty(name, defaultValue);
		if (!alreadyLoggedProps.contains(name)) {
			String toLog;
			if (result == null) {
				toLog = "[null]";
			} else {
				toLog = (maskInLog) ? "[********]" : "\"" + result + "\"";
			}
			log.debug("Value of property \"" + name + "\" is " + toLog + ".");
			
			Set<String> newSet = new HashSet<String>(alreadyLoggedProps);
			newSet.add(name);
			alreadyLoggedProps = newSet;
		}
		return result;
	}
	private String getProperty(String name) {
		return getProperty(name, null, false);
	}
	
	private String getPasswordProperty(String name) {
		String enc = getProperty(name, null, true);
		return PasswordUtils.decrypt(enc);
	}
	
	
	public boolean isFetchFromHeadEnd() {
		return getHeadEndServerName() != null;
	}
	
	public String getServerName() {
		return getProperty("server.name");
	}
	
	public String getServerUsername() {
		return getProperty("server.username");
	}
	
	public String getServerPassword() {
		return getPasswordProperty("server.password.enc");
	}
	
	public String getHeadEndServerName() {
		return getProperty("he.server.name");
	}
	
	public String getHeadEndServerUsername() {
		return getProperty("he.server.username");
	}
	
	public String getHeadEndServerPassword() {
		return getPasswordProperty("he.server.password.enc");
	}
	
	public File getCmepOutputDirectory() {
		String str = getProperty("cmep.output.dir");
		return (str == null) ? null : new File(str);
	}
	
	public File getBillingFileOutputDirectory() {
		String str = getProperty("billing.output.dir");
		return (str == null) ? null : new File(str);
	}
	
	public String getShellPromptPattern() {
		return getProperty("shell.prompt.pattern");
	}
	
	public String getSftpPromptPattern() {
		return getProperty("he.sftp.prompt.pattern");
	}
	
	public String getCmepOutputFilePattern() {
		return getProperty("cmep.output.file.pattern");
	}
	public String getBillingFileOutputFilePattern() {
		return getProperty("billing.output.file.pattern");
	}
	
	public Date getDate() {
		Date result = (date == 0L) ? null : new Date(date);
		log.debug("Date parameter: " + result);
		return result;
	}
	
	
	
}
