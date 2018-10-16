package com.matthalstead.gasami.cmep;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

public class SFTPUtils {
	
	private static final Logger log = Logger.getLogger(SFTPUtils.class);

	public static void fetchCMEP(CMEPPullRunProperties props, OutputStream os) throws Exception {
		if (props.isFetchFromHeadEnd()) {
			log.info("Logging into " + props.getServerName() + " as " + props.getServerUsername() + "...");
			Session session = getSession(props.getServerName(), props.getServerUsername(), props.getServerPassword());
			
			ByteArrayOutputStream shellBuffer = new ByteArrayOutputStream();
			PipedOutputStream commandOS = new PipedOutputStream();
			
			log.info("Opening SSH channel...");
			ChannelShell sshChannel = getSSHChannel(session, new PipedInputStream(commandOS), shellBuffer);
			
			String tempFile = getTempFilename();
			
			log.info("Fetching CMEP onto the server...");
			fetchCMEPFromHeadEndToServer(props, tempFile, commandOS, shellBuffer);
			
			log.info("Opening SFTP channel...");
			ChannelSftp sftpChannel = getSftpChannel(session);
			
			log.info("Fetching file...");
			sftpChannel.get(tempFile, os);
			
			log.info("Deleting temp file...");
			shellBuffer.reset();
			runCommand("rm " + tempFile, commandOS, shellBuffer, props.getShellPromptPattern(), false);
			
			log.info("Closing SFTP channel...");
			sftpChannel.disconnect();
			
			log.info("Exiting SSH channel...");
			commandOS.write("exit\n".getBytes());
			sshChannel.disconnect();
			
			log.info("Closing session...");
			session.disconnect();
			
		} else {
			throw new RuntimeException("???");
		}
	}
	
	private static void fetchCMEPFromHeadEndToServer(CMEPPullRunProperties props, String tempFile, OutputStream commandOS, ByteArrayOutputStream shellBuffer) throws Exception {
		
		waitUntilBufferMatchesPattern(shellBuffer, props.getShellPromptPattern());
		shellBuffer.reset();
		
		runCommand("sftp " + props.getHeadEndServerUsername() + "@" + props.getHeadEndServerName(), commandOS, shellBuffer, ".*password:.*", false);
		shellBuffer.reset();
		
		runCommand(props.getHeadEndServerPassword(), commandOS, shellBuffer, props.getSftpPromptPattern(), true);
		shellBuffer.reset();
		runCommand("cd delivery_files", commandOS, shellBuffer, props.getSftpPromptPattern(), false);
		shellBuffer.reset();
		
		String cmepFilePattern = getCMEPFilePattern(props);
		runCommand("get " + cmepFilePattern + " " + tempFile, commandOS, shellBuffer, props.getSftpPromptPattern(), false);
		shellBuffer.reset();
		
		runCommand("quit", commandOS, shellBuffer, props.getShellPromptPattern(), false);
		
		
	}
	
	private static void runCommand(String command, OutputStream commandOS, ByteArrayOutputStream resultBuffer, String endPattern, boolean maskLog) throws Exception {
		log.info("Sending command " + (maskLog ? "********" : command) + "...");
		commandOS.write((command + "\n").getBytes());
		waitUntilBufferMatchesPattern(resultBuffer, endPattern);
	}
	
	private static void waitUntilBufferMatchesPattern(ByteArrayOutputStream baos, String pattern) throws Exception {
		long startTime = System.currentTimeMillis();
		Pattern p = Pattern.compile(pattern, Pattern.MULTILINE | Pattern.DOTALL);
		log.debug("Waiting until buffer matches /" + pattern + "/...");
		int count = 0;
		while (!p.matcher(baos.toString()).matches()) {
			Thread.sleep(500L);
			count++;
			if (count > 10) {
				log.debug("Current buffer: " + baos.toString());
				count = 0;
			}
		}
		
		long stopTime = System.currentTimeMillis();
		log.debug("...pattern found after " + (stopTime - startTime) + "ms");
	}
	
	private static String getCMEPFilePattern(CMEPPullRunProperties props) {
		return "CMEPI*Gas." + new SimpleDateFormat("yyyyMMdd").format(props.getDate()) + "*";
	}
	
	private static String getTempFilename() {
		return "tmp_" + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
	}
	

	private static Session getSession(String serverName, String username, String password) throws Exception {
		JSch jsch = new JSch();
		Session session = jsch.getSession(username, serverName);
		session.setUserInfo(buildUserInfo(username, password));
		Properties config = new Properties();
		config.put("compression.s2c", "zlib,none");
		config.put("compression.c2s", "zlib,none");
		session.setConfig(config);
		session.connect();
		return session;
	}
	
	private static ChannelShell getSSHChannel(Session session, InputStream is, OutputStream os) throws Exception {
		ChannelShell channel = (ChannelShell) session.openChannel("shell");
		channel.setExtOutputStream(System.err);
		channel.setInputStream(is);
		channel.setOutputStream(os);
		channel.connect();
		return channel;
	}
	
	private static ChannelSftp getSftpChannel(Session session) throws Exception {
		ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
		channel.setExtOutputStream(System.err);
		channel.connect();
		return channel;
	}
	
	private static UserInfo buildUserInfo(final String username, final String password) {
		UserInfo ui = new UserInfo() {
			
			boolean triedOnce = false;

			public String getPassphrase() {
				return null;
			}

			public String getPassword() {
				if (triedOnce) {
					throw new RuntimeException("Already tried to log in as " + username + "!");
				}
				triedOnce = true;
				return password;
			}

			public boolean promptPassphrase(String msg) {
				log.debug(msg + ":");
				return false;
			}

			public boolean promptPassword(String msg) {
				log.debug(msg + ":");
				return !triedOnce;
			}

			public boolean promptYesNo(String msg) {
				log.debug(msg + " (y/n):");
				return true;
			}

			public void showMessage(String msg) {
				log.debug(msg);
			}

		};
		return ui;
	}
}
