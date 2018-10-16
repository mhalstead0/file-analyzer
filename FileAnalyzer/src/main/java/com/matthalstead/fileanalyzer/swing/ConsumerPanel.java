package com.matthalstead.fileanalyzer.swing;

import java.awt.GridBagConstraints;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTextField;

import com.matthalstead.fileanalyzer.InputStreamConsumer;

public abstract class ConsumerPanel extends JPanel {

	private static final long serialVersionUID = 7121533710356585531L;

	protected InputStreamConsumer consumer;
	
	public ConsumerPanel() {
		init();
	}
	
	private final void init() {
		doInit();
	}
	
	protected abstract void doInit();
	public abstract void handleConsumerUpdate();
	public abstract String getTitle();
	public abstract List<SingleValue> getSingleValues();
	
	public void setConsumer(InputStreamConsumer consumer) {
		this.consumer = consumer;
	}
	
	protected GridBagConstraints getDefaultGBC() {
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = c.gridy = 0;
		c.weightx = c.weighty = 1.0;
		c.gridwidth = c.gridheight = 1;
		return c;
	}
	
	protected JTextField getUneditableTextField(int columns) {
		JTextField result = new JTextField(columns);
		result.setEditable(false);
		return result;
	}
	
	
	public static class SingleValue {
		public final String key;
		public final long value;
		public SingleValue(String key, long value) {
			this.key = key;
			this.value = value;
		}
	}
	
}
