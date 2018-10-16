package com.matthalstead.gasami.cmep.tasks;

import java.util.List;

import com.matthalstead.gasami.cmep.CMEPPull;
import com.matthalstead.gasami.cmep.CMEPPullRunProperties;

public class PrintHelpTask implements Task {
	public List<String> getConfigErrors(CMEPPullRunProperties props) {
		return null;
	}
	public void run(CMEPPullRunProperties props) throws Exception {
		CMEPPull.printHelp(System.out);
	}
}
