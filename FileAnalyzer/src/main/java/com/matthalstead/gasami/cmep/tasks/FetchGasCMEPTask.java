package com.matthalstead.gasami.cmep.tasks;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;

import com.matthalstead.gasami.cmep.CMEPPullRunProperties;
import com.matthalstead.gasami.cmep.SFTPUtils;

public class FetchGasCMEPTask implements Task {
	private static final Logger log = Logger.getLogger(FetchGasCMEPTask.class);
	
	public List<String> getConfigErrors(CMEPPullRunProperties props) {
		List<String> errors = new ArrayList<String>();
		if (props == null) {
			errors.add("No properties file was defined.");
		} else {
			if (props.getDate() == null) {
				errors.add("Date is a required command-line argument.");
			}
			
			File outputDir = props.getCmepOutputDirectory();
			if (outputDir == null) {
				errors.add("CMEP Output directory is a required property.");
			} else {
				boolean exists = outputDir.exists();
				if (!exists) {
					exists = outputDir.mkdir();
					if (!exists) {
						errors.add("CMEP Output directory " + outputDir.getAbsolutePath() + " does not exist and could not be created.");
					}
				}
				if (exists && !outputDir.isDirectory()) {
					errors.add(outputDir.getAbsolutePath() + " is not a directory.");
				}
			}
		}
		
		return errors.isEmpty() ? null : errors;
	}
	public void run(CMEPPullRunProperties props) throws Exception {
		log.info("Fetching Gas CMEP for " + new SimpleDateFormat("M/d/yyyy").format(props.getDate()) + " from " + props.getHeadEndServerName() + " (via " + props.getServerName() + ")");
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		SFTPUtils.fetchCMEP(props, baos);
		
		byte[] bAry = baos.toByteArray();
		baos = null; // May allow baos to be GC'd, as it's now a copy of bAry.  Not sure this is necessary but it doesn't hurt.
		
		boolean isGzip = ((bAry[0] == (byte)0x1f) && (bAry[1] == (byte)0x8b));
		ByteArrayInputStream bais = new ByteArrayInputStream(bAry);
		InputStream is = (isGzip) ? new GZIPInputStream(bais) : bais;
		
		bAry = null; // May allow bAry to be GC'd, as has now been cloned in bais.  Not sure this is necessary but it doesn't hurt.
		
		File f = buildCMEPOutputFile(props.getCmepOutputDirectory(), props.getCmepOutputFilePattern(), props.getDate());
		log.info("Writing CMEP to " + f.getAbsolutePath() + "...");
		FileOutputStream fos = new FileOutputStream(f);
		long byteCount = 0;
		byte[] buf = new byte[4096];
		int read;
		while ((read = is.read(buf)) > 0) {
			fos.write(buf, 0, read);
			byteCount += (long) read;
		}
		fos.flush();
		fos.close();
		log.info("Done writing " + new DecimalFormat("#,##0").format(byteCount) + (byteCount == 1 ? " byte" : " bytes") + " to " + f.getAbsolutePath() + ".");
		log.info("Done fetching Gas CMEP for " + new SimpleDateFormat("M/d/yyyy").format(props.getDate()) + ".");

	}
	
	private File buildCMEPOutputFile(File outputDir, String filenamePattern, Date cmepDate) {
		return new File(outputDir, new SimpleDateFormat(filenamePattern).format(cmepDate));
	}
}
