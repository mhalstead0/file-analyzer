package com.matthalstead.fileanalyzer.swing;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import org.apache.log4j.Logger;

import com.matthalstead.fileanalyzer.filedb.FileDatabase;
import com.matthalstead.fileanalyzer.filedb.FileDatabase.FileDatabaseRecord;
import com.matthalstead.fileanalyzer.util.GBC;
import com.matthalstead.fileanalyzer.util.ImageUtils;

public class FileListDialog extends JDialog {
	
	private static final String UNKNOWN = "unknown";

	private static final long serialVersionUID = -5144079575363962186L;
	private static final Logger log = Logger.getLogger(FileListDialog.class);
	
	private JComboBox<String> typeDropdown = new JComboBox<>();
	private JPanel listPanel;
	private FileDatabaseRecord selectedRecord = null;
	
	private List<FileRecordPanel> fileRecordPanels = new ArrayList<FileRecordPanel>();
	
	public FileListDialog() {
		super();
		setTitle("Choose a file...");
		ImageUtils.setupIcons(this);
		init();
	}
	
	
	private void init() {
		setLayout(new GridBagLayout());
		GBC c = GBC.getDefault();
		
		c.fill = GBC.HORIZONTAL;
		add(buildHeaderPanel(), c);
		
		c.fill = GBC.NONE;
		c.gridy++;
		listPanel = buildListPanel();
		populateListPanel();
		JScrollPane scrollPane = new JScrollPane(listPanel);
		scrollPane.setMinimumSize(new Dimension(600, 350));
		scrollPane.setPreferredSize(new Dimension(600, 500));
		scrollPane.setMaximumSize(new Dimension(600, 500));
		add(scrollPane, c);
		pack();
		
		addListeners();
	}
	
	private void addListeners() {
		typeDropdown.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doTypeFilter();
			}
		});
	}
	
	private void doTypeFilter() {
		if (typeDropdown.getSelectedIndex() == 0) {
			for (FileRecordPanel frp : fileRecordPanels) {
				frp.setVisible(true);
			}
		} else {
			String selectedType = (String) typeDropdown.getSelectedItem();
			for (FileRecordPanel frp : fileRecordPanels) {
				FileDatabaseRecord fdr = frp.fdr;
				String type = fdr.getType();
				if (type == null || type.trim().length() == 0) {
					type = UNKNOWN;
				}
				frp.setVisible(type.equals(selectedType));
			}
		}
	}
	
	private JPanel buildHeaderPanel() {
		JPanel result = new JPanel();
		result.setLayout(new GridBagLayout());
		GBC c = GBC.getDefault();
		c.anchor = GBC.EAST;
		result.add(new JLabel("Type:"), c);
		
		c.weightx = 20.0;
		c.gridx++;
		c.anchor = GBC.WEST;
		result.add(typeDropdown, c);
		return result;
	}
	
	private void setTypes(FileDatabase db) {
		
		String existingChoice = (String) typeDropdown.getSelectedItem();
		
		Set<String> types = new TreeSet<String>();
		
		Map<String, FileDatabaseRecord> map = db.getMap();
		if (map != null) {
			Set<String> names = new TreeSet<String>(map.keySet());
			for (String name : names) {
				FileDatabaseRecord fdr = map.get(name);
				
				String type = fdr.getType();
				if (type == null) {
					type = UNKNOWN;
				}
				types.add(type);
			}
		}
		
		int selectedIndex = 0;
		String[] ary = new String[types.size() + 1];
		ary[0] = "[All]";
		int index = 1;
		for (String str : types) {
			ary[index] = str;
			if (existingChoice != null && existingChoice.equals(str)) {
				selectedIndex = index;
			}
			index++;
		}
		typeDropdown.setModel(new DefaultComboBoxModel<>(ary));
		typeDropdown.setSelectedIndex(selectedIndex);
	}
	
	private void populateListPanel() {
		try {
			while (listPanel.getComponentCount() > 0) {
				listPanel.remove(0);
			}
			GBC c = GBC.getDefault();
			c.anchor = GBC.NORTHWEST;
			c.fill = GBC.HORIZONTAL;
			c.insets = new Insets(2, 4, 2, 4);
			c.ipadx = c.ipady = 1;
		
			fileRecordPanels.clear();
			FileDatabase db = FileDatabase.load();
			
			Map<String, FileDatabaseRecord> map = db.getMap();
			if (map == null) {
				map = new HashMap<String, FileDatabaseRecord>();
			}
			Set<String> names = new TreeSet<String>(map.keySet());
			for (String name : names) {
				FileDatabaseRecord fdr = map.get(name);
				FileRecordPanel frp = new FileRecordPanel(name, fdr);
				fileRecordPanels.add(frp);
				
			}
			Collections.sort(fileRecordPanels, TIMESTAMP_COMPARATOR);
			for (FileRecordPanel frp : fileRecordPanels) {
				listPanel.add(frp, c);
				c.gridy++;
			}
			setTypes(db);
			doTypeFilter();
			listPanel.repaint();
		} catch (IOException ioe) {
			log.error(ioe);
			throw new RuntimeException(ioe);
		}
	}
	
	private JPanel buildListPanel() {
		JPanel p = new JPanel();
		p.setLayout(new GridBagLayout());
		
		return p;
	}
	
	
	
	
	private void changeRecordAndSave(String oldName, String newName, FileDatabaseRecord fdr) {
		try {
			FileDatabase db = FileDatabase.load();
			Map<String, FileDatabaseRecord> map = db.getMap();
			map.remove(oldName);
			map.put(newName, fdr);
			db.setMap(map);
			FileDatabase.save(db);
			
			populateListPanel();
			setTypes(db);
			doTypeFilter();
		} catch (IOException ioe) {
			log.error(ioe);
			throw new RuntimeException(ioe);
		}
	}
	
	
	
	public FileDatabaseRecord getSelectedRecord() {
		return selectedRecord;
	}
	
	
	public static final Comparator<FileRecordPanel> TIMESTAMP_COMPARATOR = new Comparator<FileRecordPanel>() {
		public int compare(FileRecordPanel frp1, FileRecordPanel frp2) {
			long tsDiff = frp2.fdr.getModifyTimestamp() - frp1.fdr.getModifyTimestamp();
			if (tsDiff == 0) {
				return frp2.fdr.getAbsoluteFilename().compareTo(frp1.fdr.getAbsoluteFilename());
			} else {
				return tsDiff > 0L ? 1 : -1;
			}
		}
	};
	
	
	private class FileRecordPanel extends JPanel {
		private static final long serialVersionUID = 8761104109994396451L;
		
		private final String name;
		private final FileDatabaseRecord fdr;
		
		private JButton editButton = new JButton("Edit...");
		
		private final Border HOVERED_BORDER = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
		private final Border NORMAL_BORDER = BorderFactory.createBevelBorder(BevelBorder.RAISED);
		
		public FileRecordPanel(String name, FileDatabaseRecord fdr) {
			this.name = name;
			this.fdr = fdr;
			init();
		}
		
		private void init() {
			setMinimumSize(new Dimension(160, 40));
			
			setLayout(new GridBagLayout());
			GBC c = GBC.getDefault();
			
			c.anchor = GBC.WEST;
			c.fill = GBC.NONE;
			JLabel typeLabel = new JLabel("[" + (fdr.getType() == null ? "?" : fdr.getType()) + "]");
			typeLabel.setFont(new Font("Calibri", Font.ITALIC, 12));
			add(typeLabel, c);
			
			
			c.fill = GBC.HORIZONTAL;
			c.gridx++;
			c.weightx = 50.0;
			add(new JLabel(name), c);
			
			c.gridx++;
			c.anchor = GBC.EAST;
			c.fill = GBC.NONE;
			c.weightx = 1.0;
			add(editButton, c);
			
			setToolTipText(fdr.getDescription());

			editButton.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setBorder(NORMAL_BORDER);
			
			addListeners();
		}
		
		private void addListeners() {
			//MouseAdapter ma = 
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent e) {
					FileRecordPanel.this.setBorder(HOVERED_BORDER);
				}
				
				@Override
				public void mouseExited(MouseEvent e) {
					//if (!FileRecordPanel.this.getBounds().contains(e.getPoint())) {
						FileRecordPanel.this.setBorder(NORMAL_BORDER);
					//}
				}
				
				@Override
				public void mouseClicked(MouseEvent e) {
					FileListDialog.this.selectedRecord = fdr;
					FileListDialog.this.setVisible(false);
				}
			});
			
			editButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					EditFileDialog dlg = new EditFileDialog(name, fdr);
					dlg.setModal(true);
					dlg.setLocationRelativeTo(editButton);
					dlg.setVisible(true);
				}
			});
		}
		
	}
	
	private class EditFileDialog extends JDialog {
		private static final long serialVersionUID = 4344029223816428258L;
		
		private final String name;
		private final FileDatabaseRecord fdr;
		
		private JTextField filenameText = new JTextField(40);
		private JTextField nameText = new JTextField(40);
		private JTextField typeText = new JTextField(10);
		private JTextField descriptionText =  new JTextField(40);
		private JButton saveButton = new JButton("Save");
		
		
		public EditFileDialog(String name, FileDatabaseRecord fdr) {
			super(FileListDialog.this);
			this.name = name;
			this.fdr = fdr;
			EditFileDialog.this.init();
		}
		
		private void init() {
			filenameText.setEditable(false);
			filenameText.setText(fdr.getAbsoluteFilename());
			nameText.setText(name);
			typeText.setText(UNKNOWN.equals(fdr.getType()) ? "" : fdr.getType());
			descriptionText.setText(fdr.getDescription());
			
			setLayout(new GridBagLayout());
			GBC c = GBC.getDefault();
			
			c.anchor = GBC.EAST;
			add(new JLabel("File:"), c);
			c.gridy++;
			add(new JLabel("Name:"), c);
			c.gridy++;
			add(new JLabel("Type:"), c);
			c.gridy++;
			add(new JLabel("Description:"), c);
			
			c.gridx++;
			c.gridy = 0;
			c.anchor = GBC.WEST;
			add(filenameText, c);
			c.gridy++;
			add(nameText, c);
			c.gridy++;
			add(typeText, c);
			c.gridy++;
			add(descriptionText, c);
			
			c.gridx = 0;
			c.gridy++;
			c.gridwidth = 2;
			c.anchor = GBC.CENTER;
			add(saveButton, c);
			
			addListeners();
			pack();
		}
		
		private void addListeners() {
			saveButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String newName = getText(nameText);
					if (newName == null) {
						newName = getText(filenameText);
					}
					fdr.setType(getText(typeText));
					fdr.setDescription(getText(descriptionText));
					FileListDialog.this.changeRecordAndSave(name, newName, fdr);
					EditFileDialog.this.setVisible(false);
				}
			});
		}
		
		private String getText(JTextField txt) {
			String result = txt.getText();
			if (result != null) {
				result = result.trim();
				if (result.length() == 0) {
					result = null;
				}
			}
			return result;
		}
		
		
		
		
	}
	
}
