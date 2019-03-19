/*
 * HTTPRedirectCache.java
 *
 * Copyright (C) KAI Square Pte Ltd
 */

package com.kaisquare.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import play.Logger;

/**
 * A cache for HTTP URL redirects.
 *
 * @author Tan Yee Fan
 */
public class HTTPRedirectCache {
	private static final long REQUERY_INTERVAL = 60000L;
	private static final HTTPRedirectCache INSTANCE = new HTTPRedirectCache();

	public static HTTPRedirectCache getInstance() {
		return INSTANCE;
	}

	private Map<URL, Redirect> redirectMap;

	public HTTPRedirectCache() {
		this.redirectMap = new HashMap<URL, Redirect>();
	}

	public synchronized URL getRedirectedURL(URL url) {
		Redirect redirect = this.redirectMap.get(url);
		if (redirect == null || System.currentTimeMillis() - redirect.getQueryTime() > REQUERY_INTERVAL) {
			URL redirectUrl = HTTPUtil.getRedirectedURL(url);
			if (redirectUrl != null) {
				Logger.info("Redirection: %s -> %s", url, redirectUrl);
				redirect = new Redirect(System.currentTimeMillis(), redirectUrl);
				this.redirectMap.put(url, redirect);
			}
		}
		if (redirect != null)
			return redirect.getURL();
		else
			return null;
	}

	public synchronized URL getRedirectedURL(String path) {
		try {
			URL url = new URL(path);
			return getRedirectedURL(url);
		}
		catch (MalformedURLException e) {
			return null;
		}
	}

	private class Redirect {
		private long queryTime;
		private URL url;

		public Redirect(long queryTime, URL url) {
			this.queryTime = queryTime;
			this.url = url;
		}

		public long getQueryTime() {
			return this.queryTime;
		}

		public URL getURL() {
			return this.url;
		}
	}
}

