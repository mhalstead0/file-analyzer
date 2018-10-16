package com.matthalstead.fileanalyzer.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import com.matthalstead.fileanalyzer.util.GBC;
import com.matthalstead.fileanalyzer.util.ImageUtils;
import com.matthalstead.fileanalyzer.util.VersionUtils;
import com.matthalstead.fileanalyzer.util.VersionUtils.Version;

public class FileAnalyzerMainFrame extends JFrame {
	private static final Logger log = Logger.getLogger(FileAnalyzerMainFrame.class);

	private static final long serialVersionUID = 2969065823605125864L;
	
	private final JDesktopPane desktopPane = new JDesktopPane();
	
	private final Object FRAME_LOCK = new Object();
	private final List<FileAnalyzerFrame> fileAnalyzerFrames = new ArrayList<FileAnalyzerFrame>();
	
	private CMEPFetchFrame cmepFetchFrame = new CMEPFetchFrame();

	public FileAnalyzerMainFrame() {
		super("File Analyzer");
		init();
	}
	
	private void init() {
		setSize(1000, 600);
		setupMenu();
		//setContentPane(desktopPane);
		setContentPane(buildContentPane());
		
		cmepFetchFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		cmepFetchFrame.setVisible(false);
		desktopPane.add(cmepFetchFrame);
		
		startFrameCleanupThread();
	}
	
	private JPanel buildContentPane() {
		JPanel result = new JPanel();
		result.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = c.gridy = 0;
		c.gridwidth = c.gridheight = 1;
		c.weightx = c.weighty = 1.0;
		
		
		JScrollPane scrollPane = new JScrollPane(desktopPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setSize(1000, 600);
		c.fill = GridBagConstraints.BOTH;
		result.add(desktopPane, c);
		
		c.gridy++;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weighty = 0.01;
		c.insets = new Insets(0, 0, 0, 0);
		result.add(new StatusBarPanel(), c);
		
		return result;
		
	}
	
	private void setupMenu() {
		JMenuBar mb = new JMenuBar();
		JMenu m = new JMenu("File");
		JMenuItem newFAWMI = new JMenuItem("New File Analyzer Window");
		newFAWMI.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addFileFrame(new FileAnalyzerFrame());
			}
		});
		m.add(newFAWMI);
		JMenuItem cmepFetchMI = new JMenuItem("Fetch CMEP Files");
		cmepFetchMI.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cmepFetchFrame.setVisible(true);
			}
		});
		m.add(cmepFetchMI);
		mb.add(m);
		setJMenuBar(mb);
	}
	
	
	public void addFileFrame(final FileAnalyzerFrame f) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ImageUtils.setupIcons(f);
				desktopPane.add(f);
				f.setVisible(true);
				synchronized(FRAME_LOCK) {
					fileAnalyzerFrames.add(f);
				}
			}
		});
	}
	
	private void cleanupFrames() {
		List<FileAnalyzerFrame> framesToRemove = new ArrayList<FileAnalyzerFrame>(0);
		synchronized(FRAME_LOCK) {
			Iterator<FileAnalyzerFrame> fIter = fileAnalyzerFrames.iterator();
			while (fIter.hasNext()) {
				FileAnalyzerFrame f = fIter.next();
				if (!f.isVisible()) {
					framesToRemove.add(f);
					fIter.remove();
				}
			}
		}
		for (FileAnalyzerFrame f : framesToRemove) {
			remove(f);
			log.debug("Removed a FileAnalyzerFrame.");
		}
	}
	
	private void startFrameCleanupThread() {
		Runnable r = new Runnable() {
			public void run() {
				log.info("FileAnalyzerMainFrame.FrameCleanupThread starting...");
				boolean keepGoing = true;
				while (keepGoing) {
					try {
						Thread.sleep(10000L);
					} catch (InterruptedException ie) {
						log.info("FileAnalyzerMainFrame.FrameCleanupThread interrupted!");
					}
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							cleanupFrames();
						}
					});
					keepGoing = FileAnalyzerMainFrame.this.isVisible();
				}
				log.info("FileAnalyzerMainFrame.FrameCleanupThread ending.");
			}
		};
		Thread t = new Thread(r, "FileAnalyzerMainFrame.FrameCleanupThread");
		t.setDaemon(true);
		t.setPriority(Thread.NORM_PRIORITY);
		t.start();
	}
	
	private class StatusBarPanel extends JPanel {
		private static final long serialVersionUID = 2454857388398604654L;
		
		private final Object FORMAT_LOCK = new Object();
		private SimpleDateFormat timestampSDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz");
		private DecimalFormat memDF = new DecimalFormat("0.00");
		private JLabel timestampLabel = new JLabel();
		private JLabel memoryLabel = new JLabel();

		public StatusBarPanel() {
			init();
		}
		
		private void init() {
			Runnable labelUpdateRunner = buildUpdateRunner();
			labelUpdateRunner.run();
			
			setLayout(new GridBagLayout());
			GBC c = GBC.getDefault();
			
			c.fill = GBC.NONE;
			c.anchor = GBC.WEST;
			add(timestampLabel, c);
			
			try {
				Version v = VersionUtils.getVersion();
				if (v != null) {
					JLabel vLabel = new JLabel("Revision " + v.getRevision() + ", Built " + new SimpleDateFormat("M/d/yyyy HH:mm:ss zzz").format(v.getRevisionTimestamp()));
					c.anchor = GBC.CENTER;
					c.gridx++;
					add(vLabel, c);
				}
			} catch (Exception e) {
				//ignore
			}
			
			c.gridx++;
			c.anchor = GBC.EAST;
			add(memoryLabel, c);
			
			setBorder(BorderFactory.createLoweredBevelBorder());
			startUpdateThread();
		}
		
		private void startUpdateThread() {
			Runnable r = new Runnable() {
				public void run() {
					Runnable updateRunner = buildUpdateRunner();
					boolean keepGoing = true;
					while (keepGoing) {
						try {
							Thread.sleep(500L);
						} catch (InterruptedException ie) {
							log.info("FileAnalyzerMainFrame.StatusBarPanelUpdateThread interrupted!");
						}
						SwingUtilities.invokeLater(updateRunner);
						keepGoing = FileAnalyzerMainFrame.this.isVisible();
					}
				}
			};
			Thread t = new Thread(r, "FileAnalyzerMainFrame.StatusBarPanelUpdateThread");
			t.setDaemon(true);
			t.setPriority(Thread.NORM_PRIORITY - 1);
			t.start();
		}
		
		private Runnable buildUpdateRunner() {
			return new Runnable() {
				private int counter = -1;
				public void run() {
					synchronized (FORMAT_LOCK) {
						timestampLabel.setText(timestampSDF.format(new Date()));
						counter = ((counter+1) % 120);
						if (counter < 0) {
							counter = 0;
						}
						if ((counter % 4) == 0) {
							final Runtime runtime = Runtime.getRuntime();
							if (counter == 0) {
								new Thread(new Runnable() {
									public void run() {
										//log.debug("Running GC...");
										runtime.gc();
										//log.debug("GC complete.");
									}
								}).start();
							}
							long freeMemBytes = runtime.freeMemory();
							long totalMemBytes = runtime.totalMemory();
							long usedBytes = totalMemBytes - freeMemBytes;
							String usedBytesString;
							if (usedBytes < 1024L) {
								usedBytesString = usedBytes + "B";
							} else if (usedBytes < (1024L*(1024L*9L/10L))) {
								double kb = ((double) usedBytes) / 1024.0;
								usedBytesString = memDF.format(kb) + "kB";
							} else if (usedBytes < (1024L*1024L*(1024L*9L/10L))) {
								double mb = ((double) usedBytes) / (1024.0 * 1024.0);
								usedBytesString = memDF.format(mb) + "MB";
							} else {
								double gb = ((double) usedBytes) / (1024.0 * 1024.0 * 1024.0);
								usedBytesString = memDF.format(gb) + "GB";
							}
							memoryLabel.setText("Used Memory: " + usedBytesString);
						}
					}
				}
			};
		}
	}
}
