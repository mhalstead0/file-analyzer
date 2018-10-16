package com.matthalstead.fileanalyzer.swing;

import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.matthalstead.fileanalyzer.util.GBC;

public class CollapserPanel extends JPanel {

	private static final long serialVersionUID = 7121533710356585531L;

	private final JLabel twistyLabel;
	private final JLabel label;
	private final JPanel labelHolder;
	private final JPanel content;
	
	private boolean collapsed = false;
	
	public CollapserPanel(String label, JPanel content) {
		this.twistyLabel = new JLabel(getTwistyContent(false));
		this.label = new JLabel(label);
		this.labelHolder = buildLabelHolder();
		this.content = content;
		init();
		
	}
	
	private static String getTwistyContent(boolean collapsed) {
		return collapsed ? "[+]" : "[-]";
	}
	
	private JPanel buildLabelHolder() {
		JPanel result = new JPanel();
		result.setLayout(new GridBagLayout());
		GBC c = GBC.getDefault();
		result.add(this.twistyLabel, c);
		c.gridx++;
		result.add(this.label, c);
		c.gridx++;
		c.weightx *= 100.0;
		result.add(new JLabel(), c);
		return result;
	}
	
	private final void init() {
		twistyLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		twistyLabel.setFont(new Font("Courier New", Font.BOLD, 11));
		
		label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		Font defaultLabelFont = label.getFont();
		label.setFont(new Font(defaultLabelFont.getName(), Font.BOLD, defaultLabelFont.getSize()+1));
		
		setLayout(new GridBagLayout());
		GBC c = GBC.getDefault();

		c.anchor = GBC.NORTHWEST;
		c.fill = GBC.HORIZONTAL;
		add(labelHolder, c);
		
			
		c.gridy++;
		add(content, c);
		
		addListeners();
	}
	
	private void addListeners() {
		MouseAdapter ma = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				collapsed = !collapsed;
				twistyLabel.setText(getTwistyContent(collapsed));
				content.setVisible(!collapsed);
			}
		};
		labelHolder.addMouseListener(ma);
	}
	
	
}
