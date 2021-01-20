package com.actelion.research.datawarrior;

import com.actelion.research.util.BrowserControl;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import java.awt.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class ICSynthCommunicator {
	private static final String BASE_URL = "https://icsynth-teaser.infochem.de";
	private static final String URL_LOGIN = "/login";
	private static final String URL_QUERY = "/api/query";
	private static final String KEY_USER_NAME = "username";
	private static final String KEY_PASSWORD = "password";
	private static final String KEY_FORMAT = "format";
	private static final String KEY_STRUCTURE = "structure";
	private static final String VALUE_USER_NAME = "?";
	private static final String VALUE_PASSWORD = "?";
	private static final String VALUE_FORMAT_SMILES = "SMILES";

	private static String sToken;
	private Frame mParentFrame;

	public ICSynthCommunicator(Frame parent) {
		mParentFrame = parent;
		}

	private URLConnection getConnection(String url) throws MalformedURLException, IOException {
		URL urlServlet = new URL(BASE_URL.concat(url));
		HttpsURLConnection con = (HttpsURLConnection)urlServlet.openConnection();

		// konfigurieren
		con.setRequestMethod("POST");
		con.setDoInput(true);
		con.setDoOutput(true);
		con.setUseCaches(false);
		con.setRequestProperty("User-Agent", "Mozilla/5.0");
		con.setRequestProperty("Content-Type", "application/x-java-serialized-object");

		return con;
		}

	public boolean getToken() {
		if (sToken != null)
			return true;

		try {
			HttpsURLConnection con = (HttpsURLConnection)getConnection(URL_LOGIN);
			con.addRequestProperty(KEY_USER_NAME, VALUE_USER_NAME);
			con.addRequestProperty(KEY_PASSWORD, VALUE_PASSWORD);
			String response = getResponse(con);
			if (response == null)
				return false;

			if (response.contains("Bearer"))
				response = response.substring(response.indexOf("Bearer")+6).trim();

			System.out.println("Token:"+response);

			sToken = response;
			}
		catch (Exception ex) {
			showErrorMessage(ex.toString());
			return false;
			}

		showBusyMessage("");
		return true;
		}

	public boolean suggestSynthesis(String smiles) {
		if (!getToken())
			return false;

		try {
			HttpsURLConnection con = (HttpsURLConnection)getConnection(URL_QUERY);
			con.setRequestProperty("Authorization", "Bearer " + sToken);
			con.addRequestProperty(KEY_FORMAT, VALUE_FORMAT_SMILES);
			con.addRequestProperty(KEY_STRUCTURE, smiles);
			String response = getResponse(con);
			if (response == null)
				return false;

			System.out.println("resultURL:"+response);
			BrowserControl.displayURL(response);
			return true;
			}
		catch (Exception ex) {
			showErrorMessage(ex.toString());
			return false;
			}
		}

	private String getResponse(HttpsURLConnection con) {
		try {
			int returnCode = con.getResponseCode();
			if (returnCode != 200) {
//				InputStream is = con.getErrorStream();
				showErrorMessage("No URL response. Error code:"+returnCode);
				}
			else {
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
				if (sb.length() != 0)
					return sb.toString();
				}
			}
		catch (IOException ioe) {
			showErrorMessage(ioe.toString());
			}
		catch (Throwable t) {
			showErrorMessage(t.toString());
			}

		return null;
	}

	private void showBusyMessage(String msg) {
		System.out.println(msg);
		}

	private void showErrorMessage(String msg) {
		JOptionPane.showMessageDialog(mParentFrame, msg, "Communication Error", JOptionPane.ERROR_MESSAGE);
		}
	}
