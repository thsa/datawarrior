package com.actelion.research.datawarrior.task.db;

import java.awt.*;
import java.sql.Connection;

public interface SQLConnector {
	public Connection connect(Frame parent);
}
