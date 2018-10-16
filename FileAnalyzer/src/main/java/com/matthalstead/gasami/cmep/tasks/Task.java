package com.matthalstead.gasami.cmep.tasks;

import java.util.List;

import com.matthalstead.gasami.cmep.CMEPPullRunProperties;

public interface Task {


	public List<String> getConfigErrors(CMEPPullRunProperties props);
	
	public void run(CMEPPullRunProperties props) throws Exception;
	
}
