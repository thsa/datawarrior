package com.actelion.research.gui.form;

import javax.swing.*;

public class JFXPDBFormObject extends ReferenceFormObject {
	public JFXPDBFormObject(String key, String type) {
		super(key, type);
		mComponent = new JPanel();
		mComponent.add(new JFXPDBDetailView(null, null, null));
		mComponent = new JFXPDBDetailView(null, null, null);

		// if we don't encapsulate in a JPanel, then form maximization leads to loss of the component fopr some reason
		//		mComponent = new JFXPDBDetailView(null, null, null);
	}

	public int getDefaultRelativeHeight() {
		return 3;
	}
}
