package com.matthalstead.fileanalyzer.util;

import java.awt.Component;
import java.awt.Image;
import java.awt.Window;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JInternalFrame;

import org.apache.log4j.Logger;

import com.matthalstead.fileanalyzer.AppSettings;
import com.matthalstead.fileanalyzer.swing.FileAnalyzerGUI;

public class ImageUtils {
	
	private static final Logger log = Logger.getLogger(ImageUtils.class);
	
	private static List<Image> images = null;
	
	private static void buildIconsIfNecessary() {
		if (images == null) {
			try {
				final String prefix = AppSettings.class.getPackage().getName().replaceAll("\\.", "/") + "/images/";
				log.debug("Loading icons from " + prefix + "...");
				ClassLoader cl = FileAnalyzerGUI.class.getClassLoader();
				images = Collections.unmodifiableList(Arrays.asList(new Image[] {	
					ImageIO.read(cl.getResourceAsStream(prefix + "icon32.bmp")),
					ImageIO.read(cl.getResourceAsStream(prefix + "icon20.bmp")),
					ImageIO.read(cl.getResourceAsStream(prefix + "icon16.bmp"))
				}));
			} catch (IOException ioe) {
				throw new RuntimeException(ioe);
			}
		}
	}

	public static void setupIcons(Component c) {
		buildIconsIfNecessary();
		if (c instanceof Window) {
			((Window) c).setIconImages(images);
		} else if (c instanceof JInternalFrame) {
			
			((JInternalFrame) c).setFrameIcon(new ImageIcon(images.get(0)));
		}
	}
}
