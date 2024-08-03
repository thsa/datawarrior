package com.actelion.research.datawarrior.task.macro;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.AbstractTaskWithoutConfiguration;

import java.awt.*;
import java.util.Properties;

public class DETaskEndIf extends AbstractTaskWithoutConfiguration {
	public static final String TASK_NAME = "End If";

	public DETaskEndIf(Frame parent) {
		super(parent, false);
	}

	@Override
	public boolean isConfigurable() {
		return true;
	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public void runTask(Properties configuration) {
		// the if/then is actually performed by the DEMacroRecorder
	}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
	}
}
