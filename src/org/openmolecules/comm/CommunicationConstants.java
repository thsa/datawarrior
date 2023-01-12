/*
 * @(#)CommunicationConstants.java
 *
 * Copyright 2013 openmolecules.org, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains the property
 * of openmolecules.org.  The intellectual and technical concepts contained
 * herein are proprietary to openmolecules.org.
 * Actelion Pharmaceuticals Ltd. is granted a non-exclusive, non-transferable
 * and timely unlimited usage license.
 *
 * @author Thomas Sander
 */

package org.openmolecules.comm;

public interface CommunicationConstants {
    String ERROR_INVALID_SESSION = "Invalid session";

    String BODY_MESSAGE = "Message";
    String BODY_OBJECT = "Object";
    String BODY_ERROR = "Error";
    String BODY_ERROR_INVALID_SESSION = BODY_ERROR + ":" + ERROR_INVALID_SESSION;
    String BODY_IMAGE_PNG = "PNG";

    String KEY_SESSION_ID = "sessionID";
    String KEY_REQUEST = "what";
    String KEY_QUERY = "query";
    String KEY_APP_NAME = "appname";
    String KEY_USER = "user";
    String KEY_PASSWORD = "password";

    String REQUEST_NEW_SESSION = "new";
    String REQUEST_END_SESSION = "end";
    String REQUEST_GET_STATUS = "status";
    String REQUEST_RUN_QUERY = "query";
    String REQUEST_LOGIN = "login";
	}
