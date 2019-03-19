/**
 * HTTPUtil.java
 *
 * Copyright (C) KAI Square Pte Ltd
 */

package com.kaisquare.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedHashMap;
import java.util.Map;
import play.mvc.Http;

/**
 * Utility methods for HTTP related tasks.
 */
public class HTTPUtil {
	private static Map<Integer, String> statusMap = null;
	private static final long MAX_FILE_SIZE = 500 * 1024 * 1024;

	private static void initStatusMap() {
		if (statusMap == null) {
			statusMap = new LinkedHashMap<Integer, String>();
			statusMap.put(100, "Continue");
			statusMap.put(101, "Switching Protocols");
			statusMap.put(102, "Processing (WebDAV)");
			statusMap.put(103, "Checkpoint");
			statusMap.put(122, "Request-URI Too Long");
			statusMap.put(200, "OK");
			statusMap.put(201, "Created");
			statusMap.put(202, "Accepted");
			statusMap.put(203, "Non-Authoritative Information");
			statusMap.put(204, "No Content");
			statusMap.put(205, "Reset Content");
			statusMap.put(206, "Partial Content");
			statusMap.put(207, "Multi-Status (WebDAV)");
			statusMap.put(226, "IM Used");
			statusMap.put(300, "Multiple Choices");
			statusMap.put(301, "Moved Permanently");
			statusMap.put(302, "Found");
			statusMap.put(303, "See Other");
			statusMap.put(304, "Not Modified");
			statusMap.put(305, "Use Proxy");
			statusMap.put(306, "Switch Proxy");
			statusMap.put(307, "Temporary Redirect");
			statusMap.put(308, "Resume Incomplete");
			statusMap.put(400, "Bad Request");
			statusMap.put(401, "Unauthorized");
			statusMap.put(402, "Payment Required");
			statusMap.put(403, "Forbidden");
			statusMap.put(404, "Not Found");
			statusMap.put(405, "Method Not Allowed");
			statusMap.put(406, "Not Acceptable");
			statusMap.put(407, "Proxy Authentication Required");
			statusMap.put(408, "Request Timeout");
			statusMap.put(409, "Conflict");
			statusMap.put(410, "Gone");
			statusMap.put(411, "Length Required");
			statusMap.put(412, "Precondition Failed");
			statusMap.put(413, "Request Entity Too Large");
			statusMap.put(414, "Request-URI Too Long");
			statusMap.put(415, "Unsupported Media Type");
			statusMap.put(416, "Requested Range Not Satisfiable");
			statusMap.put(417, "Expectation Failed");
			statusMap.put(418, "I'm a Teapot");
			statusMap.put(422, "Unprocessable Entity (WebDAV)");
			statusMap.put(423, "Locked (WebDAV)");
			statusMap.put(424, "Failed Dependency (WebDAV)");
			statusMap.put(425, "Unordered Collection");
			statusMap.put(426, "Upgrade Required");
			statusMap.put(444, "No Response");
			statusMap.put(449, "Retry With");
			statusMap.put(450, "Blocked by Windows Parental Controls");
			statusMap.put(499, "Client Closed Request");
			statusMap.put(500, "Internal Server Error");
			statusMap.put(501, "Not Implemented");
			statusMap.put(502, "Bad Gateway");
			statusMap.put(503, "Service Unavailable");
			statusMap.put(504, "Gateway Timeout");
			statusMap.put(505, "HTTP Version Not Supported");
			statusMap.put(506, "Variant Also Negotiates");
			statusMap.put(507, "Insufficient Storage (WebDAV)");
			statusMap.put(509, "Bandwidth Limit Exceeded");
			statusMap.put(510, "Not Extended");
		}
	}

	/**
	 * Private constructor.
	 */
	private HTTPUtil() {
	}

	/**
	 * Returns the description for the given HTTP status code.
	 *
	 * @param code HTTP status code.
	 */
	public static String getStatusDescription(int code) {
		initStatusMap();
		return statusMap.get(code);
	}

	/**
	 * Performs a single redirection of the given URL. If the given URL is
	 * not redirected, then it is returned. If an error occurred while
	 * performing the redirection, {@code null} is returned. It is possible
	 * that the returned URL may be another redirection URL.
	 *
	 * @param url URL to redirect.
	 */
	public static URL getSingleRedirectedURL(URL url) {
		if (url != null) {
			try {
				URLConnection connection = url.openConnection();
				if (connection instanceof HttpURLConnection) {
					HttpURLConnection httpConnection = (HttpURLConnection)connection;
					httpConnection.setConnectTimeout(5000);
					httpConnection.setInstanceFollowRedirects(false);
					httpConnection.connect();
					int responseCode = httpConnection.getResponseCode();
					if (responseCode / 100 == 3) {
						String location = httpConnection.getHeaderField("Location");
						if (location != null) {
							URL redirectUrl = new URL(url, location);
							return redirectUrl;
						}
						else {
							return url;
						}
					}
					else {
						return url;
					}
				}
				else {
					return url;
				}
			}
			catch (IllegalArgumentException e) {
				return null;
			}
			catch (IOException e) {
				return null;
			}
		}
		else {
			return null;
		}
	}

	/**
	 * Performs all redirections of the given URL. If the given URL is not
	 * redirected, then it is returned. If an error occurred while
	 * performing the redirection, {@code null} is returned.
	 *
	 * @param url URL to redirect.
	 */
	public static URL getRedirectedURL(URL url) {
		final int MAX_NUM_REDIRECTS = 10;
		for (int i = 0; i < MAX_NUM_REDIRECTS; i++) {
			URL redirectUrl = getSingleRedirectedURL(url);
			if (redirectUrl == url)
				return url;
			url = redirectUrl;
		}
		return null;
	}

	/**
	 * Downloads the contents of the given URL and returns it as a byte
	 * array. If an error occurred during the download, {@code null} is
	 * returned.
	 *
	 * @param url URL to download.
	 */
	public static byte[] download(URL url) {
		byte[] result = null;
		if (url != null) {
			InputStream inputStream = null;
			try {
				URLConnection connection = url.openConnection();
				if(MAX_FILE_SIZE >= connection.getContentLengthLong()){
					connection.setConnectTimeout(5000);
					connection.setReadTimeout(5000);
					connection.connect();
					inputStream = connection.getInputStream();
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					byte[] buffer = new byte[8192];
					while (true) {
						int length = inputStream.read(buffer);
						if (length < 0)
							break;
						if (length > 0)
							outputStream.write(buffer, 0, length);
					}
					result = outputStream.toByteArray();
				}
			}
			catch (IOException e) {
			}
			finally {
				if (inputStream != null) {
					try {
						inputStream.close();
					}
					catch (IOException e) {
					}
				}
			}
		}
		return result;
	}
    
    /**
     * For the given request, returns the well formed URI.
     * For example, http://www.example.com or http://www.example.com:9000
     * Port number is added only if not using the common port numbers 80 or 443
     * @param request       The request object.
     * @return 
     */
    public static String getUriForRequest(Http.Request request) {
        String uri;
        if(request.secure) {
            uri = "https://";
        } else {
            uri = "http://";
        }
        
        uri = uri + request.host;
        return uri;
    }
}

