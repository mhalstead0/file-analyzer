package com.matthalstead.fileanalyzer.swing;

import java.awt.Cursor;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import com.matthalstead.fileanalyzer.filefetch.FileFetchManager;
import com.matthalstead.fileanalyzer.filefetch.LoggedCMEPFileFetcher;
import com.matthalstead.fileanalyzer.filefetch.FileFetcher.DefaultFetchController;
import com.matthalstead.fileanalyzer.filefetch.FileFetcher.FetchStatus;
import com.matthalstead.fileanalyzer.util.GBC;
import com.matthalstead.fileanalyzer.util.ImageUtils;

public class CMEPFetchFrame extends JInternalFrame {

	private static final long serialVersionUID = -7477974541404361226L;
	private static final Logger log = Logger.getLogger(CMEPFetchFrame.class);
	
	private JComboBox<String> yearDropdown;
	private JComboBox<String> monthDropdown;
	private JComboBox<String> dayDropdown;

	private JComboBox<String> profileComboBox;
	
	private JButton fetchButton = new JButton("Fetch CMEP");
	
	private boolean fetching = false;
	
	private JLabel statusLabel = new JLabel(" ");
	
	
	private static final Map<String, String> profileDescToCode = new TreeMap<String, String>();
	
	static {
		profileDescToCode.put("Production", "cmep.prod");
		profileDescToCode.put("Test1", "cmep.test1");
	}
	
	
	public CMEPFetchFrame() {
		super("Fetch CMEP", true, true, true, true);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		ImageUtils.setupIcons(this);
		init();
	}
	
	private void init() {
		buildDropdowns();
		
		setLayout(new GridBagLayout());
		GBC c = GBC.getDefault();
		add(buildDateDropdownPanel(), c);
		
		c.gridy++;
		add(profileComboBox, c);
		
		c.gridy++;
		add(fetchButton, c);
		
		c.gridy++;
		add(statusLabel, c);
		
		addListeners();
		setStatusMessage("Select a date to fetch CMEP files...");
		pack();
	}
	
	private void buildDropdowns() {
		yearDropdown = buildComboBox(Calendar.YEAR, "yyyy", ((Calendar.getInstance().get(Calendar.YEAR) - 2011) + 1));
		monthDropdown = buildComboBox(Calendar.MONTH, "MMMM", 12);
		dayDropdown = buildComboBox(Calendar.DAY_OF_MONTH, "dd", 31);
		profileComboBox = new JComboBox<>(new ArrayList<>(profileDescToCode.keySet()).toArray(new String[0]));
	}
	
	private JPanel buildDateDropdownPanel() {
		JPanel result = new JPanel();
		result.setLayout(new GridBagLayout());
		GBC c = GBC.getDefault();
		result.add(new JLabel("Date:"), c);
		c.gridx++;
		result.add(monthDropdown, c);
		c.gridx++;
		result.add(dayDropdown, c);
		c.gridx++;
		result.add(yearDropdown, c);
		return result;
	}
	private JComboBox<String> buildComboBox(int calendarField, String outputFormat, int count) {
		Calendar c = Calendar.getInstance();
		int todayValue = c.get(calendarField);
		c.clear();
		c.set(Calendar.YEAR, 2011);
		c.set(Calendar.MONTH, Calendar.JANUARY);
		c.set(Calendar.DAY_OF_MONTH, 1);
		SimpleDateFormat sdf = new SimpleDateFormat(outputFormat);
		
		int defaultIndex = 0;
		JComboBox<String> result = new JComboBox<>();
		DefaultComboBoxModel<String> model = ((DefaultComboBoxModel<String>) result.getModel());
		for (int i=0; i<count; i++) {
			if ((defaultIndex == 0) && (c.get(calendarField) == todayValue)) {
				defaultIndex = i;
			}
			String str = sdf.format(c.getTime());
			model.addElement(str);
			c.add(calendarField, 1);
		}
		result.setSelectedIndex(defaultIndex);
		return result;
	}

	private void addListeners() {
		fetchButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fetching = true;
				setEnabled(isEnabled());
				final String profileCode = profileDescToCode.get(profileComboBox.getSelectedItem());
				final Calendar cal = getCalendarFromDropdowns();
				Runnable r = new Runnable() {
					public void run() {
						doFetch(profileCode, cal);
					}
				};
				Thread t = new Thread(r);
				t.setDaemon(false);
				t.start();
			}
		});
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		setCursor(enabled ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		yearDropdown.setEnabled(enabled && !fetching);
		monthDropdown.setEnabled(enabled && !fetching);
		dayDropdown.setEnabled(enabled && !fetching);
		profileComboBox.setEnabled(enabled && !fetching);
		fetchButton.setEnabled(enabled && !fetching);
		
	}
	
	private Calendar getCalendarFromDropdowns() {
		Calendar c = Calendar.getInstance();
		c.set(Calendar.DAY_OF_MONTH, 1);
		c.set(Calendar.YEAR, Integer.parseInt((String) yearDropdown.getSelectedItem()));
		c.set(Calendar.MONTH, Calendar.JANUARY + monthDropdown.getSelectedIndex());
		c.set(Calendar.DAY_OF_MONTH, Integer.parseInt((String) dayDropdown.getSelectedItem()));
		return c;
	}
	
	private void setStatusMessage(final String str) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				statusLabel.setText(str);
			}
		});
	}
	
	private void doFetch(String profileCode, Calendar cal) {
		log.debug(cal.getTime());
		LoggedCMEPFileFetcher fileFetcher = FileFetchManager.getCMEPFileFetcher(profileCode, cal);
		fileFetcher.fetch(new DefaultFetchController() {
			@Override
			public void statusUpdate(FetchStatus status, String message, long bytesFetched, Long totalBytes) {
				String logMessage = "" + status;
				if (message != null) {
					logMessage += ": " + message;
				}
				if (bytesFetched > 0) {
					DecimalFormat countDF = new DecimalFormat("#,##0");
					logMessage += " (bytes: " + countDF.format(bytesFetched);
					if (totalBytes != null) {
						logMessage += " of " + countDF.format(totalBytes);
						if (totalBytes.longValue() > 0) {
							double num = (double) bytesFetched;
							double denom = (double) totalBytes.longValue();
							DecimalFormat pctDF = new DecimalFormat("0.0%");
							logMessage += " (" + pctDF.format(num / denom) + ")";
						}
					}
					logMessage += ")";
				}
				setStatusMessage(logMessage);
			}
		});
		fetching = false;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setEnabled(isEnabled());
			}
		});
	}
}
