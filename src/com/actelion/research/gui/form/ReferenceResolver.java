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

package com.actelion.research.gui.form;

public interface ReferenceResolver {

	public static final int MODE_DEFAULT = 0;

	public static final int MODE_THUMBNAIL = 0;
	public static final int MODE_FULL_IMAGE = 1;

	/**
	 * Asynchronous way to request image (or other) data.
	 * Once the data is available it will be send to the
	 * consumer by calling setReferencedData().
	 * @param source data source name, which may carry a generic SQL or URL
	 * @param reference a key to retrieve the particular detail data
	 * @param mode source specific mode/flags to more closely describe what data to deliver
	 * @param consumer the receiver of the data
	 */
	public void requestData(RemoteDetailSource source, String reference, int mode, ReferencedDataConsumer consumer);

	/**
	 * Synchronous way to retrieve image (or other) data.
	 * @param source data source name, which may carry a generic SQL or URL
	 * @param reference a key to retrieve the particular detail data
	 * @param mode source specific mode/flags to more closely describe what data to deliver
	 * @return the requested data or null, if not available or timed out
	 */
	public byte[] resolveReference(RemoteDetailSource source, String reference, int mode);
	}
