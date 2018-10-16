package com.matthalstead.gasami.cmep.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class TaskFactory {

	public static final String TASKNAME_ENCPASS = "encpass";
	public static final String TASKNAME_HELP = "help";
	public static final String TASKNAME_FETCH = "fetch";
	public static final String TASKNAME_PROCESS = "process";

	private static final List<TaskDef> taskDefs = Arrays.asList(new TaskDef[] {
			new TaskDef(TASKNAME_ENCPASS, EncodePasswordTask.class, "Encode a password for the configuration file"),
			new TaskDef(TASKNAME_HELP, PrintHelpTask.class, "Print this help message"),
			new TaskDef(TASKNAME_FETCH, FetchGasCMEPTask.class, "Fetch a gas CMEP file"),
			new TaskDef(TASKNAME_PROCESS, ProcessGasCMEPTask.class, "Process a gas CMEP file (create a billing and an archive file)"),
	});
	
	private static final Map<String, TaskDef> taskDefMap = mapTaskDefs(taskDefs);
	
	private static final Map<String, TaskDef> mapTaskDefs(List<TaskDef> taskDefs) {
		SortedMap<String, TaskDef> result = new TreeMap<String, TaskDef>();
		for (TaskDef td : taskDefs) {
			result.put(td.name.toLowerCase(), td);
		}
		return Collections.unmodifiableSortedMap(result);
	}
	
	public static List<TaskDef> getAllTaskDefs() {
		return new ArrayList<TaskDef>(taskDefMap.values());
	}
	
	public static TaskDef getTaskDef(String taskName) {
		if (taskName == null) {
			return null;
		}
		String lowerCaseTaskName = taskName.toLowerCase();
		TaskDef td = taskDefMap.get(lowerCaseTaskName);
		return td;
	}
	
	public static Task buildTask(String taskName) {
		if (taskName == null) {
			throw new RuntimeException("No task name specified");
		}
		TaskDef td = getTaskDef(taskName); 
		if (td == null) {
			throw new RuntimeException("Could not find a task named \"" + taskName + "\"");
		}
		try {
			return td.implementationClass.newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Error creating task \"" + taskName + "\"", e);
		}
	}
	
	
	public static class TaskDef {
		public final String name;
		private final Class<? extends Task> implementationClass;
		public final String description;
		
		private TaskDef(String name, Class<? extends Task> implementationClass, String description) {
			this.name = name;
			this.implementationClass = implementationClass;
			this.description = description;
		}
	}
}
