package org.openmolecules.comm;

import java.io.IOException;

public class ServerErrorException extends IOException {
	public ServerErrorException(String message) {
		super(message);
	}
}
