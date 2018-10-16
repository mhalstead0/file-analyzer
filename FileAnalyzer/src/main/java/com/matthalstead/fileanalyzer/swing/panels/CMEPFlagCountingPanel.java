package com.matthalstead.fileanalyzer.swing.panels;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.matthalstead.fileanalyzer.cmep.CMEPBitLookups;
import com.matthalstead.fileanalyzer.cmep.consumers.CMEPFlagConsumer;
import com.matthalstead.fileanalyzer.cmep.consumers.CMEPFlagConsumer.FlagCounter;
import com.matthalstead.fileanalyzer.swing.ConsumerPanel;
import com.matthalstead.fileanalyzer.swing.ConsumerPanelWithUpdate;
import com.matthalstead.fileanalyzer.swing.TextFieldWithLabel;

public class CMEPFlagCountingPanel extends ConsumerPanelWithUpdate<CMEPFlagConsumer> {

	private static final long serialVersionUID = 8120162403933483403L;

	// Don't initialize any fields here - they won't get populated before doInit
	
	private Map<String, FlagsPanel> unitsToFlagsPanel;
	
	private GridBagConstraints panelGBC;
	
	@Override
	public String getTitle() {
		return "CMEP Flag Counting";
	}
	
	@Override
	protected void doInit() {
		unitsToFlagsPanel = new HashMap<String, FlagsPanel>();
		
		setLayout(new GridBagLayout());
		
		GridBagConstraints c = getDefaultGBC();
		c.gridx = c.gridy = 0;
		c.gridwidth = c.gridheight = 1;
		c.weightx = c.weighty = 1.0;
		
		panelGBC = c;
		
		//setPreferredSize(new Dimension(400, 600));
		
	}
	
	@Override
	public List<SingleValue> getSingleValues() {
		
		SortedMap<String, FlagsPanel> unitsToFlagPanel = new TreeMap<String, FlagsPanel>(this.unitsToFlagsPanel);
		List<SingleValue> result = new ArrayList<ConsumerPanel.SingleValue>();
		for (Boolean useMeterCounts : Arrays.asList(new Boolean[] { false, true })) {
			for (Map.Entry<String, FlagsPanel> entry : unitsToFlagPanel.entrySet()) {
				String units = entry.getKey();
				FlagsPanel flagsPanel = entry.getValue();
				for (int i=0; i<flagsPanel.singleFlagPanels.length; i++) {
					SingleFlagPanel sfp = flagsPanel.singleFlagPanels[i];
					long flagMask = 1L << i;
					int count = (useMeterCounts) ? sfp.getMeterCount() : sfp.getFlagCount();
					result.add(new SingleValue(units + " " + (useMeterCounts ? "meters" : "flags") + " (0x" + Long.toHexString(flagMask) + ")", count));
				}
			}
		}
		return result;
	}
	
	@Override
	protected void handleUpdate(CMEPFlagConsumer consumer) {
		Map<String, FlagCounter[]> unitsToFlagCounterAry = consumer.getUnitsToFlagCounterArray();
		
		for (Map.Entry<String, FlagCounter[]> entry : unitsToFlagCounterAry.entrySet()) {
			String units = entry.getKey();
			FlagsPanel fp = unitsToFlagsPanel.get(units);
			if (fp == null) {
				fp = new FlagsPanel(units);
				unitsToFlagsPanel.put(units, fp);
				Dimension size = getSize();
				CMEPFlagCountingPanel.this.add(fp, panelGBC);
				panelGBC.gridy++;
				//fp.setSize(400, 500);
				fp.revalidate();
				fp.repaint();
				
				//System.out.println("Added FP! " + getPreferredSize());
				setSize(size.height + fp.getPreferredSize().height, size.width);
				//CMEPFlagCountingPanel.this.setPreferredSize(preferredSize)
				CMEPFlagCountingPanel.this.revalidate();
				CMEPFlagCountingPanel.this.repaint();
				
				
			}
			FlagCounter[] fcAry = entry.getValue();
			for (int i=0; i<fcAry.length; i++) {
				FlagCounter counter = fcAry[i];
				fp.updateCounts(i, counter.getFlagCount(), counter.getDistinctMeterCount(), counter.getMeters());
			}
		}
	}
	
	private static class FlagsPanel extends JPanel {
		private static final long serialVersionUID = 6631091022245733187L;
		private final String units;
		private SingleFlagPanel[] singleFlagPanels;
		public FlagsPanel(String units) {
			this.units = units;
			init();
		}
		private void init() {
			setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.gridx = c.gridy = 0;
			c.gridwidth = c.gridheight = 1;
			c.weightx = c.weighty = 1.0;
			
			JLabel unitsLabel = new JLabel(units);
			c.anchor = GridBagConstraints.WEST;
			c.fill = GridBagConstraints.HORIZONTAL;
			unitsLabel.setFont(new Font(null, Font.BOLD, 12));
			add(unitsLabel, c);
			c.gridy++;
			
			singleFlagPanels = new SingleFlagPanel[CMEPFlagConsumer.NUM_FLAGS];
			for (int i=0; i<CMEPFlagConsumer.NUM_FLAGS; i++) {
				singleFlagPanels[i] = new SingleFlagPanel(i);
				add(singleFlagPanels[i], c);
				c.gridy++;
			}
		}
		
		public void updateCounts(int flagIndex, int flagCount, int meterCount, Collection<String> meterIds) {
			SingleFlagPanel sfp = singleFlagPanels[flagIndex];
			sfp.setFlagCount(flagCount);
			sfp.setMeterCount(meterCount);
			sfp.setMeterIds(meterIds);
		}
	}

	private static class SingleFlagPanel extends JPanel {
		private static final long serialVersionUID = 5500058210726126657L;
		private final int index;
		private TextFieldWithLabel flagCountTFWL = new TextFieldWithLabel("Count:", "Total count of flags", "#,##0", false);
		private TextFieldWithLabel meterCountTFWL =  new TextFieldWithLabel("Distinct Meters:", "Count of distinct meters", "#,##0", true);
		
		private int flagCount = 0;
		private int meterCount = 0;
		
		public SingleFlagPanel(int index) {
			this.index = index;
			init();
		}
		private void init() {
			setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.gridx = c.gridy = 0;
			c.gridwidth = c.gridheight = 1;
			c.weightx = c.weighty = 1.0;
			
			c.anchor = GridBagConstraints.EAST;
			
			long flagValue = 1L << index;
			JLabel label = new JLabel("Flag 0x" + Long.toHexString(flagValue) + " (" + flagValue + ") ");
			Font defaultFont = label.getFont();
			label.setFont(new Font(defaultFont.getName(), Font.BOLD, defaultFont.getSize()));
			label.setToolTipText(CMEPBitLookups.getIntervalFlagDescription(index));
			add(label, c);
			
			c.gridx++;
			c.anchor = GridBagConstraints.WEST;
			add(flagCountTFWL, c);
			
			c.gridx++;
			add(meterCountTFWL, c);
		}
		
		private void setFlagCount(int i) {
			flagCountTFWL.setValue(i);
			flagCount = i;
			setVisible(i > 0);
		}
		private void setMeterCount(int i) {
			meterCountTFWL.setValue(i);
			meterCount = i;
		}
		private void setMeterIds(Collection<String> meterIds) {
			meterCountTFWL.setList(meterIds);
		}
		
		private int getFlagCount() {
			return flagCount;
		}
		
		private int getMeterCount() {
			return meterCount;
		}
		
	}
}
