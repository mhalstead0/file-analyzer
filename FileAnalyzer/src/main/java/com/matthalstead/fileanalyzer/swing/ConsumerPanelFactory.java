package com.matthalstead.fileanalyzer.swing;

import com.matthalstead.fileanalyzer.InputStreamConsumer;
import com.matthalstead.fileanalyzer.consumerfactory.ConsumerDefinition;

public class ConsumerPanelFactory {

	public static ConsumerPanel buildConsumerPanel(ConsumerDefinition cd, InputStreamConsumer consumer) {
		try {
			String panelClassName = cd.getPanelClassName();
			Class<?> c = Class.forName(panelClassName);
			ConsumerPanel panel = (ConsumerPanel) c.newInstance();
			panel.setConsumer(consumer);
			return panel;
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
