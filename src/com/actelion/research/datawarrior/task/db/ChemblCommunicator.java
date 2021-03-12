/*
 * Copyright 2017 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91, CH-4123 Allschwil, Switzerland
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
import org.openmolecules.chembl.ChemblServerConstants;
import org.openmolecules.comm.ClientCommunicator;

import java.util.TreeMap;

public class ChemblCommunicator extends ClientCommunicator implements ChemblServerConstants {
//	public static final String sURL_1 = "http://localhost:8083";			// this only used by the client
	private static final String sURL_1 = "https://chembl.openmolecules.org";
	private static final String sURL_2 = "http://87.102.212.253:8083";
	private static String sPrimaryURL = sURL_1;
	private static String sSecondaryURL = sURL_2;
	private static boolean sUseSecondaryServer = false;

	private ProgressController mProgressController;

	public static void setServerURL(String url) {
		sPrimaryURL = url;
		sSecondaryURL = sURL_1;
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

	public ChemblCommunicator(ProgressController task, String applicationName) {
		super(false, applicationName);
		mProgressController = task;
		}

	public Object[] getVersionAndTargets() {
		return (Object[])getResponse(REQUEST_GET_VERSION_AND_TARGETS);
		}

	public byte[][][] getProteinClassDictionary() {
		return (byte[][][])getResponse(REQUEST_GET_PROTEIN_CLASS_DICTIONARY);
		}

	public byte[][][] search(TreeMap<String,Object> query) {
		return (byte[][][])getResponse(REQUEST_RUN_QUERY, KEY_QUERY, encode(query));
		}

	public byte[][][] getAssayDetailTable(byte[][] assayID) {
		return (byte[][][])getResponse(REQUEST_GET_ASSAY_DETAILS, KEY_ID_LIST, encode(assayID));
		}

	/**
	 * This runs a skelSpheres search against many(!) given molecules
	 * @param idcode
	 * @return
	 */
	public byte[][][] findActiveCompoundsSkelSpheres(byte[][] idcode) {
		return (byte[][][])getResponse(REQUEST_FIND_ACTIVES_SKELSPHERES, KEY_IDCODE_LIST, encode(idcode));
		}

	/**
	 * This runs a flexophore and skelSPheres search against one(!) given molecule
	 * @param idcode
	 * @return
	 */
	public byte[][][] findActiveCompoundsFlexophore(byte[] idcode) {
		return (byte[][][])getResponse(REQUEST_FIND_ACTIVES_FLEXOPHORE, KEY_IDCODE, encode(idcode));
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
