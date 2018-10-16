package com.matthalstead.fileanalyzer.swing;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.apache.log4j.Logger;

import com.matthalstead.fileanalyzer.AppSettings;
import com.matthalstead.fileanalyzer.ByteSink;
import com.matthalstead.fileanalyzer.ConsumerUtils;
import com.matthalstead.fileanalyzer.InputStreamConsumer;
import com.matthalstead.fileanalyzer.cmep.consumers.AbstractCMEPConsumer;
import com.matthalstead.fileanalyzer.cmep.consumers.CMEPConsumerCombiner;
import com.matthalstead.fileanalyzer.consumerfactory.ConsumerDefinition;
import com.matthalstead.fileanalyzer.consumerfactory.ConsumerFactory;
import com.matthalstead.fileanalyzer.consumers.AbstractLineConsumer;
import com.matthalstead.fileanalyzer.consumers.LineConsumerCombiner;
import com.matthalstead.fileanalyzer.filedb.FileDatabase.FileDatabaseRecord;
import com.matthalstead.fileanalyzer.swing.ConsumerPanel.SingleValue;
import com.matthalstead.fileanalyzer.swing.TextFieldWithLabel.ListDialog;
import com.matthalstead.fileanalyzer.util.StreamUtils;

public class FileAnalyzerFrame extends JInternalFrame {

	private static final long serialVersionUID = -7477974541404361226L;
	private static final Logger log = Logger.getLogger(FileAnalyzerFrame.class);
	
	//private final JFrame rootFrame;
	private List<ConsumerPanel> consumerPanels;
	
	private JTextField filenameText = new JTextField(50);
	private File file = null;
	private JButton runButton = new JButton("Process File");
	private JButton cancelButton = new JButton("Cancel");

	private JButton resetButton = new JButton("Reset Panels");
	private JButton condenseButton = new JButton("Condense Panels");
	private JButton chooseFileButton = new JButton("Choose file...");
	
	private JButton resultsButton = new JButton("Comma-Separated Results");
	
	private boolean running = false;
	private boolean cancelled = false;

	public FileAnalyzerFrame() {
		super("File Analyzer", true, true, true, true);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		//this.rootFrame = rootFrame;
		init();
	}
	
	private void init() {
		consumerPanels = buildConsumerPanels();
		filenameText.setEditable(false);
		
		getContentPane().setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = c.gridy = 0;
		c.weightx = c.weighty = 1.0;
		c.gridwidth = c.gridheight = 1;

		getContentPane().add(buildControlPanel(), c);
		
		c.gridy++;
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 10.0;
		getContentPane().add(buildScrollPane(), c);
		
		resetConsumers();
		addListeners();
		updateComponentEnabling();
		
		pack();
	}
	
	public JPanel buildControlPanel() {
		JPanel result = new JPanel();
		result.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = c.gridy = 0;
		c.weightx = c.weighty = 1.0;
		c.gridwidth = c.gridheight = 1;

		result.add(filenameText, c);
		c.gridx++;
		result.add(chooseFileButton, c);
		
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 2;
		result.add(buildHorizontalButtonPanel(runButton, cancelButton), c);

		c.gridy++;
		result.add(buildHorizontalButtonPanel(resetButton, condenseButton, resultsButton), c);
		
		return result;
	}
	
	public JPanel buildHorizontalButtonPanel(JButton ... buttons) {
		JPanel result = new JPanel();
		result.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.CENTER;
		c.gridx = c.gridy = 0;
		c.weightx = c.weighty = 1.0;
		c.gridwidth = c.gridheight = 1;
		for (int i=0; i<buttons.length; i++) {
			result.add(buttons[i], c);
			c.gridx++;
		}
		return result;
	}
	
	private void resetConsumers() {
		try {
			for (ConsumerPanel cp : consumerPanels) {
				cp.consumer.reset();
				cp.handleConsumerUpdate();
			}
		} catch (Exception ex) {
			log.error(ex);
			throw new RuntimeException(ex);
		}
	}
	
	private void condenseConsumers() {
		try {
			for (ConsumerPanel cp : consumerPanels) {
				cp.consumer.condense();
				cp.handleConsumerUpdate();
			}
		} catch (Exception ex) {
			log.error(ex);
			throw new RuntimeException(ex);
		}
	}
	
	private Runnable buildRunner() {
		return new Runnable() {
			public void run() {
				try {
					int maxEnqueuedBytes = AppSettings.getMaxEnqueuedBytes();
					long millisToSleep = AppSettings.getMillisToWaitWhenEnqueuedBytesExceedMax();
					DecimalFormat bytesOutputDF = new DecimalFormat("#,##0");
					running = true;
					cancelled = false;
					updateComponentEnabling();
					ByteSink sink = ConsumerUtils.buildAndStartSinkForConsumers(combineConsumers(getConsumers()));
					final InputStream is = StreamUtils.getInputStream(file);
					byte[] buf = new byte[4096];
					int read;
					while ((!cancelled) && ((read = is.read(buf)) > 0)) {
						sink.writeBytes(buf, 0, read);
						for (ConsumerPanel cp : consumerPanels) {
							cp.handleConsumerUpdate();
						}
						int enqueuedBytes = sink.getEnqueuedBytesEstimate();
						if (millisToSleep > 0 && enqueuedBytes > maxEnqueuedBytes) {
							log.debug("Enqueued bytes: " + bytesOutputDF.format(enqueuedBytes) + " ...sleeping for " + millisToSleep + "ms...");
							Thread.sleep(millisToSleep);
							log.debug("...New enqueued bytes count = " + bytesOutputDF.format(sink.getEnqueuedBytesEstimate()));
						}
					}
					if (cancelled) {
						sink.cancelSink();
					}
					sink.closeSink();
					for (ConsumerPanel cp : consumerPanels) {
						cp.handleConsumerUpdate();
					}
					running = false;
					updateComponentEnabling();
					is.close();
				} catch (Exception e) {
					log.error(e);
					running = false;
					updateComponentEnabling();
					throw (e instanceof RuntimeException) ? ((RuntimeException) e) : new RuntimeException(e);
				}
			}
		};
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		updateComponentEnabling();
	}
	
	private void updateComponentEnabling() {
		boolean enabled = isEnabled();
		runButton.setEnabled(enabled && (file != null) && !running);
		resetButton.setEnabled(enabled && !running);
		condenseButton.setEnabled(enabled && !running);
		chooseFileButton.setEnabled(enabled && !running);
		cancelButton.setEnabled(enabled && running && !cancelled);
		resultsButton.setEnabled(enabled);
	}
	
	private void addListeners() {
		runButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					if (file == null) {
						JOptionPane.showMessageDialog(FileAnalyzerFrame.this, "No file selected!");
					} else {
					
						Runnable r = buildRunner();
						Thread t = new Thread(r);
						t.start();
					}
				} catch (Exception ex) {
					log.error(ex);
					throw new RuntimeException(ex);
				}
			}
		});
		
		resetButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				resetConsumers();
			}
		});

		condenseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				condenseConsumers();
			}
		});

		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cancelled = true;
				updateComponentEnabling();
			}
		});

		chooseFileButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FileListDialog dlg = new FileListDialog();
				dlg.setLocationRelativeTo(FileAnalyzerFrame.this);
				dlg.setModal(true);
				dlg.setVisible(true);
				FileDatabaseRecord fdr = dlg.getSelectedRecord();
				if (fdr != null) {
					setFile(fdr.getFile());
				}
			}
		});
		
		resultsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ListDialog dlg = new ListDialog(getCommaSeparatedResults());
				dlg.setLocationRelativeTo(FileAnalyzerFrame.this);
				dlg.setVisible(true);
			}
		});
	}
	
	private List<String> getCommaSeparatedResults() {
		List<String> result = new ArrayList<String>();
		for (ConsumerPanel consumerPanel : consumerPanels) {
			List<SingleValue> values = consumerPanel.getSingleValues();
			for (SingleValue sv : values) {
				result.add(sv.key.replaceAll("\\,", "") + "," + sv.value);
			}
		}
		return result;
	}
	
	public void setFile(File f) {
		this.file = f;
		if (f == null) {
			filenameText.setText("");
			setTitle("File Analyzer");
		} else {
			filenameText.setText(f.getAbsolutePath());
			setTitle("File Analyzer: " + f.getName());
		}
		updateComponentEnabling();
	}
	
	private InputStreamConsumer[] getConsumers() {
		List<InputStreamConsumer> consumers = new ArrayList<InputStreamConsumer>();
		for (ConsumerPanel cp : consumerPanels) {
			consumers.add(cp.consumer);
		}
		return consumers.toArray(new InputStreamConsumer[consumers.size()]);
	}
	
	private InputStreamConsumer[] combineConsumers(InputStreamConsumer[] consumers) {
		log.debug("Combining " + consumers.length + " consumers...");
		List<AbstractCMEPConsumer> cmepConsumers = new ArrayList<AbstractCMEPConsumer>();
		List<AbstractLineConsumer> lineConsumers = new ArrayList<AbstractLineConsumer>();
		List<InputStreamConsumer> otherConsumers = new ArrayList<InputStreamConsumer>();
		for (int i=0; i<consumers.length; i++) {
			InputStreamConsumer c = consumers[i];
			if (c instanceof AbstractCMEPConsumer) {
				log.debug("Consumer at index " + i + " is a CMEP consumer");
				cmepConsumers.add((AbstractCMEPConsumer) c);
			} else if (c instanceof AbstractLineConsumer) {
				log.debug("Consumer at index " + i + " is a line consumer");
				lineConsumers.add((AbstractLineConsumer) c);
			} else {
				log.debug("Consumer at index " + i + " is a generic consumer");
				otherConsumers.add(c);
			}
		}
		if (cmepConsumers.isEmpty() && lineConsumers.isEmpty()) {
			log.debug("No special consumers!");
			return consumers;
		}
		
		List<InputStreamConsumer> result = new ArrayList<InputStreamConsumer>(otherConsumers);
		if (!cmepConsumers.isEmpty()) {
			CMEPConsumerCombiner cmepCombiner = new CMEPConsumerCombiner(cmepConsumers);
			result.add(cmepCombiner);
		}
		if (!lineConsumers.isEmpty()) {
			LineConsumerCombiner lineCombiner = new LineConsumerCombiner(lineConsumers);
			result.add(lineCombiner);
		}
		log.debug("New number of combiners: " + result.size());
		return result.toArray(new AbstractLineConsumer[result.size()]);
	}
	
	private JScrollPane buildScrollPane() {
		JPanel p = new JPanel();
		p.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = c.gridy = 0;
		c.weightx = c.weighty = 1.0;
		c.gridwidth = c.gridheight = 1;
		for (ConsumerPanel consumerPanel : consumerPanels) {
			CollapserPanel collapser = new CollapserPanel(consumerPanel.getTitle(), consumerPanel);
			collapser.setBorder(BorderFactory.createEtchedBorder());
			p.add(collapser, c);
			c.gridy++;
		}
		
		JScrollPane result = new JScrollPane(p, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		result.setPreferredSize(new Dimension(400, 300));
		return result;
	}
	
	private List<ConsumerPanel> buildConsumerPanels() {
		List<ConsumerPanel> result = new ArrayList<ConsumerPanel>();
		List<ConsumerDefinition> cdList = ConsumerFactory.loadConsumerDefinitions();
		for (ConsumerDefinition cd : cdList) {
			if (cd.isRoot()) {
				InputStreamConsumer consumer = ConsumerFactory.buildConsumer(cd);
				ConsumerPanel cp = ConsumerPanelFactory.buildConsumerPanel(cd, consumer);
				//cp.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), cp.getTitle()));
				result.add(cp);
			}
		}
		return result;
	}

	@Override
	public void setVisible(boolean aFlag) {
		if (!aFlag) {
			cancelled = true;
		}
		super.setVisible(aFlag);
	}
}
