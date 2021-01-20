package com.actelion.research.table.view.config;

import com.actelion.research.util.BinaryDecoder;
import com.actelion.research.util.BinaryEncoder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Set;
import java.util.TreeMap;

public abstract class AbstractConfiguration extends TreeMap<String,Object> {
	private static final String cBinaryObject = "isBinaryEncoded";
	private static final String cNameTag = " name=\"";
	private static final String cTypeTag = " type=\"";

	private String mConfigTag;

	public AbstractConfiguration(String configTagName) {
		mConfigTag = configTagName;
		}

	public String getProperty(String key) {
		return (String) get(key);
	}

	public void setProperty(String key, String value) {
		put(key, value);
	}

	public byte[] getBinary(String key) {
		return (byte[]) get(key);
	}

	public void setBinary(String key, byte[] value) {
		put(key, value);
	}

	public static boolean isStartTag(String line, String tagName) {
		return line.equals("<".concat(tagName).concat(">"))
			|| line.startsWith("<".concat(tagName).concat(cNameTag))
			|| line.startsWith("<".concat(tagName).concat(cTypeTag));
		}

	public static String extractName(String line) {
		return extract(line, cNameTag);
		}

	public static String extractType(String line) {
		return extract(line, cTypeTag);
		}

	private static String extract(String line, String tag) {
		int index1 = line.indexOf(tag);
		if (index1 == -1)
			return null;

		index1 += tag.length();
		int index2 = index1;
		while (index2 < line.length() && (line.charAt(index2) != '\"'
			|| (index2+1 < line.length() && line.charAt(index2+1) == '\"'))) {
			if (line.charAt(index2) == '\"')
				index2++;
			index2++;
			}

		return line.substring(index1, index2).replace("\"\"", "\"");
		}

	private boolean isEndTag(String s) {
		return s.startsWith("</".concat(mConfigTag).concat(">"));
	}

	public void read(BufferedReader theReader) throws IOException {
		clear();
		while (true) {
			String theLine = theReader.readLine();
			if (theLine == null || isEndTag(theLine))
				break;

			int index1 = theLine.indexOf('<');
			if (index1 == -1)
				continue;
			int index2 = theLine.indexOf('=', index1 + 1);
			while (index2 != -1 && theLine.charAt(index2 + 1) == '=')
				index2 = theLine.indexOf('=', index2 + 2);
			if (index2 == -1)
				continue;
			int index3 = theLine.indexOf('"', index2 + 1);
			if (index3 == -1)
				continue;
			int index4 = theLine.indexOf('"', index3 + 1);
			while (index4 != -1 && theLine.charAt(index4 + 1) == '"')
				index4 = theLine.indexOf('"', index4 + 2);
			if (index4 == -1)
				continue;

			String key = theLine.substring(index1 + 1, index2).replace("==", "=");
			String value = theLine.substring(index3 + 1, index4).replace("\"\"", "\"");

			if (value.equals(cBinaryObject)) {
				BinaryDecoder decoder = new BinaryDecoder(theReader);
				int size = decoder.initialize(8);
				byte[] detailData = new byte[size];
				for (int i = 0; i < size; i++)
					detailData[i] = (byte) decoder.read();
				theLine = theReader.readLine();
				while (theLine != null
						&& !theLine.equals("</" + key.replace("=", "==") + ">"))
					theLine = theReader.readLine();
				if (theLine != null)
					put(key, detailData);
			} else {
				put(key, value);
			}
		}
	}

	/**
	 * @param writer
	 * @param name may be null, if there is only one of its type
	 * @param type may be null, if there is only one of its class
	 * @throws IOException
	 */
	public void write(BufferedWriter writer, String name, String type) throws IOException {
		writer.write("<");
		writer.write(mConfigTag);
		if (name != null) {
			writer.write(cNameTag);
			writer.write(name.replace("\"", "\"\""));
			writer.write("\"");
			}
		if (type != null) {
			writer.write(cTypeTag);
			writer.write(type.replace("\"", "\"\""));
			writer.write("\"");
			}
		writer.write(">");
		writer.newLine();

		Set<String> keys = keySet();
		for (String key : keys) {
			Object value = get(key);
			if (value instanceof String) {
				writer.write("<" + key.replace("=", "==") + "=\"" + ((String) getProperty(key)).replace("\"", "\"\"") + "\">");
				writer.newLine();
				}
			else if (value instanceof byte[]) {
				writer.write("<" + key.replace("=", "==") + "=\"" + cBinaryObject + "\">");
				writer.newLine();

				byte[] data = (byte[]) value;
				BinaryEncoder encoder = new BinaryEncoder(writer);
				encoder.initialize(8, data.length);
				for (int i = 0; i < data.length; i++)
					encoder.write(data[i]);
				encoder.finalize();

				writer.write("</" + key.replace("=", "==") + ">");
				writer.newLine();
				}
			}
		writer.write("</");
		writer.write(mConfigTag);
		writer.write(">");
		writer.newLine();
		}
	}