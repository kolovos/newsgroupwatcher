/*******************************************************************************
 * Copyright (c) 2008 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Dimitrios Kolovos - initial API and implementation
 ******************************************************************************/

package io.dimitris.newsgroupwatcher;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import javax.swing.ImageIcon;
import javax.swing.UIManager;

public class Application implements NewArticleListener {
	
	protected SystemTray systemTray;
	protected TrayIcon trayIcon;
	protected Timer timer;
	protected Collection<NewsgroupWatcher> newsgroupWatchers = new ArrayList<NewsgroupWatcher>();
	protected Image defaultIcon;
	protected Image attentionIcon;
	
	public static void main(String[] args) {
		
		//try {
		//	UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		//} catch (Exception e) {
		//}
		new Application().launch();
	}
	
	public void launch() {
		
		defaultIcon = new ImageIcon(Application.class.getResource("application.gif")).getImage();
		attentionIcon = new ImageIcon(Application.class.getResource("attention.gif")).getImage();
		
		if (SystemTray.isSupported()) {
			systemTray = SystemTray.getSystemTray();
			
			PopupMenu popup = new PopupMenu();
			MenuItem defaultItem = new MenuItem("Exit");
			defaultItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					shutdown();
				}
		    });
		    popup.add(defaultItem);
			
			trayIcon = new TrayIcon(defaultIcon, "Newsgroup Watcher", popup);
			trayIcon.setImageAutoSize(true);
			trayIcon.addActionListener(new ActionListener(){

				public void actionPerformed(ActionEvent e) {
					setAttentionRequired(false);
				}
				
			});
			try {
				systemTray.add(trayIcon);
			} catch (AWTException e) {
				shutdown();
			}
			
			setAttentionRequired(false);
			startNewsgroupWatchers();
			
		} else {
			shutdown();
		}
	}
	
	public void setAttentionRequired(boolean attentionRequired) {
		
		String tooltip = "Newsgroup Watcher \r\n  ";
		
		if (attentionRequired) {
			trayIcon.setImage(attentionIcon);
			Set<String> newsgroups = new TreeSet<String>();
			for (ArticleHeader header : articleHeaders) {
				for (String newsgroup : header.newsgroups.split(",")) {
					newsgroups.add(newsgroup.trim());
				}
			}
			
			Iterator<String> it = newsgroups.iterator();
			while (it.hasNext()) {
				String newsgroup = it.next();
				tooltip = tooltip + newsgroup;
				if (it.hasNext()) {
					tooltip += ", ";
				}
			}
		}
		else {
			trayIcon.setImage(defaultIcon);
			tooltip += "No new articles";
		}
		
		trayIcon.setToolTip(null);
		trayIcon.setToolTip(tooltip);
		
	}
	
	protected void loadNewsgroupWatchers() {
		String dirPath = System.getProperty("user.dir");
		File dir = new File(dirPath);
		for (File file : dir.listFiles()) {
			if (file.getName().endsWith(".watched")) {
				newsgroupWatchers.add(createNewsgroupWatcher(file));
			}
		}
	}
	
	protected NewsgroupWatcher createNewsgroupWatcher(File file) {
		Properties properties = new Properties();
		NewsgroupWatcher newsgroupWatcher = new NewsgroupWatcher();
		try {
			properties.load(new FileInputStream(file));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		newsgroupWatcher.setServer(properties.getProperty("server"));
		newsgroupWatcher.setAuthenticationRequired(Boolean.parseBoolean(properties.getProperty("authenticationRequired")));
		if (newsgroupWatcher.isAuthenticationRequired()) {
			newsgroupWatcher.setUsername(properties.getProperty("username"));
			newsgroupWatcher.setPassword(properties.getProperty("password"));
		}
		newsgroupWatcher.setNewsgroup(properties.getProperty("newsgroup"));
		newsgroupWatcher.setInterval(Integer.parseInt(properties.getProperty("interval")));
		
		return newsgroupWatcher;
	}
	
	// Add .monitored property files in directory
	protected void startNewsgroupWatchers() {
		
		loadNewsgroupWatchers();
		timer = new Timer();
		timer.schedule(new DisplayArticleHeaderTask(), 0, 5000);
		
		for (NewsgroupWatcher newsgroupWatcher : newsgroupWatchers) {
			//System.err.println("Starting " + newsgroupWatcher.getNewsgroup());
			newsgroupWatcher.addNewArticleListener(this);	
			//int randomDelay = new Double(Math.random() * 10000).intValue();
			timer = new Timer();
			timer.schedule(new StartWatchingTask(newsgroupWatcher), 0);
		}
		
		/*
		newsgroupWatcher = new NewsgroupWatcher();
		newsgroupWatcher.addNewArticleListener(this);
		newsgroupWatcher.setAuthenticationRequired(true);
		newsgroupWatcher.setUsername("exquisitus");
		newsgroupWatcher.setPassword("flinder1f7");
		newsgroupWatcher.setServer("news.eclipse.org");
		//newsgroupWatcher.setNewsgroup("eclipse.tools.emf");
		newsgroupWatcher.setNewsgroup("eclipse.epsilon");
		newsgroupWatcher.setInterval(60000);
		*/
		
		
	}
	
	private class StartWatchingTask extends TimerTask {
		
		protected NewsgroupWatcher newsgroupWatcher;
		
		public StartWatchingTask(NewsgroupWatcher newsgroupWatcher) {
			this.newsgroupWatcher = newsgroupWatcher;
		}
		
		@Override
		public void run() {
			try {
				newsgroupWatcher.startWatching();
			} catch (IOException e) {
				shutdown();
			}
		}
		
	}
	
	protected void stopNewsgroupWatchers() {
		for (NewsgroupWatcher newsgroupWatcher : newsgroupWatchers) {
			newsgroupWatcher.removeNewArticleListener(this);
			newsgroupWatcher.stopWatching();
		}
	}
	
	public void shutdown() {
		stopNewsgroupWatchers();
		//if (timer != null) timer.cancel();
		if (systemTray != null) {
			systemTray.remove(trayIcon);
		}
		System.exit(0);
	}
	
	
	protected final List<ArticleHeader> articleHeaders = new ArrayList<ArticleHeader>();
	
	public void newArticle(ArticleHeader header) {
		//System.err.println("Adding " + header.getNewsgroups());
		articleHeaders.add(header);
		setAttentionRequired(true);
	}
	
	class DisplayArticleHeaderTask extends TimerTask {

		@Override
		public void run() {
			if (articleHeaders.size() > 0) {
				ArticleHeader header = articleHeaders.remove(0);
				trayIcon.displayMessage(header.getNewsgroups(), header.getSender() + ": " + header.getSubject(),
						TrayIcon.MessageType.INFO);
				trayIcon.setImage(attentionIcon);
			}				
		}
		
	}
	
}
