package com.matthalstead.gasami.cmep.tasks;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;

import com.matthalstead.fileanalyzer.cmep.CMEPRecord;
import com.matthalstead.gasami.cmep.BillingGasCSVCreator;
import com.matthalstead.gasami.cmep.CMEPPullRunProperties;
import com.matthalstead.gasami.cmep.SFTPUtils;

public class ProcessGasCMEPTask implements Task {
	private static final Logger log = Logger.getLogger(ProcessGasCMEPTask.class);
	
	public List<String> getConfigErrors(CMEPPullRunProperties props) {
		List<String> errors = new ArrayList<String>();
		if (props == null) {
			errors.add("No properties file was defined.");
		} else {
			if (props.getDate() == null) {
				errors.add("Date is a required command-line argument.");
			}
	
			checkDirectory("CMEP output directory", props.getCmepOutputDirectory(), errors);
			checkDirectory("Billing file output directory", props.getBillingFileOutputDirectory(), errors);
		}
		
		return errors.isEmpty() ? null : errors;
	}
	
	private void checkDirectory(String dirDescription, File dir, List<String> errors) {
		if (dir == null) {
			errors.add(dirDescription + " is a required property.");
		} else {
			boolean exists = dir.exists();
			if (!exists) {
				exists = dir.mkdir();
				if (!exists) {
					errors.add(dirDescription + " " + dir.getAbsolutePath() + " does not exist and could not be created.");
				}
			}
			if (exists && !dir.isDirectory()) {
				errors.add(dirDescription + " " + dir.getAbsolutePath() + " is not a directory.");
			}
		}
	}
	
	public void run(CMEPPullRunProperties props) throws Exception {
		log.info("Processing Gas CMEP for " + new SimpleDateFormat("M/d/yyyy").format(props.getDate()) + " from " + props.getHeadEndServerName() + " (via " + props.getServerName() + ")");
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		SFTPUtils.fetchCMEP(props, baos);
		
		byte[] bAry = baos.toByteArray();
		baos = null; // May allow baos to be GC'd, as it's now a copy of bAry.  Not sure this is necessary but it doesn't hurt.
		
		boolean isGzip = ((bAry[0] == (byte)0x1f) && (bAry[1] == (byte)0x8b));
		ByteArrayInputStream bais = new ByteArrayInputStream(bAry);
		InputStream is = (isGzip) ? new GZIPInputStream(bais) : bais;
		
		File cmepFile = buildOutputFile(props.getCmepOutputDirectory(), props.getCmepOutputFilePattern(), props.getDate());
		log.info("Writing CMEP to " + cmepFile.getAbsolutePath() + "...");
		FileOutputStream fos = new FileOutputStream(cmepFile);
		long byteCount = 0;
		byte[] buf = new byte[4096];
		int read;
		while ((read = is.read(buf)) > 0) {
			fos.write(buf, 0, read);
			byteCount += (long) read;
		}
		fos.flush();
		fos.close();
		log.info("Done writing " + new DecimalFormat("#,##0").format(byteCount) + (byteCount == 1 ? " byte" : " bytes") + " to " + cmepFile.getAbsolutePath() + ".");
		
		bais = new ByteArrayInputStream(bAry);
		is = (isGzip) ? new GZIPInputStream(bais) : bais;
		File billingFile = buildOutputFile(props.getBillingFileOutputDirectory(), props.getBillingFileOutputFilePattern(), props.getDate());
		log.info("Writing billing file to " + billingFile.getAbsolutePath() + "...");
		fos = new FileOutputStream(billingFile);
		BillingGasCSVCreator csvCreator = new BillingGasCSVCreator(fos);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line;
		while ((line = br.readLine()) != null) {
			csvCreator.handleCMEPRecord(new CMEPRecord(line));
		}
		csvCreator.finish();
		fos.close();
		int numLines = csvCreator.getNumLinesWritten();
		log.info("Done writing " + new DecimalFormat("#,##0").format(numLines) + (numLines == 1 ? " line" : " lines") + " to " + billingFile.getAbsolutePath() + ".");
		log.info("Done processing Gas CMEP for " + new SimpleDateFormat("M/d/yyyy").format(props.getDate()) + ".");
		
	}
	
	private File buildOutputFile(File outputDir, String filenamePattern, Date cmepDate) {
		return new File(outputDir, new SimpleDateFormat(filenamePattern).format(cmepDate));
	}
	
}
