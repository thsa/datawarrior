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

package com.actelion.research.table.view.data;

import com.actelion.research.util.Base64;

//import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class DataConnection extends URLConnection {

	public DataConnection(URL u) {
		super(u);
	    }

	@Override
	public void connect() throws IOException {
		connected = true;
	    }

	@Override
	public InputStream getInputStream() throws IOException {
		String data = url.toString().replaceFirst("^.*;base64,", "");
		byte[] bytes = Base64.decode(data);
//		byte[] bytes = DatatypeConverter.parseBase64Binary(data);
		return new ByteArrayInputStream(bytes);
		}
	}
