package com.matthalstead.fileanalyzer.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.matthalstead.fileanalyzer.cmep.consumers.DistinctObjectCounterWithCondense;
import com.matthalstead.fileanalyzer.util.ImageUtils;

public class TextFieldWithLabel extends JPanel {

	private static final long serialVersionUID = 7045570561858860631L;

	private final JLabel label;
	private final JTextField text;
	private final JButton linkToListButton;
	private final List<String> list;
	private final Set<String> listAsSet;
	private final Object LIST_LOCK;
	private final DecimalFormat df;
	private final Object DF_LOCK;
	
	public TextFieldWithLabel(String labelText) {
		this(labelText, null, null, false);
	}
	
	public TextFieldWithLabel(String labelText, String tooltipText) {
		this(labelText, tooltipText, null, false);
	}
	
	public TextFieldWithLabel(String labelText, String tooltipText, String numberFormat, boolean linkToList) {
		label = new JLabel(labelText);
		text = new JTextField(10);
		if (linkToList) {
			linkToListButton = new JButton("...");
			list = new ArrayList<String>();
			listAsSet = new HashSet<String>();
			LIST_LOCK = new Object();
		} else {
			linkToListButton = null;
			list = null;
			listAsSet = null;
			LIST_LOCK = null;
		}
		if (numberFormat == null) {
			df = null;
			DF_LOCK = null;
		} else {
			df = new DecimalFormat(numberFormat);
			DF_LOCK = new Object();
		}
		init();
		if (tooltipText != null) {
			setToolTipText(tooltipText);
		}
	}
	
	public String getLabelText() {
		return label.getText();
	}
	
	public String getText() {
		return text.getText();
	}
	
	private void init() {
		text.setEditable(false);
		text.setMinimumSize(text.getPreferredSize());
		
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = c.gridy = 0;
		c.gridwidth = c.gridheight = 1;
		c.weightx = c.weighty = 1.0;
		
		c.anchor = GridBagConstraints.EAST;
		add(label, c);
		
		c.anchor = GridBagConstraints.WEST;
		c.gridx++;
		add(text, c);
		
		if (linkToListButton != null) {
			c.gridx++;
			c.ipadx = -25;
			add(linkToListButton, c);
		}
		
		addListeners();
		updateEnabling();
	}
	
	private void addListeners() {
		if (linkToListButton != null) {
			linkToListButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					ListDialog dlg = new ListDialog(getList());
					dlg.setLocationRelativeTo(TextFieldWithLabel.this);
					dlg.setVisible(true);
				}
			});
		}
	}
	
	public void setText(String str) {
		text.setText(str);
	}
	
	public void setValue(long l) {
		String str;
		if (df == null) {
			str = "" + l;
		} else {
			synchronized(DF_LOCK) {
				str = df.format(l);
			}
		}
		setText(str);
	}

	public void setValue(double d) {
		String str;
		if (df == null) {
			str = "" + d;
		} else {
			synchronized(DF_LOCK) {
				str = df.format(d);
			}
		}
		setText(str);
	}
	
	public void addStringToList(String str) {
		if (list != null) {
			synchronized(LIST_LOCK) {
				if (!listAsSet.contains(str)) {
					list.add(str);
				}
				updateEnabling();
			}
		}
	}
	
	public void setList(Collection<String> strings) {
		if (list != null) {
			synchronized(LIST_LOCK) {
				list.clear();
				listAsSet.clear();
				if (strings != null) {
					list.addAll(strings);
					listAsSet.addAll(strings);
				}
				updateEnabling();
			}
		}
	}
	
	public void clearList() {
		if (list != null) {
			synchronized(LIST_LOCK) {
				listAsSet.clear();
				list.clear();
				updateEnabling();
			}
		}
	}
	
	public void setInfoFromCounter(DistinctObjectCounterWithCondense<String> counter) {
		synchronized(LIST_LOCK) {
			setValue(counter.getCount());
			setList(counter.getObjects());
		}
	}
	
	private List<String> getList() {
		if (list == null) {
			return new ArrayList<String>(0);
		} else {
			synchronized(LIST_LOCK) {
				return new ArrayList<String>(list);
			}
		}
	}
	
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		updateEnabling();
	}
	
	private void updateEnabling() {
		boolean enabled = isEnabled();
		if (linkToListButton != null) {
			linkToListButton.setEnabled(enabled && !list.isEmpty());
		}
	}

	
	public static class ListDialog extends JDialog {
		private static final long serialVersionUID = 6062070052688873797L;
		
		private JTextArea txt = new JTextArea(20, 60);
		private JButton closeButton = new JButton("Close");
		
		public ListDialog(List<String> list) {
			super((JFrame) null, true);
			ImageUtils.setupIcons(this);
			setTitle("Count: " + new DecimalFormat("#,##0").format(list.size()));
			StringBuilder sb = new StringBuilder();
			for (int i=0; i<list.size(); i++) {
				sb.append(list.get(i));
				sb.append("\r\n");
			}
			txt.setText(sb.toString());
			txt.setCaretPosition(0);
			init();
		}
		
		private void init() {
			setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.gridx = c.gridy = 0;
			c.gridwidth = c.gridheight = 1;
			c.weightx = c.weighty = 1.0;
			
			c.gridwidth = 2;
			c.weighty = 5.0;
			JScrollPane scrollPane = new JScrollPane(txt);
			add(scrollPane, c);
			
			c.weighty = 1.0;
			c.gridy++;
			add(closeButton, c);
			addListeners();
			pack();
		}
		
		private void addListeners() {
			closeButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					ListDialog.this.setVisible(false);
				}
			});
		}
		
		
		
	}
}
