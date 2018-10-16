package com.matthalstead.fileanalyzer.swing;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class GUITestClass {

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			final FileAnalyzerMainFrame frame = new FileAnalyzerMainFrame();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			
			frame.setVisible(true);
			
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					frame.addFileFrame(new FileAnalyzerFrame());
				}
			});
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
