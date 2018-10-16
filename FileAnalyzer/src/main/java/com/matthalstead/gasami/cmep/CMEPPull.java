package com.matthalstead.gasami.cmep;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import com.matthalstead.gasami.cmep.tasks.Task;
import com.matthalstead.gasami.cmep.tasks.TaskFactory;
import com.matthalstead.gasami.cmep.tasks.TaskFactory.TaskDef;

public class CMEPPull {

	public static void main(String[] args) {
		try {			
			CMEPPullCommandLine cl;
			try {
				cl = CMEPPullCommandLine.parseCommandLine(args);
			} catch (Exception e) {
				printCommandLine(args);
				e.printStackTrace();
				printHelp();
				System.exit(1);
				return;
			}
			
			if (!cl.isValid()) {
				List<String> errors = cl.getErrors();
				if (errors != null) {
					for (String error : errors) {
						System.err.println(error);
					}
				}
				printCommandLine(args);
				printHelp();
				System.exit(1);
				return;
			}
			
			Task task = TaskFactory.buildTask(cl.getTaskName());
			CMEPPullRunProperties props = CMEPPullRunProperties.build(cl);
			List<String> errors = task.getConfigErrors(props);
			if (errors != null && !errors.isEmpty()) {
				for (String error : errors) {
					System.err.println(error);
				}
				printCommandLine(args);
				printHelp();
				System.exit(1);
			} else {
				task.run(props);
			}
			
			
		} catch (Exception e) {
			printCommandLine(args);
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	private static void printCommandLine(String[] args) {
		StringBuilder sb = new StringBuilder("Executed: " + CMEPPull.class.getName());
		for (int i=0; i<args.length; i++) {
			sb.append(" " + args[i]);
		}
		System.err.println(sb);
	}
	
	public static void printHelp() {
		printHelp(System.err);
	}
	
	public static void printHelp(PrintStream ps) {
		HelpFormatter hf = new HelpFormatter();
		Options o = CMEPPullCommandLine.buildOptions();
		String commandLine = CMEPPull.class.getName() + " [options] <taskname> [date]";
		String header = "Options:";
		String footer = "Tasks:";
		for (TaskDef taskDef : TaskFactory.getAllTaskDefs()) {
			footer += System.getProperty("line.separator") + "- " + taskDef.name + ": " + taskDef.description;
		}
		boolean autogenHeader = false;
		PrintWriter pw = new PrintWriter(ps);
		hf.printHelp(pw, 80, commandLine, header, o, 2, 2, footer, autogenHeader);
		pw.flush();

	}
	
}
