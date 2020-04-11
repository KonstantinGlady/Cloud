package org.gik.cloud.storage.common;

public class MessageCode {
    public static final byte AUTH_CODE               = 22;
    public static final byte AUTH_CODE_ACCEPTED      = 33;
    public static final byte AUTH_CODE_FAIL          = 44;
    public static final byte GET_DIR                 = 55;
    public static final byte COPY_FILE_FROM_SERVER   = 66;
    public static final byte DELETE_FILE_ON_SERVER   = 77;
    public static final byte MOVE_FILE_TO_SERVER     = 81;
    public static final byte DELETE_FILE_ON_CLIENT   = 82;
    public static final byte MOVE_FILE_FROM_SERVER   = 83;
    public static final byte COPY_FILE_TO_SERVER     = 88;
    public static final byte REFRESH_UI              = 99;
    public static final byte CLEAR_SERVER_LIST       = 90 ;
}
