/*
 * @(#)ClientCommunicator.java
 *
 * Copyright 2013 openmolecules.org, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains the property
 * of openmolecules.org.  The intellectual and technical concepts contained
 * herein are proprietary to openmolecules.org.
 * Actelion Pharmaceuticals Ltd. is granted a non-exclusive, non-transferable
 * and timely unlimited usage license.
 *
 * @author Thomas Sander
 */

package org.openmolecules.comm;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.*;

public abstract class ClientCommunicator extends CommunicationHelper {
	private static final int CONNECT_TIME_OUT = 5000;
	private static final int READ_TIME_OUT = 300000; // 0 -> no timeout

    private boolean	mWithSessions;
	private String	mSessionID,mSessionServerURL,mAppicationName;

	public abstract String getPrimaryServerURL();

	/**
	 * @return null or URL of fallback server in case the primary server is not available
	 */
	public abstract String getSecondaryServerURL();

	/**
	 * Whether the service received a setUseSecondaryServer() call earlier,
	 * because the primary server could not be reached.
	 * @return whether URL was switched to fallback server
	 */
	public abstract boolean isUseSecondaryServer();

	/**
	 * If the primary server is not available, then this method is called to switch
	 * to the secondary server for the rest of life of the client application.
	 */
	public abstract void setUseSecondaryServer();

	public abstract void showBusyMessage(String message);
	public abstract void showErrorMessage(String message);

	public ClientCommunicator(boolean withSessions, String applicationName) {
		mWithSessions = withSessions;
		mAppicationName = (applicationName == null) ? "unknown" : applicationName;
		}

	private URLConnection getConnection(String serverURL) throws IOException {
        URL urlServlet = new URL(serverURL);
        HttpURLConnection con = (HttpURLConnection)urlServlet.openConnection();
    
        // konfigurieren
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
		con.setConnectTimeout(CONNECT_TIME_OUT);
		con.setReadTimeout(READ_TIME_OUT);
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        con.setRequestProperty("Content-Type", "application/x-java-serialized-object");

        if (mWithSessions && mSessionID != null)
            con.addRequestProperty(KEY_SESSION_ID, mSessionID);

        return con;
        }

	private void convertToPostRequest(HttpURLConnection con, String request, String... keyValuePair) throws IOException {
		StringBuilder postData = new StringBuilder();

		postData.append(URLEncoder.encode(KEY_REQUEST, "UTF-8"));
		postData.append('=');
		postData.append(URLEncoder.encode(request, "UTF-8"));

		postData.append('&');
		postData.append(URLEncoder.encode(KEY_APP_NAME, "UTF-8"));
		postData.append('=');
		postData.append(URLEncoder.encode(mAppicationName, "UTF-8"));

		for (int i=0; i<keyValuePair.length; i+=2) {
			postData.append('&');
			postData.append(URLEncoder.encode(keyValuePair[i], "UTF-8"));
			postData.append('=');
			postData.append(URLEncoder.encode(String.valueOf(keyValuePair[i+1]), "UTF-8"));
			}

		byte[] postDataBytes = postData.toString().getBytes("UTF-8");

		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		con.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));

		con.getOutputStream().write(postDataBytes); // does make the connection
		}

    private String getResponse(URLConnection con) throws IOException {
        final int BUFFER_SIZE = 1024;
        StringBuilder sb = new StringBuilder();
        BufferedInputStream is = new BufferedInputStream(con.getInputStream());
        byte[] buf = new byte[BUFFER_SIZE];
        while (true) {
            int size = is.read(buf, 0, BUFFER_SIZE);
            if (size == -1)
                break;

            sb.append(new String(buf, 0, size));
            }

        return sb.length() != 0 ? sb.toString() : null;
        }

    public void closeConnection() {
		if (mSessionID != null) {
			showBusyMessage("Closing Communication Channel ...");

	        try {
	            URLConnection con = getConnection(mSessionServerURL);
                con.addRequestProperty(KEY_REQUEST, REQUEST_END_SESSION);

                getResponse(con);
	            }
	        catch (Exception ex) {
				showErrorMessage(ex.toString());
	            }

			mSessionID = null;
			System.runFinalization();
					// supposed to call the unreferenced() method on the server object

			showBusyMessage("");
			}
		}

	private void getNewSession() {
		if (mSessionID == null) {
			showBusyMessage("Opening session...");

			mSessionID = (String)getResponse(REQUEST_NEW_SESSION);
			if (mSessionID != null)
				mSessionServerURL = isUseSecondaryServer() ? getSecondaryServerURL() : getPrimaryServerURL();

			showBusyMessage("");
			}
		}

	/**
	 * Tries to get a proper response or search result from the primary server.
	 * If the primary server cannot be contacted and a secondary server exists,
	 * then the secondary server is contacted and in case of a successful completion
	 * used for further getResponse() calls. In case of connection problems or other
	 * errors a proper error message is shown through showErrorMessage().
	 * @param request
	 * @param keyValuePair
	 * @return null in case of any error
	 */
	public Object getResponse(String request, String... keyValuePair) {
		boolean mayUseSecondaryServer = (getSecondaryServerURL() != null && mSessionServerURL == null);

		if (!isUseSecondaryServer() || mSessionServerURL != null) {
			try {
				String url = (mSessionServerURL != null) ? mSessionServerURL : getPrimaryServerURL();
				Object response = getResponse(url, request, keyValuePair);
				if (response != null)
					return response;
				}
			catch (ServerErrorException see) {  // server reached, but could not satisfy request
				showErrorMessage(see.getMessage());
				return null;
				}
			catch (ConnectException ce) {  // connection refused
				if (!mayUseSecondaryServer) {
					showErrorMessage(ce.toString());
					return null;
					}
				showBusyMessage("Connection refused. Trying alternative server...");
				}
			catch (SocketTimeoutException ste) {  // timed out
				if (!mayUseSecondaryServer) {
					showErrorMessage(ste.toString());
					return null;
					}
				showBusyMessage("Connection timed out. Trying alternative server...");
				}
			catch (IOException ioe) {
				showErrorMessage(ioe.toString());
				return null;
				}
			}

		if (mayUseSecondaryServer) {
			try {
				Object response = getResponse(getSecondaryServerURL(), request, keyValuePair);
				if (response != null) {
					setUseSecondaryServer();
					return response;
					}
				showErrorMessage("No response from neither primary nor fail-over server.");
				return null;
				}
			catch (IOException ioe) {
				showErrorMessage(ioe.toString());
				return null;
				}
			}

		showErrorMessage("No response from server.");
		return null;
		}

	private Object getResponse(String serverURL, String request, String... keyValuePair) throws IOException {
		if (mWithSessions && mSessionID == null) {
			getNewSession();
			if (mSessionID == null)
				return null;
			}

		showBusyMessage("Requesting data ...");
        URLConnection con = getConnection(serverURL);

// The default is a GET request, which is limited on Apache to 8700 characters.
// As long as we use Apache as entry door to distribute our requests to virtual
// servers, this may be a problem.
        convertToPostRequest((HttpURLConnection)con, request, keyValuePair);

// This would add the key value pairs as GET request params
//	        con.addRequestProperty(KEY_REQUEST, request);
// 	        con.addRequestProperty(KEY_APP_NAME, mApplicationName);
//          for (int i=0; i<keyValuePair.length; i+=2)
//          	con.addRequestProperty(keyValuePair[i], keyValuePair[i+1]);

        String response = getResponse(con);

        if (BODY_ERROR_INVALID_SESSION.equals(response))
			mSessionID = null;

		showBusyMessage("");

		if (response == null)
        	return null;
		else if (response.startsWith(BODY_MESSAGE))
			return response.substring(BODY_MESSAGE.length() + 1).trim();
        else if (response.startsWith(BODY_OBJECT))
	        return decode(response.substring(BODY_OBJECT.length() + 1));
		else if (response.startsWith(BODY_ERROR))
			throw new ServerErrorException(response);
        else
	        throw new ServerErrorException("Unexpected response:" + (response.length()<40 ? response : response.substring(0, 40) + "..."));
		}
	}