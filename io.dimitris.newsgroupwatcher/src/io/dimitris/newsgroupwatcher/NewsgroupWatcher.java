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

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

import org.apache.commons.net.nntp.NNTPClient;
import org.apache.commons.net.nntp.NewsgroupInfo;

public class NewsgroupWatcher {
	
	protected String server;
	protected String newsgroup;
	protected String username;
	protected String password;
	protected boolean authenticationRequired;
	protected NNTPClient client;
	protected long interval = 1000;
	protected boolean watching = false;
	protected int articleCount = -1;
	
	protected ArrayList<NewArticleListener> newArticleListeners = new ArrayList<NewArticleListener>();
	
	public void startWatching() throws IOException {
		watching = true;
		watch();
	}
	
	protected void sleep() {
		try {
			Thread.currentThread().sleep(interval);
		} catch (InterruptedException e) {
			
		}
	}
	
	protected void watch() throws IOException {
		
		while (watching) {
			try {
				//System.err.println("Watching ");
				client = new NNTPClient();
				client.connect(server);
				if (isAuthenticationRequired()) {
					client.authenticate(username, password);
				}
				
				client.selectNewsgroup(newsgroup);
				
				NewsgroupInfo newsgroupInfo = null;
	
				for (NewsgroupInfo ni : client.listNewsgroups()) {
	
					if (ni.getNewsgroup().equals(newsgroup)) {
						newsgroupInfo = ni;
						break;
					}
				}
				
				int newArticleCount = newsgroupInfo.getArticleCount();
				
				//if (articleCount == -1) {
				//	articleCount = newArticleCount;
				//}
				//else 
				if (articleCount < newArticleCount){
					Reader articleReader = client.retrieveArticleHeader(newArticleCount);
					notifyNewArticleListeners(new ArticleHeader(articleReader));
					articleCount = newArticleCount;
				}
				else {
					//System.err.println("Still " + newArticleCount);
				}
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
			finally {
				sleep();
			}
		}
	}
	
	public void stopWatching() {
		watching = false;
	}
	
	protected void notifyNewArticleListeners(ArticleHeader header) {
		
		for (NewArticleListener listener : newArticleListeners) {
			listener.newArticle(header);
		}
	}
	
	public void addNewArticleListener(NewArticleListener listener) {
		newArticleListeners.add(listener);
	}
	
	public void removeNewArticleListener(NewArticleListener listener) {
		newArticleListeners.remove(listener);
	}

	public long getInterval() {
		return interval;
	}

	public void setInterval(long interval) {
		this.interval = interval;
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public String getNewsgroup() {
		return newsgroup;
	}

	public void setNewsgroup(String newsgroup) {
		this.newsgroup = newsgroup;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isAuthenticationRequired() {
		return authenticationRequired;
	}

	public void setAuthenticationRequired(boolean authenticationRequired) {
		this.authenticationRequired = authenticationRequired;
	}

	public ArrayList<NewArticleListener> getNewMessageListeners() {
		return newArticleListeners;
	}

	public void setNewMessageListeners(
			ArrayList<NewArticleListener> newMessageListeners) {
		this.newArticleListeners = newMessageListeners;
	}
	
}
 