package com.matthalstead.fileanalyzer.util;

import java.awt.GridBagConstraints;

public class GBC extends GridBagConstraints {

	private static final long serialVersionUID = -2999714690180889611L;

	private GBC() {
		
	}
	
	public static GBC getDefault() {
		GBC c = new GBC();
		c.gridx = c.gridy = 0;
		c.gridwidth = c.gridheight = 1;
		c.weightx = c.weighty = 1.0;
		return c;
	}
}
