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

package com.actelion.research.table;

import com.actelion.research.calc.*;
import com.actelion.research.chem.SSSearcherWithIndex;
import com.actelion.research.chem.descriptor.*;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.DoubleFormat;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.TreeMap;
import java.util.TreeSet;

public class CompoundTableSOM implements SOMController {
	public static final int SOM_TYPE_DOUBLE = 0;
	private static final int SOM_TYPE_BINARY = 1;

	public static final String[] SOM_TYPE_FILE = {"doubleVector", "binaryFP"};

	public static final String[] SOM_ANALYSIS_COLUMN_NAME = {"x", "y", "border dissimilarity"};

	private SelfOrganizedMap	mSOM;
	private CompoundTableModel	mTableModel;
	private int[]				mColumnList,mVaryingKeyInt,mUsedRowIndex;
	private long[]              mVaryingKeyLong;
	private int					mType,mParameterCount,mInputVectorCount,mPivotGroupColumn,mPivotDataColumn,mReactionCenterBitCount;
	private double[]			mRowParameter;
	private double[][]			mPivotValue;
	private TreeMap<String,Integer> mPivotDataMap;
	private String				mCompatibilityError;
	private boolean				mIsReactionFP;

	public static boolean isDescriptorSupported(String shortName) {
		return DescriptorHelper.isDescriptorShortName(shortName)
			&& (DescriptorHelper.isBinaryFingerprint(shortName)
			 || shortName.equals(DescriptorConstants.DESCRIPTOR_SkeletonSpheres.shortName)
			 || shortName.equals(DescriptorConstants.DESCRIPTOR_ReactionFP.shortName));
		}

	public CompoundTableSOM(CompoundTableModel tableModel, int type) {
			// constructor to be used if SOM interna are read from a SOM file with read()
		if (type == SOM_TYPE_DOUBLE)
			mSOM = createReactionFPAwareVectorSOM();
		else if (type == SOM_TYPE_BINARY)
			mSOM = new BinarySOM();

		mTableModel = tableModel;
		mType = type;
		mPivotGroupColumn = -1;
		mPivotDataColumn = -1;
		mPivotDataMap = null;   // will be set by SOM file;
		}

	public CompoundTableSOM(int nx, int ny, int mode, CompoundTableModel tableModel, int[] columnList) {
		mSOM = createReactionFPAwareVectorSOM();
		mSOM.initializeReferenceVectors(nx, ny, mode);
		mTableModel = tableModel;
		mColumnList = columnList;
		mType = SOM_TYPE_DOUBLE;
		mPivotGroupColumn = -1;
		mPivotDataColumn = -1;
		mPivotDataMap = null;
		}

	private VectorSOM createReactionFPAwareVectorSOM() {
		return new VectorSOM() {
			@Override public double getDissimilarity(Object vector1, Object vector2) {
				if (!mIsReactionFP)
					return super.getDissimilarity(vector1, vector2);

				double[] v1 = (double[])vector1;
				double[] v2 = (double[])vector2;

				double reactionCenterSum = 0.0;
				for (int i=0; i<mReactionCenterBitCount; i++) {
					double dif = Math.abs(v1[i] - v2[i]);
					reactionCenterSum += dif * dif;
				}

				double peripherySum = 0.0;
				for (int i=mReactionCenterBitCount; i<v1.length; i++) {
					double dif = Math.abs(v1[i] - v2[i]);
					peripherySum += dif * dif;
				}

				// euclidian dissimilarity normalized by SQRT(dimensionCount)
				double reactionCenterDissimilarity = (mReactionCenterBitCount == 0) ? 1.0 : Math.sqrt(reactionCenterSum)/Math.sqrt(mReactionCenterBitCount);
				double peripheryDissimilarity = (v1.length-mReactionCenterBitCount == 0) ? 1.0 : Math.sqrt(peripherySum)/Math.sqrt(v1.length-mReactionCenterBitCount);

				return DescriptorHandlerReactionFP.REACTION_CENTER_WEIGHT * reactionCenterDissimilarity
						+ DescriptorHandlerReactionFP.PERIPHERY_WEIGHT * peripheryDissimilarity;
				}
			};
		}

	public void addProgressListener(ProgressListener l) {
		mSOM.addProgressListener(l);
		}

	public void setThreadMaster(ThreadMaster t) {
		mSOM.setThreadMaster(t);
		}

	public int getWidth() {
		return mSOM.getWidth();
		}

	public int getHeight() {
		return mSOM.getHeight();
		}

	public void setPivotColumns(int groupColumn, int dataColumn) {
		mPivotGroupColumn = groupColumn;
		mPivotDataColumn = dataColumn;
		}

	public void organize() {
		mSOM.setController(this);

		if (mType == SOM_TYPE_DOUBLE) {
			mParameterCount = mColumnList.length;
			if (mTableModel.isDescriptorColumn(mColumnList[0])) {
				mSOM.startProgress("Analyzing descriptor...", 0, 0);
				Object descriptor = mTableModel.getTotalRecord(0).getData(mColumnList[0]);
				if (descriptor instanceof long[]) {
					mIsReactionFP = (mTableModel.getDescriptorHandler(mColumnList[0]) instanceof DescriptorHandlerReactionFP);

					long[] firstIndex = (long[])descriptor;
					mVaryingKeyLong = new long[firstIndex.length];
					for (int row=1; row<mTableModel.getTotalRowCount(); row++) {
						long[] currentIndex = (long[])mTableModel.getTotalRecord(row).getData(mColumnList[0]);
						if (currentIndex != null && currentIndex.length != 0)
							for (int i=0; i<firstIndex.length; i++)
								mVaryingKeyLong[i] |= (firstIndex[i] ^ currentIndex[i]);
					}

					int varyingBits = 0;
					for (int i=0; i<firstIndex.length; i++)
						varyingBits += Long.bitCount(mVaryingKeyLong[i]);

					mParameterCount += varyingBits - 1;

					if (mIsReactionFP) {
						mReactionCenterBitCount = 0;
						for (int i=0; i<DescriptorHandlerReactionFP.REACTION_CENTER_LONG_COUNT; i++)
							mReactionCenterBitCount += Long.bitCount(mVaryingKeyLong[i]);
						}
					}
				else if (descriptor instanceof int[]) {
					int[] firstIndex = (int[])descriptor;
					mVaryingKeyInt = new int[firstIndex.length];
					for (int row=1; row<mTableModel.getTotalRowCount(); row++) {
						int[] currentIndex = (int[])mTableModel.getTotalRecord(row).getData(mColumnList[0]);
						if (currentIndex != null && currentIndex.length != 0)
							for (int i=0; i<firstIndex.length; i++)
								mVaryingKeyInt[i] |= (firstIndex[i] ^ currentIndex[i]);
						}
		
					int varyingBits = 0;
					for (int i=0; i<firstIndex.length; i++)
						varyingBits += Integer.bitCount(mVaryingKeyInt[i]);

					mParameterCount += varyingBits - 1;
					}
				else {
					byte[] firstIndex = (byte[])descriptor;
					mVaryingKeyInt = new int[(firstIndex.length+31)/32];
					for (int row=1; row<mTableModel.getTotalRowCount(); row++) {
						byte[] currentIndex = (byte[])mTableModel.getTotalRecord(row).getData(mColumnList[0]);
						if (currentIndex != null && currentIndex.length != 0)
							for (int i=0; i<firstIndex.length; i++)
								if (firstIndex[i] != currentIndex[i])
									mVaryingKeyInt[i>>5] |= (1 << (i & 31));
						}
		
					int varyingBits = 0;
					for (int i=0; i<mVaryingKeyInt.length; i++)
						varyingBits += Integer.bitCount(mVaryingKeyInt[i]);

					mParameterCount += varyingBits - 1;
					}
				}

			if (mPivotDataColumn != -1)
				mParameterCount *= mTableModel.getCategoryCount(mPivotDataColumn);

			mRowParameter = new double[mParameterCount];
			((VectorSOM)mSOM).setParameterCount(mParameterCount);
			}

		if (mPivotGroupColumn != -1)
			calculatePivotTable();

		mSOM.organize();
		}

	public void checkCompatibility() throws Exception {
		if (mCompatibilityError != null)
			throw new Exception(mCompatibilityError);
		}
		
	public void positionRecords() {
		if (!mSOM.threadMustDie()) {
			double[][] pivotLocation = null;
			if (mPivotGroupColumn != -1) {  // if we do on-the-fly pivoting
				if (mPivotValue == null)	// if the SOM file was read from file the pivot table was not calculated yet
					calculatePivotTable();

				pivotLocation = new double[mPivotValue.length][];
				for (int i=0; i<mPivotValue.length; i++)
					pivotLocation[i] = findExactMatchLocation(i);
				}

			String[] columnTitle = { "SOM_X", "SOM_Y", "SOM_Fit" };
			int firstNewColumn = mTableModel.addNewColumns(columnTitle);
			mSOM.startProgress("Positioning records...", 0, mTableModel.getTotalRowCount());
			for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
				if (mSOM.threadMustDie())
					break;
				mSOM.updateProgress(row);

				double location[] = (mPivotGroupColumn == -1) ?
						  findExactMatchLocation(row)
						: pivotLocation[(int)mTableModel.getTotalDoubleAt(row, mPivotGroupColumn)];
				if (location != null
				 && (mPivotGroupColumn == -1
				  || mPivotValue[(int)mTableModel.getTotalDoubleAt(row, mPivotGroupColumn)] != null)) {
					mTableModel.setTotalValueAt(DoubleFormat.toString(location[0]+0.5D), row, firstNewColumn);
					mTableModel.setTotalValueAt(DoubleFormat.toString(location[1]+0.5D), row, firstNewColumn+1);
					mTableModel.setTotalValueAt(DoubleFormat.toString(location[2]), row, firstNewColumn+2);
					}
				}
			mSOM.stopProgress("Positioning done");

			if ((mSOM.getCreationMode() & SelfOrganizedMap.cModeTopologyUnlimited) != 0) {
				mTableModel.setColumnProperty(firstNewColumn, CompoundTableModel.cColumnPropertyCyclicDataMax, ""+mSOM.getWidth());
				mTableModel.setColumnProperty(firstNewColumn+1, CompoundTableModel.cColumnPropertyCyclicDataMax, ""+mSOM.getHeight());
				}
			mTableModel.finalizeNewColumns(firstNewColumn, null);
			}
		}

	public void createSimilarityMap(final CompoundTableModel targetTableModel) {
		int xdim = mSOM.getWidth();
		int ydim = mSOM.getHeight();
		if ((mSOM.getCreationMode() & SelfOrganizedMap.cModeTopologyUnlimited) == 0) {
			xdim--;
			ydim--;
			}

		targetTableModel.initializeTable(2 * xdim * ydim, SOM_ANALYSIS_COLUMN_NAME.length);

		for (int column=0; column<SOM_ANALYSIS_COLUMN_NAME.length; column++)
			targetTableModel.setColumnName(SOM_ANALYSIS_COLUMN_NAME[column], column);

		mSOM.startProgress("Calculating dissimilarities...", 0, xdim);
		int row = 0;
		for (int x1=0; x1<xdim; x1++) {
			if (mSOM.threadMustDie())
				break;
			mSOM.updateProgress(x1);

			for (int y1=0; y1<ydim; y1++) {
				int x2 = (x1 == xdim-1) ? 0 : x1+1;
				int y2 = (y1 == ydim-1) ? 0 : y1+1;

				targetTableModel.setTotalValueAt(""+x1, row, 0);
				targetTableModel.setTotalValueAt(""+y1+".5", row, 1);
				targetTableModel.setTotalValueAt(DoubleFormat.toString(Math.sqrt(
						mSOM.getDissimilarity(mSOM.getReferenceVector(x1, y1),
											  mSOM.getReferenceVector(x1, y2)))), row, 2);
				row++;

				targetTableModel.setTotalValueAt(""+x1+".5", row, 0);
				targetTableModel.setTotalValueAt(""+y1, row, 1);
				targetTableModel.setTotalValueAt(DoubleFormat.toString(Math.sqrt(
						mSOM.getDissimilarity(mSOM.getReferenceVector(x1, y1),
											  mSOM.getReferenceVector(x2, y1)))), row, 2);
				row++;
				}
			}

		targetTableModel.finalizeTable(CompoundTableEvent.cSpecifierNoRuntimeProperties, null);
		}

	public BufferedImage createSimilarityMapImage(int width, int height) {
		int xdim = mSOM.getWidth();
		int ydim = mSOM.getHeight();
		boolean isUnlimitedTopology = ((mSOM.getCreationMode() & SelfOrganizedMap.cModeTopologyUnlimited) != 0);

			// calculate base values for the color map on the following positions of the neuron cells:
			// On every corner; on the center of every edge, in the cell center
		double[][] mdif = new double[2*xdim+1][2*ydim+1]; // differences between two neurons on vertical cell edges

		mSOM.startProgress("Creating dissimilarity landscape...", 0, 0);
		for (int y=0; y<ydim; y++) {
			if (mSOM.threadMustDie())
				break;

			// calculate dissimilarities between any horizontally adjacent cells
			for (int x=1; x<xdim; x++)
				mdif[2*x][2*y+1] = Math.sqrt(mSOM.getDissimilarity(mSOM.getReferenceVector(x-1, y),
																   mSOM.getReferenceVector(x, y)));
			}

		if (!mSOM.threadMustDie()) {
			for (int x=0; x<xdim; x++) {
				if (mSOM.threadMustDie())
					break;
	
				// calculate dissimilarities between any vertically adjacent cells
				for (int y=1; y<ydim; y++)
					mdif[2*x+1][2*y] = Math.sqrt(mSOM.getDissimilarity(mSOM.getReferenceVector(x, y-1),
																	   mSOM.getReferenceVector(x, y)));
				}
			}

		if (!mSOM.threadMustDie() && isUnlimitedTopology) {
			for (int x=0; x<xdim; x++) {
				mdif[2*x+1][0] = Math.sqrt(mSOM.getDissimilarity(mSOM.getReferenceVector(x, 0),
																 mSOM.getReferenceVector(x, ydim-1)));
				mdif[2*x+1][2*ydim] = mdif[2*x+1][0];
				}
			for (int y=0; y<ydim; y++) {
				mdif[0][2*y+1] = Math.sqrt(mSOM.getDissimilarity(mSOM.getReferenceVector(0, y),
																 mSOM.getReferenceVector(xdim-1, y)));
				mdif[2*xdim][2*y+1] = mdif[0][2*y+1];
				}
			}

		if (!mSOM.threadMustDie()) {
			for (int x=0; x<=2*xdim; x+=2) {
				for (int y=0; y<=2*ydim; y+=2) {
					double mxdif;
					if (y == 0)
						mxdif = (isUnlimitedTopology) ? (mdif[x][1] + mdif[x][2*ydim-1]) / 2.0 : mdif[x][1];
					else if (y == 2*ydim)
						mxdif = (isUnlimitedTopology) ? (mdif[x][1] + mdif[x][2*ydim-1]) / 2.0 : mdif[x][2*ydim-1];
					else
						mxdif = (mdif[x][y-1] + mdif[x][y+1]) / 2.0;
	
					double mydif;
					if (x == 0)
						mydif = (isUnlimitedTopology) ? (mdif[1][y] + mdif[2*xdim-1][y]) / 2.0 : mdif[1][y];
					else if (x == 2*xdim)
						mydif = (isUnlimitedTopology) ? (mdif[1][y] + mdif[2*xdim-1][y]) / 2.0 : mdif[2*xdim-1][y];
					else
						mydif = (mdif[x-1][y] + mdif[x+1][y]) / 2.0;
	
					mdif[x][y] = Math.max(mxdif, mydif);
					}
				}
			}

		if (!mSOM.threadMustDie()) {
			for (int x=0; x<xdim; x++)
				for (int y=0; y<ydim; y++)
					mdif[2*x+1][2*y+1] = Math.min(mdif[2*x][2*y+1]+mdif[2*x+2][2*y+1], mdif[2*x+1][2*y]+mdif[2*x+1][2*y+2]) / 2.0; 
			}

		BufferedImage image = null;
		if (!mSOM.threadMustDie()) {
			image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			int[] rgb = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
			mSOM.startProgress("Constructing image...", 0, width);
			for (int imagex=0; imagex<width; imagex++) {
				if (mSOM.threadMustDie())
					break;
				mSOM.updateProgress(imagex);
				double xf = (0.5+imagex)*2*xdim/(double)width;
				int x = (int)xf;
				double xWeight = xf - x;
				for (int imagey=0; imagey<height; imagey++) {
					double yf = (0.5+imagey)*2*ydim/(double)height;
					int y = (int)yf;
					double yWeight = yf - y;
					
					double dif1 = xWeight*mdif[x+1][y]+(1.0-xWeight)*mdif[x][y];
					double dif2 = xWeight*mdif[x+1][y+1]+(1.0-xWeight)*mdif[x][y+1];
					double dif = yWeight*dif2+(1.0-yWeight)*dif1;
					rgb[imagex+(height-imagey-1)*width] = 0x80FFFFFF & Color.HSBtoRGB((float)((1.0-dif)/1.5), (float)1.0, (float)1.0);
					}
				}
			}
		return mSOM.threadMustDie() ? null : image;
		}

	public Point findBestMatchLocation(int row) {
		return mSOM.findBestMatchLocation(mSOM.normalizeVector(getRowInputVector(row)));
		}

	protected double[] findExactMatchLocation(int row) {
		return mSOM.findExactMatchLocation(mSOM.normalizeVector(getRowInputVector(row)));
		}

	public int getType() {
		return mType;
		}

	public void write(BufferedWriter writer) throws IOException {
		mSOM.write(writer);

		if (mType == SOM_TYPE_DOUBLE) {
			writer.write("<columnCount=\""+mColumnList.length+"\">");
			writer.newLine();
	
			writer.write("<parameterCount=\""+mParameterCount+"\">");
			writer.newLine();

			if (mPivotValue != null) {
				writer.write("<pivotGroupColumn=\""+mTableModel.getColumnTitleNoAlias(mPivotGroupColumn)+"\">");
				writer.newLine();

				writer.write("<pivotDataColumn=\""+mTableModel.getColumnTitleNoAlias(mPivotDataColumn)+"\">");
				writer.newLine();

				String[] data = mTableModel.getCategoryList(mPivotDataColumn);
				writer.write("<pivotDataCount=\""+data.length+"\">");
				writer.newLine();
				for (int i=0; i<data.length; i++) {
					writer.write("<pivotData=\""+data[i]+"\">");
					writer.newLine();
					}
				}

			for (int i=0; i<mColumnList.length; i++) {
				DescriptorHandler dh = mTableModel.getDescriptorHandler(mColumnList[i]);
				if (dh == null) {
					writer.write("<columnName=\""+mTableModel.getColumnTitleNoAlias(mColumnList[i])+"\">");
					writer.newLine();
					}
				else {
					byte[] encoding = (dh instanceof AbstractDescriptorHandlerLongFP) ?
								new DescriptorEncoder().encodeLong(mVaryingKeyLong)
							  : new DescriptorEncoder().encode(mVaryingKeyInt);
					writer.write("<descriptorName=\""+dh.getInfo().shortName+"\">");
					writer.newLine();
					writer.write("<descriptorVersion=\""+dh.getVersion()+"\">");
					writer.newLine();
					writer.write("<keyList=\""+new String(encoding)+"\">");
					writer.newLine();
					}
				}
			}
/*		else if (mType == SOM_TYPE_BINARY) {	// not supported anymore
			writer.write("<columnName=\""+"fingerprint_"+SSSearcherWithIndex.cIndexVersion+"\">");
			writer.newLine();
			}	*/
	}

	public void read(BufferedReader reader) throws Exception {
		mSOM.read(reader);

		boolean error = false;
		mCompatibilityError = null; // this is used only if positionRecords() is called afterwards
		if (mType == SOM_TYPE_DOUBLE) {
			String theLine = reader.readLine();
			error = !theLine.startsWith("<columnCount=");
			if (!error) {
				mColumnList = new int[Integer.parseInt(SelfOrganizedMap.extractValue(theLine))];
				theLine = reader.readLine();
				error = !theLine.startsWith("<parameterCount=");
				}
	
			if (!error) {
				mParameterCount = Integer.parseInt(SelfOrganizedMap.extractValue(theLine));
				mRowParameter = new double[mParameterCount];
				}

			mPivotGroupColumn = -1;
			mPivotDataColumn = -1;
			if (!error) {
				theLine = reader.readLine();
				if (theLine.startsWith("<pivotGroupColumn=")) {
					String groupColumn = SelfOrganizedMap.extractValue(theLine);
					mPivotGroupColumn = mTableModel.findColumn(groupColumn);
					if (mPivotGroupColumn == -1)
						mCompatibilityError = "Group column for pivoting '"+groupColumn+"' not found in current data.";

					theLine = reader.readLine();
					error = !theLine.startsWith("<pivotDataColumn=");
					if (!error) {
						String dataColumn = SelfOrganizedMap.extractValue(theLine);
						mPivotDataColumn = mTableModel.findColumn(dataColumn);
						if (mPivotDataColumn == -1)
							mCompatibilityError = "Data column for pivoting '"+groupColumn+"' not found in current data.";

						theLine = reader.readLine();
						error = !theLine.startsWith("<pivotDataCount=");
						}

					if (!error) {
						int pivotDataCount = Integer.parseInt(SelfOrganizedMap.extractValue(theLine));
						mPivotDataMap = new TreeMap<String,Integer>();
						TreeSet<String> keySet = new TreeSet<String>();
						if (mPivotDataColumn != -1) {
							String[] keyList = mTableModel.getCategoryList(mPivotDataColumn);
							for (int i=0; i<keyList.length; i++)
								keySet.add(keyList[i]);
							}
						for (int i=0; i<pivotDataCount; i++) {
							theLine = reader.readLine();
							if (!theLine.startsWith("<pivotData=")) {
								error = true;
								break;
								}
							String dataKey = SelfOrganizedMap.extractValue(theLine);
							if (mCompatibilityError == null && !keySet.contains(dataKey))
								mCompatibilityError = "Missing data key '"+dataKey+"' in current dataset.";
							mPivotDataMap.put(dataKey, Integer.valueOf(i));
							}

						theLine = reader.readLine();
						}
					}
				}

			for (int i=0; i<mColumnList.length && !error; i++) {
				if (i != 0)
					theLine = reader.readLine();

				if (theLine.startsWith("<columnName=")) {
					String columnName = SelfOrganizedMap.extractValue(theLine);
					if (columnName.startsWith("fingerprint_")) {
							// this was used prior V2.7.0 for the only existing chemical fingerprint
						if (!columnName.endsWith(SSSearcherWithIndex.cIndexVersion))
							mCompatibilityError = "SOM was generated with a different structure index version.";

						int descriptorColumn = -1;
						int[] descriptorColumnList = mTableModel.getSpecialColumnList(DescriptorConstants.DESCRIPTOR_FFP512.shortName);
						if (descriptorColumnList == null)
							mCompatibilityError = "The SOM was built based on chemical structures, but your current data doesn't contain structures.";
						else
							descriptorColumn = selectDescriptorColumn(descriptorColumnList);

						if (descriptorColumn != -1 && !mTableModel.isDescriptorAvailable(descriptorColumn))
							mCompatibilityError = "Please wait until the descriptor calculation is finished.";
	
						theLine = reader.readLine();
						error = !theLine.startsWith("<keyList=");
						if (!error) {
							String keyList = SelfOrganizedMap.extractValue(theLine);
							mColumnList[i] = descriptorColumn;
							mVaryingKeyInt = SSSearcherWithIndex.getIndexFromHexString(keyList);
							}
						}
					else {
						mColumnList[i] = mTableModel.findColumn(SelfOrganizedMap.extractValue(theLine));
						if (mColumnList[i] == -1)
							mCompatibilityError = "Non matching column title in SOM file: "+SelfOrganizedMap.extractValue(theLine);
						}
					}
				else if (theLine.startsWith("<descriptorName=")) {
					String descriptorShortName = SelfOrganizedMap.extractValue(theLine);
					DescriptorHandler dh = CompoundTableModel.getDefaultDescriptorHandler(descriptorShortName);

					error = (dh == null);
					if (!error) {
						theLine = reader.readLine();
						error = !theLine.startsWith("<descriptorVersion=");

						if (!SelfOrganizedMap.extractValue(theLine).equals(dh.getVersion()))
							mCompatibilityError = "SOM was generated with a different descriptor version.";
						}

					if (!error) {
						int[] descriptorColumnList = mTableModel.getSpecialColumnList(descriptorShortName);
						if (descriptorColumnList == null)
							mCompatibilityError = "The SOM was built based on a descriptor that is not available in your data: "+descriptorShortName;

						if (dh.getInfo().isBinary
						 || dh instanceof DescriptorHandlerSkeletonSpheres
						 || dh instanceof DescriptorHandlerReactionFP) {
							theLine = reader.readLine();
							error = !theLine.startsWith("<keyList=");
							if (!error) {
								String keyList = SelfOrganizedMap.extractValue(theLine);
								mColumnList[i] = selectDescriptorColumn(descriptorColumnList);
								if (dh instanceof AbstractDescriptorHandlerLongFP)
								   	mVaryingKeyLong = new DescriptorEncoder().decodeLong(keyList);
								else
									mVaryingKeyInt = new DescriptorEncoder().decode(keyList);
								}
							}
						}
					}
				else {
					error = true;
					}
				}
			}
		else if (mType == SOM_TYPE_BINARY) {
			String theLine = reader.readLine();
			error = !theLine.startsWith("<columnName=");
			if (!error) {
				String columnName = SelfOrganizedMap.extractValue(theLine);
				if (columnName.startsWith("fingerprint_")) {
					// this was used prior V2.7.0 for the only existing chemical fingerprint
					if (!columnName.endsWith(SSSearcherWithIndex.cIndexVersion))
						mCompatibilityError = "SOM was generated with a different structure index version.";

					int descriptorColumn = -1;
					int[] descriptorColumnList = mTableModel.getSpecialColumnList(DescriptorConstants.DESCRIPTOR_FFP512.shortName);
					if (descriptorColumnList == null)
						mCompatibilityError = "The SOM was built based on chemical structures, but your current data doesn't contain structures.";
					else
						descriptorColumn = selectDescriptorColumn(descriptorColumnList);

					if (descriptorColumn != -1 && !mTableModel.isDescriptorAvailable(descriptorColumn))
						mCompatibilityError = "Please wait until the descriptor calculation is finished.";

					mColumnList = new int[1];
					mColumnList[0] = descriptorColumn;
					}
				}
			}

		if (error)
			throw new IOException("Invalid SOM file format");
		}

	private int selectDescriptorColumn(int[] descriptorColumnList) {
		if (descriptorColumnList != null) {
			if (descriptorColumnList.length == 1)
				return descriptorColumnList[0];

			String[] columnNameList = new String[descriptorColumnList.length];
			for (int i=0; i<descriptorColumnList.length; i++)
				columnNameList[i] = mTableModel.getColumnTitle(descriptorColumnList[i]);

			String columnName = (String)JOptionPane.showInputDialog(null,
								"Please select one of these columns containing chemical structures!",
								"Select Structure Column",
								JOptionPane.QUESTION_MESSAGE,
								null,
								columnNameList,
								columnNameList[0]);
			return mTableModel.getChildColumn(mTableModel.findColumn(columnName), DescriptorConstants.DESCRIPTOR_FFP512.shortName);
			}
		return -1;
		}

	private boolean calculateParameterRow(int row) {
		if (mPivotValue != null) {
			if (mPivotValue[row] == null)
				return false;

			for (int i=0; i<mPivotValue[row].length; i++)
				mRowParameter[i] = mPivotValue[row][i];

			return true;
			}

		int paramIndex = 0;
		int firstListIndex = 0;
		if (mTableModel.isDescriptorColumn(mColumnList[0])) {
			Object descriptor = mTableModel.getTotalRecord(row).getData(mColumnList[0]);
			if (descriptor == null || mTableModel.getDescriptorHandler(mColumnList[0]).calculationFailed(descriptor))
				return false;

			if (mTableModel.getDescriptorHandler(mColumnList[0]) instanceof DescriptorHandlerReactionFP) {
				long[] currentIndex = (long[])descriptor;
				for (int i=0; i<mVaryingKeyLong.length; i++) {
					boolean isReactionCenter = (i < DescriptorHandlerReactionFP.REACTION_CENTER_LONG_COUNT);
					long theBit = 1;
					for (int j=0; j<64; j++) {
						if ((mVaryingKeyLong[i] & theBit) != 0) {
							mRowParameter[paramIndex++] = ((currentIndex[i] & theBit) == 0) ? 0.0
						: isReactionCenter ? DescriptorHandlerReactionFP.REACTION_CENTER_WEIGHT : DescriptorHandlerReactionFP.PERIPHERY_WEIGHT;
							}
						theBit <<= 1;
						}
					}
				}
			else if (descriptor instanceof long[]) {
				long[] currentIndex = (long[])descriptor;
				for (int i=0; i<mVaryingKeyLong.length; i++) {
					long theBit = 1;
					for (int j=0; j<64; j++) {
						if ((mVaryingKeyLong[i] & theBit) != 0) {
							mRowParameter[paramIndex++] = ((currentIndex[i] & theBit) != 0) ? 1.0 : 0.0;
							}
						theBit <<= 1;
						}
					}
				}
			else if (descriptor instanceof int[]) {
				int[] currentIndex = (int[])descriptor;
				for (int i=0; i<mVaryingKeyInt.length; i++) {
					int theBit = 1;
					for (int j=0; j<32; j++) {
						if ((mVaryingKeyInt[i] & theBit) != 0) {
							mRowParameter[paramIndex++] = ((currentIndex[i] & theBit) != 0) ? 1.0 : 0.0;
							}
						theBit <<= 1;
						}
					}
				}
			else {
				byte[] currentIndex = (byte[])descriptor;
				for (int i=0; i<mVaryingKeyInt.length; i++) {
					int theBit = 1;
					for (int j=0; j<32; j++) {
						if ((mVaryingKeyInt[i] & theBit) != 0) {
							mRowParameter[paramIndex++] = currentIndex[i*32+j];
							}
						theBit <<= 1;
						}
					}
				}
			firstListIndex = 1;
			}

		for (int i=firstListIndex; i<mColumnList.length; i++) {
			double d = mTableModel.getTotalDoubleAt(row, mColumnList[i]);
			if (Double.isNaN(d))
				return false;
			mRowParameter[paramIndex++] = d;
			}

		return true;
		}

	private void calculatePivotTable() {
		int pivotRows = mTableModel.getCategoryCount(mPivotGroupColumn);
		int pivotColumns = mParameterCount;
		mPivotValue = new double[pivotRows][pivotColumns];
		int[][] count = new int[pivotRows][pivotColumns];
		mSOM.startProgress("Calculating pivot table...", 0, 0);
		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			int groupCategory = (int)mTableModel.getTotalDoubleAt(row, mPivotGroupColumn);
			int dataCategory = (mPivotDataMap == null) ?
							   (int)mTableModel.getTotalDoubleAt(row, mPivotDataColumn)
							 : mPivotDataMap.get(mTableModel.getTotalValueAt(row, mPivotDataColumn)).intValue();
			for (int i=0; i<mColumnList.length; i++) {
				mPivotValue[groupCategory][dataCategory*mColumnList.length+i]
						+= mTableModel.getTotalDoubleAt(row, mColumnList[i]);
				count[groupCategory][dataCategory*mColumnList.length+i]++;
				}
			}
		for (int i=0; i<pivotRows; i++) {
			for (int j=0; j<pivotColumns; j++) {
				if (count[i][j] == 0) {
					mPivotValue[i] = null;
					break;
					}
				else {
					mPivotValue[i][j] /= count[i][j];
					}
				}
			}
		}

	/////////////////////////////////////////////////////
	////////////// SOMController methods ////////////////
	/////////////////////////////////////////////////////
	@Override
	public int getInputVectorCount() {
		if (mInputVectorCount == 0) {
			mUsedRowIndex = null;

			if (mPivotValue != null) {  // if we use a pivot table
				for (int row=0; row<mPivotValue.length; row++) {
					if (mPivotValue[row] == null) {
						if (mUsedRowIndex == null) {
							mUsedRowIndex = new int[mPivotValue.length];
							for (int i=0; i<row; i++)
								mUsedRowIndex[i] = i;
							mInputVectorCount = row;
							}
						}
					else {
						if (mUsedRowIndex != null)
							mUsedRowIndex[mInputVectorCount] = row;
						mInputVectorCount++;
						}
					}
				}
			else {  // we take the rows from mTableModel
				for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
					Object o = getRowInputVector(row);
					if (o == null) {
						if (mUsedRowIndex == null) {
							mUsedRowIndex = new int[mTableModel.getTotalRowCount()];
							for (int i=0; i<row; i++)
								mUsedRowIndex[i] = i;
							mInputVectorCount = row;
							}
						}
					else {
						if (mUsedRowIndex != null)
							mUsedRowIndex[mInputVectorCount] = row;
						mInputVectorCount++;
						}
					}
				}
			}
		return mInputVectorCount;
		}

	@Override
	public Object getInputVector(int row) {
		return (mUsedRowIndex == null) ? getRowInputVector(row) : getRowInputVector(mUsedRowIndex[row]);
		}

	private Object getRowInputVector(int row) {
		if (mType == SOM_TYPE_BINARY)
			return mTableModel.getTotalRecord(row).getData(mColumnList[0]);

		if (!calculateParameterRow(row))
			return null;

		double[] v = new double[mParameterCount];
		for (int i=0; i<mParameterCount; i++)
			v[i] = mRowParameter[i];
		return v;
		}
	}
