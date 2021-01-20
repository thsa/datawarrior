package com.actelion.research.gui.form;

public class RemoteDetailSource {
	private String mSource;

	/**
	 * Default implementation of a source definition for accessing external detail objects
	 * @param source
	 */
	public RemoteDetailSource(String source) {
		mSource = source;
		}

	public String getSource() {
		return mSource;
		}
	}
