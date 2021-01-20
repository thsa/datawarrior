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
    public static final String ERROR_INVALID_SESSION = "Invalid session";

    public static final String BODY_MESSAGE = "Message";
    public static final String BODY_OBJECT = "Object";
    public static final String BODY_ERROR = "Error";
    public static final String BODY_ERROR_INVALID_SESSION = BODY_ERROR + ":" + ERROR_INVALID_SESSION;
    public static final String BODY_IMAGE_PNG = "PNG";

    public static final String KEY_SESSION_ID = "sessionID";
    public static final String KEY_REQUEST = "what";
    public static final String KEY_QUERY = "query";
    public static final String KEY_APP_NAME = "appname";

    public static final String REQUEST_NEW_SESSION = "new";
    public static final String REQUEST_END_SESSION = "end";
    public static final String REQUEST_GET_STATUS = "status";
    public static final String REQUEST_RUN_QUERY = "query";
	}
