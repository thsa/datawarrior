package com.actelion.research.table.view;

import com.actelion.research.util.DoubleFormat;

import java.io.BufferedWriter;
import java.io.IOException;

public class LabelPosition3D extends LabelPosition2D {
	private float mZ;
	private int mScreenZ;

	public LabelPosition3D(int column, LabelPosition2D nextInChain) {
		super(column, nextInChain);
	}

	/**
	 * @return screen z-coord of label center without retina and AA factors
	 */
	public int getScreenZ() {
		return mScreenZ;
	}

	public void setScreenZ(int screenZ) {
		mScreenZ = screenZ;
	}

	public float getZ() {
		return mZ;
	}

	public void setZ(float z) {
		mZ = z;
	}

	@Override
	public void writePosition(BufferedWriter writer, String rowID, String columnTitle) throws IOException {
		super.writePosition(writer, rowID, columnTitle);
		writer.write('\t');
		writer.write(DoubleFormat.toString(mZ));
	}
}
