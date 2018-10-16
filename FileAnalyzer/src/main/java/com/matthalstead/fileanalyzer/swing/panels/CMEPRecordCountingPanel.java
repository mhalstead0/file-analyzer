package com.matthalstead.fileanalyzer.swing.panels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.matthalstead.fileanalyzer.cmep.consumers.CMEPRecordSetCountConsumer;
import com.matthalstead.fileanalyzer.swing.ConsumerPanel;

public class CMEPRecordCountingPanel extends ConsumerPanel {

	private static final long serialVersionUID = 8120162403933483403L;

	// Don't initialize any fields here - they won't get populated before doInit
	private JTextField recordSetCountText;
	private JTextField recordCountText;
	private JTextField meterCountText;
	private JTextField unitRecordCountText;
	private JTextField unitIntervalCountText;
	
	private Runnable updateRunner;
	
	@Override
	public String getTitle() {
		return "CMEP Record Counting";
	}
	
	@Override
	protected void doInit() {
		updateRunner = buildUpdateRunner();
		
		setLayout(new GridBagLayout());
		recordSetCountText = getUneditableTextField(10);
		recordCountText = getUneditableTextField(10);
		meterCountText = getUneditableTextField(10);
		unitRecordCountText = getUneditableTextField(10);
		unitIntervalCountText = getUneditableTextField(10);
		
		GridBagConstraints c = getDefaultGBC();
		c.anchor = GridBagConstraints.EAST;
		add(new JLabel("Record Set Count:"), c);
		c.gridy++;
		add(new JLabel("Record Count:"), c);
		c.gridy++;
		add(new JLabel("Distinct Meter Count:"), c);
		c.gridy++;
		add(new JLabel("Records By Unit:"), c);
		c.gridy++;
		add(new JLabel("Intervals By Unit:"), c);
		
		c.anchor = GridBagConstraints.WEST;
		c.gridx++;
		c.gridy = 0;
		c.weightx = 2.0;
		add(recordSetCountText, c);
		c.gridy++;
		add(recordCountText, c);
		c.gridy++;
		add(meterCountText, c);
		c.gridy++;
		add(unitRecordCountText, c);
		c.gridy++;
		add(unitIntervalCountText, c);
		
	}
	
	private Runnable buildUpdateRunner() {
		return new Runnable() {
			public void run() {
				final CMEPRecordSetCountConsumer consumer = (CMEPRecordSetCountConsumer) CMEPRecordCountingPanel.super.consumer;
				recordSetCountText.setText(formatNumber(consumer.getRecordSetCount()));
				recordCountText.setText(formatNumber(consumer.getRecordCount()));
				meterCountText.setText(formatNumber(consumer.getDistinctMeterIdCount()));

				unitRecordCountText.setText(getUnitToCountContent(consumer.getRecordCountsByUnits()));
				unitIntervalCountText.setText(getUnitToCountContent(consumer.getIntervalCountsByUnits()));
			}
		};
	}

	@Override
	public List<SingleValue> getSingleValues() {
		final CMEPRecordSetCountConsumer consumer = (CMEPRecordSetCountConsumer) CMEPRecordCountingPanel.super.consumer;
		List<SingleValue> result = new ArrayList<SingleValue>();
		result.add(new SingleValue("Record Sets", consumer.getRecordSetCount()));
		result.add(new SingleValue("Records", consumer.getRecordCount()));
		result.add(new SingleValue("Distinct Meters", consumer.getDistinctMeterIdCount()));
		for (Map.Entry<String, Integer> entry : consumer.getRecordCountsByUnits().entrySet()) {
			result.add(new SingleValue("Record Counts (" + entry.getKey() + ")", entry.getValue()));
		}
		for (Map.Entry<String, Integer> entry : consumer.getIntervalCountsByUnits().entrySet()) {
			result.add(new SingleValue("Interval Counts (" + entry.getKey() + ")", entry.getValue()));
		}
		
		return result;
	}
	

	private String getUnitToCountContent(Map<String, Integer> map) {
		boolean first = true;
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, Integer> entry : map.entrySet()) {
			if (!first) {
				sb.append("; ");
			}
			sb.append(entry.getKey() + ": " + formatNumber(entry.getValue()));
			first = false;
		}
		String content = sb.toString();
		return content;
	}

	@Override
	public void handleConsumerUpdate() {
		SwingUtilities.invokeLater(updateRunner);
	}
	
	private static final Object DF_LOCK = new Object();
	private static final DecimalFormat DF = new DecimalFormat("#,##0");
	private static String formatNumber(int n) {
		synchronized(DF_LOCK) {
			return DF.format(n);
		}
	}

}
