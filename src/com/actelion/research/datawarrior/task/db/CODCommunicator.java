/*
 * Copyright 2016, Thomas Sander, openmolecules.org
 *
 * This file is part of COD-Structure-Server.
 *
 * COD-Structure-Server is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * COD-Structure-Server is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with COD-Structure-Server.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package com.actelion.research.datawarrior.task.db;

import com.actelion.research.calc.ProgressController;
import org.openmolecules.cod.CODServerConstants;
import org.openmolecules.comm.ClientCommunicator;

import java.util.TreeMap;

public class CODCommunicator extends ClientCommunicator implements CODServerConstants {
//	private static final String SERVER_URL = "http://localhost:8086";
	private static final String sURL_1 = "https://cod.openmolecules.org";
	private static final String sURL_2 = "http://87.102.212.253:8086";
	private static String sPrimaryURL = sURL_1;
	private static String sSecondaryURL = sURL_2;
	private static boolean sUseSecondaryServer = false;

	private final ProgressController mProgressController;

	public CODCommunicator(ProgressController task, String applicationName) {
		super(false, applicationName);
		mProgressController = task;
		}

	@Override
	public String getPrimaryServerURL() {
		return sPrimaryURL;
		}

	@Override
	public String getSecondaryServerURL() {
		return sSecondaryURL;
	}

	public static void setPrimaryServerURL(String serverURL) {
		sPrimaryURL = serverURL;
	}

	public static void setSecondaryServerURL(String serverURL) {
		sSecondaryURL = serverURL;
	}

	@Override
	public boolean isUseSecondaryServer() {
		return sUseSecondaryServer;
		}

	@Override
	public void setUseSecondaryServer() {
		sUseSecondaryServer = true;
		}

	public byte[][][] search(TreeMap<String,Object> query) {
		return (byte[][][])getResponse(REQUEST_RUN_QUERY, KEY_QUERY, encode(query));
		}

	public byte[] getTemplate() {
		return (byte[])getResponse(REQUEST_TEMPLATE);
		}

	@Override
	public void showBusyMessage(String message) {
		if (message.isEmpty())
			System.out.println("Done");
		else
			System.out.println("Busy: "+message);
		}

	@Override
	public void showErrorMessage(String message) {
		if (mProgressController != null)
			mProgressController.showErrorMessage(message);
		else
			System.out.println("Error: "+message);
		}
	}
