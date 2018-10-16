package com.matthalstead.fileanalyzer.swing.panels;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.matthalstead.fileanalyzer.cmep.consumers.CMEPDataIssuesConsumer;
import com.matthalstead.fileanalyzer.swing.ConsumerPanelWithUpdate;
import com.matthalstead.fileanalyzer.swing.TextFieldWithLabel;

public class CMEPDataIssuesPanel extends ConsumerPanelWithUpdate<CMEPDataIssuesConsumer> {

	private static final long serialVersionUID = 8120162403933483403L;

	private TextFieldWithLabel multipleRegisterRecordsText;
	private TextFieldWithLabel regDropText;
	private TextFieldWithLabel negativeIntervalText;
	private TextFieldWithLabel largeIntervalText;
	private TextFieldWithLabel misalignedTimestampText;
	private TextFieldWithLabel sumCheckText;
	private TextFieldWithLabel intervalsExceedRegisterDiffText;
	private TextFieldWithLabel sameRegistersText;
	
	private TextFieldWithLabel[] getTFWLs() {
		return new TextFieldWithLabel[] {
				multipleRegisterRecordsText,
				regDropText,
				negativeIntervalText,
				largeIntervalText,
				misalignedTimestampText,
				sumCheckText,
				intervalsExceedRegisterDiffText,
				sameRegistersText
		};
	}
	
	
	@Override
	public String getTitle() {
		return "CMEP Data Issues";
	}
	
	@Override
	protected void doInit() {
		multipleRegisterRecordsText = new TextFieldWithLabel("Multiple Register Records:", "Meters with multiple register records", "#,##0", true);
		regDropText = new TextFieldWithLabel("Register Drop:", "Meters with stop register < start register", "#,##0", true);
		negativeIntervalText = new TextFieldWithLabel("Negative Intervals:", "Meters with (non-missing) negative intervals", "#,##0", true);
		largeIntervalText = new TextFieldWithLabel("High Intervals:", "Meters intervals > 99kWh", "#,##0", true);
		misalignedTimestampText = new TextFieldWithLabel("Misaligned Timestamps:", "Meters with interval timestamps that do not match register timestamps", "#,##0", true);
		sumCheckText = new TextFieldWithLabel("Interval Sum Check:", "Cases where intervals do not add up to the register difference", "#,##0", true);
		intervalsExceedRegisterDiffText = new TextFieldWithLabel("Intervals Exceed Reg Diff:", "Cases where there are missing intervals but the intervals sum to more than the register difference", "#,##0", true);
		sameRegistersText = new TextFieldWithLabel("No Distance Between Registers:", "Cases where both registers have the same timestamp", "#,##0", true);
		
		setLayout(new GridBagLayout());
		
		GridBagConstraints c = getDefaultGBC();
		c.gridx = c.gridy = 0;
		c.gridwidth = c.gridheight = 1;
		c.weightx = c.weighty = 1.0;
		
		add(multipleRegisterRecordsText, c);

		c.gridy++;
		add(regDropText, c);

		c.gridy++;
		add(negativeIntervalText, c);

		c.gridy++;
		add(largeIntervalText, c);

		c.gridy++;
		add(misalignedTimestampText, c);

		c.gridy++;
		add(sumCheckText, c);
		
		c.gridy++;
		add(intervalsExceedRegisterDiffText, c);
		
		c.gridy++;
		add(sameRegistersText, c);
		
		
	}
	
	@Override
	public List<SingleValue> getSingleValues() {
		DecimalFormat df = new DecimalFormat("#,##0");
		List<SingleValue> result = new ArrayList<SingleValue>();
		TextFieldWithLabel[] tfwls = getTFWLs();
		for (int i=0; i<tfwls.length; i++) {
			TextFieldWithLabel tfwl = tfwls[i];
			String labelText = tfwl.getLabelText().replaceAll("\\:", "");
			try {
				result.add(new SingleValue(labelText, df.parse(tfwl.getText()).longValue()));
			} catch (Exception e) {
				result.add(new SingleValue(labelText, 0));
			}
		}
		return result;
	}
	
	@Override
	protected void handleUpdate(CMEPDataIssuesConsumer consumer) {
		multipleRegisterRecordsText.setInfoFromCounter(consumer.getMultipleRegRecordsCounter());
		regDropText.setInfoFromCounter(consumer.getRegisterDropCounter());
		negativeIntervalText.setInfoFromCounter(consumer.getNegativeIntervalCounter());
		largeIntervalText.setInfoFromCounter(consumer.getHighIntervalCounter());
		misalignedTimestampText.setInfoFromCounter(consumer.getMisalignedTimestampCounter());
		sumCheckText.setInfoFromCounter(consumer.getSumCheckCounter());
		intervalsExceedRegisterDiffText.setInfoFromCounter(consumer.getIntervalsExceedRegisterDiffCounter());
		sameRegistersText.setInfoFromCounter(consumer.getSameRegistersCounter());
	}
}
