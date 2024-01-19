package com.actelion.research.datawarrior.task.db;

import com.actelion.research.calc.ProgressController;
import org.openmolecules.comm.ClientCommunicator;

import java.util.TreeMap;

public class PatentReactionCommunicator extends ClientCommunicator implements PatentReactionServerConstants {
	public static final String SERVER_URL = "https://rxns.openmolecules.org";
//	public static final String SERVER_URL = "http://localhost:8088";

	private static String sPrimaryURL = SERVER_URL;
	private static String sSecondaryURL = null;
	private static boolean sUseSecondaryServer = false;

	// TODO take these from server MercuryServerConstants:
	private static final String REQUEST_GET_QUERYABLE_FIELDS = "getFieldNames";

	private final ProgressController mProgressController;

	public PatentReactionCommunicator(ProgressController task, String applicationName) {
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

	public Object[][] search(TreeMap<String,Object> query) {
		return (Object[][])getResponse(REQUEST_RUN_QUERY_GET_TABLE, KEY_QUERY, encode(query));
		}

	public String[] getQueryFields() {
		String queryFields = (String)getResponse(REQUEST_GET_QUERYABLE_FIELDS);
		return queryFields == null || queryFields.isEmpty() ? null : queryFields.split(",");
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
