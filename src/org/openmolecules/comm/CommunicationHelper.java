/*
 * @(#)CommunicationHelper.java
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.net.ssl.HttpsURLConnection;

public class CommunicationHelper implements CommunicationConstants {
	private static final String USER_AGENT = "Mozilla/5.0";

	public static Object decode(String text) {
        Object decoded = null;
        ByteArrayInputStream byteStream = new ByteArrayInputStream(text.getBytes());
        Base64.InputStream base64Stream = new Base64.InputStream(byteStream);
        ZipInputStream zipStream = new ZipInputStream(base64Stream);
        try {
            zipStream.getNextEntry();
            ObjectInputStream objectStream = new ObjectInputStream(zipStream);
            decoded = objectStream.readObject();
            objectStream.close();
            }
        catch (ClassNotFoundException e) {
        	e.printStackTrace();
            decoded = null;
            }
        catch (IOException e) {
        	e.printStackTrace();
            decoded = null;
            }
        return decoded;
        }

    public static String encode(Serializable object) {
        String encoded = null;
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        Base64.OutputStream base64Stream = new Base64.OutputStream(byteStream);
        ZipOutputStream zipStream = new ZipOutputStream(base64Stream);
        try {
            zipStream.putNextEntry(new ZipEntry("z"));
            ObjectOutputStream objectStream = new ObjectOutputStream(zipStream);
            objectStream.writeObject(object);
            objectStream.flush();
            zipStream.closeEntry();
            encoded = byteStream.toString();
            objectStream.close();
            }
        catch (IOException e) {
        	e.printStackTrace();
            encoded = null;
            }
        return encoded;
        }

	public static String sendGet(String url) throws Exception {
		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
 
		// optional default is GET
		con.setRequestMethod("GET");
 
		//add request header
		con.setRequestProperty("User-Agent", USER_AGENT);
 
//		int responseCode = con.getResponseCode();	// 200 or 401
 
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
 
		while ((inputLine = in.readLine()) != null)
			response.append(inputLine);

		in.close();
 
		return response.toString();
		}

	/**
	 * @param url
	 * @param params e.g. what=molfile&name=Acetone
	 * @throws Exception
	 */
	public static String sendPost(String url, String params) throws Exception {
		HttpsURLConnection con = (HttpsURLConnection)new URL(url).openConnection();
 
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
//		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
 
		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(params);
		wr.close();
 
//		int responseCode = con.getResponseCode();	// 200 or 401
 
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
 
		while ((inputLine = in.readLine()) != null)
			response.append(inputLine);

		in.close();
 
		return response.toString();
		}
	}
