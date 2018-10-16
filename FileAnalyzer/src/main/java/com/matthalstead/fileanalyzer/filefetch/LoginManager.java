package com.matthalstead.fileanalyzer.filefetch;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.apache.log4j.Logger;

import com.matthalstead.fileanalyzer.AppSettings;
import com.matthalstead.fileanalyzer.util.GBC;
import com.matthalstead.fileanalyzer.util.ImageUtils;

public class LoginManager {
	
	private static final Logger log = Logger.getLogger(LoginManager.class);
	
	private static final Map<String, LoginProvider> providerMap = new HashMap<String, LoginProvider>();
	private static final Object MAP_LOCK = new Object();
	
	
	public static LoginProvider getLoginProvider(String systemName) {
		synchronized (MAP_LOCK) {
			LoginProvider result = providerMap.get(MAP_LOCK);
			if (result != null) {
				if (result.isFailed()) {
					throw new RuntimeException("Login provider for \"" + systemName + "\" previously failed.");
				}
				return result;
			}
		}

		LoginProvider result = new LoginProvider();
		result.setSystemName(systemName);
		populateFromAuthFile(result);
		if (result.getUsername() == null || result.getPassword() == null) {
			populateFromGUI(result);
		}

		synchronized (MAP_LOCK) {
			providerMap.put(systemName, result);
		}

		return result;
	}
	
	private static void populateFromAuthFile(LoginProvider provider) {
		File f = AppSettings.getAuthFile();
		if (f.exists() && !f.isDirectory()) {
			try {
				FileInputStream fis = new FileInputStream(f);
				Properties props = new Properties();
				props.load(fis);
				String username = props.getProperty(provider.getSystemName() + ".username");
				if (username != null) {
					provider.setUsername(username);
				}
				String password = props.getProperty(provider.getSystemName() + ".password");
				if (password != null) {
					provider.setPassword(password);
				}
				fis.close();
			} catch (IOException ioe) {
				log.warn("Error loading auth file " + f.getAbsolutePath(), ioe);
			}
		}
	}
	
	private static void populateFromGUI(LoginProvider provider) {
		LoginDialog dlg = new LoginDialog(provider);
		dlg.setLocationRelativeTo(null);
		dlg.setResizable(false);
		dlg.setModal(true);
		dlg.setVisible(true);
		if (!dlg.isOkSelected()) {
			throw new RuntimeException("Login not provided");
		}
	}
	
	private static class LoginDialog extends JDialog {
		private static final long serialVersionUID = 959912690728903192L;

		private final LoginProvider provider;
		
		private JTextField usernameText;
		private JPasswordField passwordText;

		private JButton okButton = new JButton("OK");
		private JButton cancelButton = new JButton("Cancel");
		
		private boolean okSelected = false;
		
		public LoginDialog(LoginProvider provider) {
			this.provider = provider;
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			setTitle("Login for " + provider.getSystemName());
			init();
		}
		
		private void init() {
			ImageUtils.setupIcons(this);
			usernameText = new JTextField(provider.getUsername(), 12);
			passwordText = new JPasswordField(provider.getPassword(), 12);
			setLayout(new GridBagLayout());
			GBC c = GBC.getDefault();
			
			add(new JLabel("Username:"), c);
			c.gridx++;
			add(usernameText, c);
			
			c.gridx = 0;
			c.gridy++;
			add(new JLabel("Password:"), c);
			c.gridx++;
			add(passwordText, c);
			
			c.gridx = 0;
			c.gridy++;
			c.anchor = GBC.EAST;
			add(okButton, c);
			
			c.gridx++;
			c.anchor = GBC.WEST;
			add(cancelButton, c);
			
			addListeners();
			
			pack();
		}
		
		private void addListeners() {
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					LoginDialog.this.setVisible(false);
				}
			});
			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String username = trimStringAndNullIfEmpty(usernameText.getText());
					String password = trimStringAndNullIfEmpty(new String(passwordText.getPassword()));
					
					if (username == null || password == null) {
						JOptionPane.showMessageDialog(LoginDialog.this, "Both username and password are required.");
						if (username == null) {
							usernameText.requestFocus();
						} else {
							passwordText.requestFocus();
						}
					} else {
						provider.setUsername(username);
						provider.setPassword(password);
						okSelected = true;
						LoginDialog.this.setVisible(false);
					}
				}
			});
		}
		
		private String trimStringAndNullIfEmpty(String str) {
			if (str == null) {
				return null;
			} else {
				str = str.trim();
				return (str.length() == 0) ? null : str;
			}
		}
		
		public boolean isOkSelected() {
			return okSelected;
		}
	}
}
