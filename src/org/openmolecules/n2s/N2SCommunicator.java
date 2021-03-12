package org.openmolecules.n2s;

import com.actelion.research.calc.ProgressController;
import org.openmolecules.comm.ClientCommunicator;

public class N2SCommunicator extends ClientCommunicator implements N2SServerConstants {
//  public static final String sURL_1 = "http://localhost:8082";
//	public static final String sURL_1 = "http://n2s.openmolecules.org";
//	public static final String sURL_1 = "http://chasl-cloudp02.idorsia.com:8082";
//	public static final String sURL_1 = "http://chalus-smicro2.idorsia.com:8082";

	private static final String sURL_1 = "https://n2s.openmolecules.org";
	private static final String sURL_2 = "http://87.102.212.253:8082";
	private static String sPrimaryURL = sURL_1;
	private static String sSecondaryURL = sURL_2;
	private static boolean sUseSecondaryServer = false;

	private ProgressController mProgressController;
	private boolean mConnectionProblem;
	private int mErrorCount;

	public static void setServerURL(String url) {
		sPrimaryURL = url;
		sSecondaryURL = sURL_1;
	}

	public N2SCommunicator(ProgressController task, String applicationName) {
		super(false, applicationName);
		mProgressController = task;
		mErrorCount = 0;
	}

	public String getIDCode(String name) {
		return (String)getResponse("idcode", KEY_STRUCTURE_NAME, name);
	}

	public String getMolfile(String name) {
		return (String)getResponse("molfile", KEY_STRUCTURE_NAME, name);
	}

	public String getSmiles(String name) {
		return (String)getResponse("smiles", KEY_STRUCTURE_NAME, name);
	}

	public String getIDCodeList(String nameList) {
		return (String)getResponse("multiple", KEY_STRUCTURE_NAME, nameList);
	}

	public boolean hasConnectionProblem() {
		return mConnectionProblem;
	}

	public int getErrorCount() {
		return mErrorCount;
	}

	@Override
	public String getPrimaryServerURL() {
		return sPrimaryURL;
	}

	@Override
	public String getSecondaryServerURL() {
		return sSecondaryURL;
	}

	@Override
	public boolean isUseSecondaryServer() {
		return sUseSecondaryServer;
		}

	@Override
	public void setUseSecondaryServer() {
		sUseSecondaryServer = true;
		}

	@Override
	public void showBusyMessage(String message) {
	}

	@Override
	public void showErrorMessage(String message) {
		if (message.startsWith("java.net.")) {
			mConnectionProblem = true;
			if (mProgressController != null)
				mProgressController.showErrorMessage(message);
		}
		else {
			mErrorCount++;
		}
	}
}
