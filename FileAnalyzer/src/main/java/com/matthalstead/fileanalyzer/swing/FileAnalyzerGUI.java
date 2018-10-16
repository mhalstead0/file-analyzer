package com.matthalstead.fileanalyzer.swing;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.log4j.Logger;

import com.matthalstead.fileanalyzer.util.ImageUtils;

public class FileAnalyzerGUI {
	
	private static final Logger log = Logger.getLogger(FileAnalyzerGUI.class);

	public static void main(String[] args) {
		try {
			log.debug("Starting up...");
			
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			
			
			final FileAnalyzerMainFrame frame = new FileAnalyzerMainFrame();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			//frame.setLocationRelativeTo(null);
			frame.setLocationByPlatform(true);
			
			ImageUtils.setupIcons(frame);
			
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
