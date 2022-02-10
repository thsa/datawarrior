/*
 * Copyright 2019 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91, CH-4123 Allschwil, Switzerland
 *
 * This file is part of DataWarrior.
 *
 * DataWarrior is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * DataWarrior is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with DataWarrior.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package com.actelion.research.datawarrior.task.db;

import com.actelion.research.calc.ProgressController;
import org.openmolecules.comm.ClientCommunicator;

import java.util.TreeMap;

import static org.openmolecules.bb.BBServerConstants.REQUEST_PROVIDER_LIST;

public class BBCommunicator extends ClientCommunicator {
//	private static final String sURL_1 = "http://localhost:8087";
	private static final String sURL_1 = "https://bb.openmolecules.org";
	private static final String sURL_2 = "http://87.102.212.253:8087";
	private static String sPrimaryURL = sURL_1;
	private static String sSecondaryURL = sURL_2;
	private static boolean sUseSecondaryServer = false;

	private static String[] sProviderList;

	private ProgressController mProgressController;

	public BBCommunicator(ProgressController task, String applicationName) {
		super(false, applicationName);
		mProgressController = task;
	}
/*
	public Object[] getLastUpdateDate() {
		return (Object[])getResponse(REQUEST_GET_LAST_UPDATE_DATE, KEY_APP_NAME, mAppName);
	}
*/
	public byte[][][] search(TreeMap<String,Object> query) {
		return (byte[][][])getResponse(REQUEST_RUN_QUERY, KEY_QUERY, encode(query));
	}

	@Override
	public String getPrimaryServerURL() {
		return sPrimaryURL;
		}

	@Override
	public String getSecondaryServerURL() {
		return sSecondaryURL;
		}

	public static void setServerURL(String serverURL) {
		sPrimaryURL = serverURL;
		sSecondaryURL = sURL_1;
		}

	@Override
	public boolean isUseSecondaryServer() {
		return sUseSecondaryServer;
		}

	@Override
	public void setUseSecondaryServer() {
		sUseSecondaryServer = true;
		}

	public String[] getProviderList() {
		if (sProviderList == null) {
			sProviderList = (String[])getResponse(REQUEST_PROVIDER_LIST);
			if (sProviderList == null)
				sProviderList = new String[0];
			}
		return sProviderList;
	}

	@Override
	public void showBusyMessage(String message) {
		if (message.length() == 0)
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
