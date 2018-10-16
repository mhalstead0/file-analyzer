package com.matthalstead.gasami.cmep.tasks;

import java.util.List;
import java.util.Scanner;

import com.matthalstead.gasami.cmep.CMEPPullRunProperties;
import com.matthalstead.gasami.cmep.PasswordUtils;

public class EncodePasswordTask implements Task {
	public List<String> getConfigErrors(CMEPPullRunProperties props) {
		return null;
	}
	
	public void run(CMEPPullRunProperties props) throws Exception {
		try (Scanner scanner = new Scanner(System.in)) {
			System.out.println("Enter the password:");
			String pw = scanner.nextLine();
			System.out.println("...encrypting....");
			String enc = PasswordUtils.encrypt(pw);
			System.out.println("Encrypted password:");
			System.out.println(enc);
		}
	}
}
