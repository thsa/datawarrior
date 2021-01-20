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

package com.actelion.research.table.model;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.gui.JProgressDialog;
import com.actelion.research.gui.form.ReferenceResolver;
import com.actelion.research.gui.form.ReferencedDataConsumer;
import com.actelion.research.gui.form.RemoteDetailSource;
import com.actelion.research.gui.form.ResultDetailPopupItemProvider;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class CompoundTableDetailHandler implements ResultDetailPopupItemProvider,ReferenceResolver,Runnable {
	public static final String URL_RESPONSE = "url/response:";
	public static final String EMBEDDED = "embedded";
	public static final String ABSOLUTE_PATH = "absPath:";
	public static final String RELATIVE_PATH = "relPath:";

    private CompoundTableModel		mTableModel;
	private HashMap<String,byte[]>	mEmbeddedDetailMap;
	private ArrayList<FileRequest>	mFileRequests;

	/**
	 * This is a helper method to extract all detail references that refer to embedded details.
	 * It assumes that the column's properties referring to detail information is up-to-date.
	 * @param column
	 * @param value cell content with appended detail
	 * @param detailIDSet the tree set to accumulate the detail IDs
	 */
	public void extractEmbeddedDetailReferences(int column, String value, TreeSet<String> detailIDSet) {
		if (value == null)
			return;
		String detailSeparator = mTableModel.getDetailSeparator(column);
		int i1 = value.indexOf(detailSeparator);
		while (i1 != -1) {
			int i2 = value.indexOf(CompoundTableConstants.cDetailIndexSeparator, i1);
			String detailIndex = value.substring(i1+detailSeparator.length(), i2);
			i1 = value.indexOf(detailSeparator, i2);
			if (EMBEDDED.equals(mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyDetailSource+detailIndex)))
				detailIDSet.add(i1 == -1 ? value.substring(i2+1) : value.substring(i2+1, i1));
			}
		}

	public CompoundTableDetailHandler(CompoundTableModel tableModel) {
        mTableModel = tableModel;
		}

	@Override
	public ArrayList<JMenuItem> getExternalPopupItems(RemoteDetailSource source, String reference) {
		return null;
		}

	@Override
	public void requestData(RemoteDetailSource sourceSpec, String reference, int mode, ReferencedDataConsumer consumer) {
		String source = sourceSpec.getSource();
		if (source.equals(EMBEDDED))
			consumer.setReferencedData(reference, getEmbeddedDetail(reference));
        else if (source.startsWith(ABSOLUTE_PATH)
              || source.startsWith(RELATIVE_PATH))
			requestDataFromFile(source, reference, consumer);
        else if (source.startsWith(URL_RESPONSE)) {
			requestURLResponse(source, source.substring(URL_RESPONSE.length()), reference, consumer);
			return;
			}
		}

	@Override
	public byte[] resolveReference(RemoteDetailSource sourceSpec, String reference, int mode) {
		String source = sourceSpec.getSource();
        if (source.equals(EMBEDDED))
			return getEmbeddedDetail(reference);
		if (source.startsWith(ABSOLUTE_PATH)
         || source.startsWith(RELATIVE_PATH))
			return getDataFromFile(getPathFromFileSource(source), reference);
		if (source.startsWith(URL_RESPONSE))
			return getURLResponse(source.substring(URL_RESPONSE.length()), reference);

		return null;
		}

	public int getEmbeddedDetailCount() {
		return (mEmbeddedDetailMap == null) ? 0 : mEmbeddedDetailMap.size();
		}

    private byte[] getEmbeddedDetail(String reference) {
		if (mEmbeddedDetailMap == null)
			return null;
		byte[] detailData = mEmbeddedDetailMap.get(reference);
		return (reference.startsWith("-")) ? unzip(detailData) : detailData;
		}

	/**
	 *
	 * @param path may contain a %s as place holder for the key, otherwise key is file name without path
	 * @param key is either file name without path or is part of file name, if path contains %s
	 * @return
	 */
	private byte[] getDataFromFile(String path, String key) {
		String filename = path.contains("%s") ? path.replace("%s", key) : path.concat(key);
		File file = new File(filename);
		byte[] data = new byte[(int)file.length()];
		try {
			FileInputStream stream = new FileInputStream(file);
			stream.read(data);
			stream.close();
			}
		catch (IOException ioe) {
			return null;
			}
		return data;
		}

	private void requestDataFromFile(String source, String reference, ReferencedDataConsumer consumer) {
		if (mFileRequests == null)
			mFileRequests = new ArrayList<>();
		synchronized(mFileRequests) {
			mFileRequests.add(new FileRequest(source, reference, consumer));
			}
		new Thread(this).run();
		}

	public void run() {
		FileRequest request = null;
		synchronized(mFileRequests) {
			request = mFileRequests.get(0);
			mFileRequests.remove(0);
			}
		final byte[] data = getDataFromFile(getPathFromFileSource(request.source), request.reference);
		final FileRequest _request = request;
		SwingUtilities.invokeLater(() -> _request.consumer.setReferencedData(_request.reference, data) );
		}

    private String getPathFromFileSource(String source) {
        if (source.startsWith(ABSOLUTE_PATH))
            return source.substring(ABSOLUTE_PATH.length());

        if (source.startsWith(RELATIVE_PATH)) {
            String path = mTableModel.getFile().getParent()+ File.separator;
            String relPath = source.substring(RELATIVE_PATH.length());
            if (File.separatorChar == '/')
                relPath = relPath.replace('\\', '/');
            else
                relPath = relPath.replace('/', '\\');
            return path + relPath;
            }
            
        return null;
        }

    protected void clearDetailData() {
		mEmbeddedDetailMap = null;
		}

	public void setEmbeddedDetail(String reference, byte[] detail) {
		if (mEmbeddedDetailMap == null)
			mEmbeddedDetailMap = new HashMap<>();

		mEmbeddedDetailMap.put(reference, detail);
		}

	public void setEmbeddedDetailMap(HashMap<String,byte[]> details) {
		mEmbeddedDetailMap = details;
		}

	public HashMap<String,byte[]> getEmbeddedDetailMap() {
		return mEmbeddedDetailMap;
		}

	public HashMap<String,String> embedDetails(Object[] oldKey, CompoundTableDetailSpecification sourceSpec,
												int mode, String type, JProgressDialog progressDialog) {
			// tries to embed all details referenced with oldKey
			// if it doesn't succeed with all then nothing is changed and null is returned
	    if (progressDialog != null)
	        progressDialog.startProgress("Embedding Detail Data...", 0, oldKey.length);

	    int detailID = getAvailableEmbeddedDetailID();
		HashMap<String,String> oldToNewKeyMap = new HashMap<String,String>();
		HashMap<String,byte[]> detail = new HashMap<>();
		for (int i=0; i<oldKey.length; i++) {
		    if (progressDialog != null) {
		        if (progressDialog.threadMustDie())
		            return null;
		        progressDialog.updateProgress(i);
		    	}

		    String newKey = oldToNewKeyMap.get(oldKey[i]);
			if (newKey == null) {
				byte[] detailData = resolveReference(sourceSpec, (String)oldKey[i], mode);
				if (detailData == null)
					continue;

				if (isZipType(type)) {
					detailData = zip(detailData);
					if (detailData == null)
						return null;
					newKey = "-"+detailID;
					}
				else {
					newKey = ""+detailID;
					}
				detail.put(newKey, detailData);
				oldToNewKeyMap.put((String)oldKey[i], newKey);
				detailID++;
				}
			}

		if (mEmbeddedDetailMap == null)
			mEmbeddedDetailMap = detail;
		else
			mEmbeddedDetailMap.putAll(detail);

		return oldToNewKeyMap;
		}

	private boolean isZipType(String type) {
		return (type.equals("text/plain")
			 || type.equals("text/html")
			 || type.equals("image/svg"));
		}

	private byte[] zip(byte[] data) {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		ZipOutputStream zipStream = new ZipOutputStream(byteStream);
		try {
			zipStream.putNextEntry(new ZipEntry("z"));
			zipStream.write(data);
			zipStream.closeEntry();
			zipStream.close();
			}
		catch (IOException ioe) {
			return null;
			}
		return byteStream.toByteArray();
		}

	private byte[] unzip(byte[] data) {
		if (data == null)
			return null;

		ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(data));
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		try {
			zipStream.getNextEntry();
			int b = zipStream.read();
			while (b != -1) {
				byteStream.write(b);
				b = zipStream.read();
				}
			zipStream.close();
			}
		catch (IOException ioe) {
			return null;
			}
		return byteStream.toByteArray();
		}

	private int getAvailableEmbeddedDetailID() {
		int highID = 0;
		if (getEmbeddedDetailCount() != 0) {
			for (String id:mEmbeddedDetailMap.keySet()) {
				try {
					int num = Integer.parseInt(id);
					if (highID < Math.abs(num))
						highID = Math.abs(num);
					}
				catch (NumberFormatException nfe) {}
				}
			}
		return highID+1;
		}

	protected void addOffsetToEmbeddedDetailIDs(int offset) {
		HashMap<String,byte[]> oldDetailMap = mEmbeddedDetailMap;
		mEmbeddedDetailMap = new HashMap<String,byte[]>();
		Iterator<String> iterator = oldDetailMap.keySet().iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			try {
				int id = Integer.parseInt(key);
				int newID = (id < 0) ? id-offset : id+offset;
				mEmbeddedDetailMap.put(""+newID, oldDetailMap.get(key));
				}
			catch (NumberFormatException nfe) {
				mEmbeddedDetailMap.put(key, oldDetailMap.get(key));
				}
			}
		}

	protected byte[] getURLResponse(String url, String reference) {
		byte[] bytes = null;
		try {
			URL u = new URL(url.replaceAll("%s", reference));
			InputStream in = new BufferedInputStream(u.openStream());
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[1024];
			int n = 0;
			while (-1!=(n=in.read(buf)))
				out.write(buf, 0, n);
	
			bytes = out.toByteArray();

			out.close();
			in.close();
			}
		catch (Exception e) {
			bytes = null;
			}

		return bytes;
		}

	protected void requestURLResponse(final String source, final String url, final String reference, final ReferencedDataConsumer consumer) {
		new Thread(() -> {
			final byte[] response = getURLResponse(url, reference);
			SwingUtilities.invokeLater(() -> consumer.setReferencedData(reference, response) );
			} ).start();
		}
	}

class FileRequest {
	public String source;
	public String reference;
	public ReferencedDataConsumer consumer;

	public FileRequest(String source, String reference, ReferencedDataConsumer consumer) {
		this.source = source;
		this.reference = reference;
		this.consumer = consumer;
		}
	}
