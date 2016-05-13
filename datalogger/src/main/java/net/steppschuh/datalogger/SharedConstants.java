package net.steppschuh.datalogger;

public class SharedConstants {

    public static final int CONNECTION_TIMEOUT = 5000;

    public static final String SHARED_PREFERENCES = "shared_preferences";

    public static final String KEY_MESSENGER = "messenger";
    public static final String KEY_PATH = "path";

    public static final String KEY_DATA = "data";
    public static final String KEY_FROM_NODE = "node";
    public static final String KEY_RECORD_LABEL = "record_label";
    public static final String KEY_RECORD_COMMENT = "record_comment";
    public static final String KEY_RECORD_NAME = "record_name";
    public static final String KEY_RECORD_DURATION = "record_duration";

    public static final String MESSAGE_PATH_SET_MESSENGER = "/set_messenger";
    public static final String MESSAGE_PATH_START_ACTIVITY = "/start_activity";
    public static final String MESSAGE_PATH_TOGGLE_RECORDING = "/toggle_recording";
    public static final String MESSAGE_PATH_TOGGLE_DEMO_MODE = "/toggle_demo_mode";
    public static final String MESSAGE_PATH_UPLOAD_DATA = "/upload_data";
    public static final String MESSAGE_PATH_SET_LATEST_TRUST_LEVELS = "/set_latest_trust_levels";
    public static final String MESSAGE_PATH_GET_LATEST_TRUST_LEVELS = "/get_latest_trust_levels";
    public static final String MESSAGE_PATH_START_GET_LATEST_TRUST_LEVELS = "/start_get_latest_trust_levels";
    public static final String MESSAGE_PATH_STOP_GET_LATEST_TRUST_LEVELS = "/stop_get_latest_trust_levels";
    public static final String MESSAGE_PATH_SET_LATEST_STATES = "/set_latest_states";
    public static final String MESSAGE_PATH_GET_LATEST_STATES = "/get_latest_states";
    public static final String MESSAGE_PATH_CLEAR_STATES = "/clear_states";
    public static final String MESSAGE_PATH_GET_STATUS = "/get_status";
    public static final String MESSAGE_PATH_SET_STATUS = "/set_status";
    public static final String MESSAGE_PATH_REGISTER_PHONE = "/register_phone";
    public static final String MESSAGE_PATH_REGISTER_WATCH = "/register_watch";
    public static final String MESSAGE_PATH_SET_RECORDED_BATCHES = "/set_recorded_batches";
}
