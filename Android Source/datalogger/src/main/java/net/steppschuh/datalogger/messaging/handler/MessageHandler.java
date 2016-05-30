package net.steppschuh.datalogger.messaging.handler;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import net.steppschuh.datalogger.messaging.GoogleApiMessenger;
import net.steppschuh.datalogger.messaging.MessageReceiver;

import java.util.ArrayList;
import java.util.List;

public class MessageHandler extends Handler {

    public static final String TAG = MessageHandler.class.getSimpleName();

    public static final String KEY_PATH = "path";
    public static final String KEY_DATA = "data";
    public static final String KEY_SOURCE_NODE_ID = "source_node_id";

    public static final String PATH_ANY = "/*";
    public static final String PATH_PING = "/ping";
    public static final String PATH_ECHO = "/echo";
    public static final String PATH_CLOSING = "/closing";
    public static final String PATH_GET_STATUS = "/get_status";
    public static final String PATH_SET_STATUS = "/set_status";
    public static final String PATH_GET_SENSORS = "/get_sensors";
    public static final String PATH_SET_SENSORS = "/set_sensors";
    public static final String PATH_SENSOR_DATA_REQUEST = "/sensor_data_request";
    public static final String PATH_SENSOR_DATA_REQUEST_RESPONSE = "/sensor_data_request_response";

    private MessageReceiver messageReceiver;
    private List<String> paths;

    public MessageHandler() {
        paths = new ArrayList<>();
    }

    public MessageHandler(MessageReceiver messageReceiver) {
        this();
        paths.add(PATH_ANY);
        this.messageReceiver = messageReceiver;
    }

    public MessageHandler(String path) {
        this();
        paths.add(path);
    }

    public MessageHandler(String path, MessageReceiver messageReceiver) {
        this(path);
        this.messageReceiver = messageReceiver;
    }

    public MessageHandler(List<String> paths, MessageReceiver messageReceiver) {
        this();
        this.paths = paths;
        this.messageReceiver = messageReceiver;
    }

    public boolean shouldHandleMessage(String path) {
        return paths.contains(path) || paths.contains(PATH_ANY);
    }

    public boolean shouldHandleMessage(Message message) {
        return shouldHandleMessage(getPathFromMessage(message));
    }

    public static String getPathFromMessage(Message message) {
        if (message == null) {
            return null;
        }
        Bundle data = message.getData();
        if (data == null) {
            return null;
        }
        return data.getString(MessageHandler.KEY_PATH);
    }

    public static String getSourceNodeIdFromMessage(Message message) {
        if (message == null) {
            return null;
        }
        Bundle data = message.getData();
        if (data == null) {
            return null;
        }
        return data.getString(MessageHandler.KEY_SOURCE_NODE_ID);
    }

    public static byte[] getDataFromMessage(Message message) {
        if (message == null) {
            return null;
        }
        Bundle data = message.getData();
        if (data == null) {
            return null;
        }
        return data.getByteArray(MessageHandler.KEY_DATA);
    }

    public static String getDataFromMessageAsString(Message message) {
        byte[] data = getDataFromMessage(message);
        if (data != null) {
            return new String(data, GoogleApiMessenger.DEFAULT_CHARSET);
        }
        return null;
    }

    @Override
    public void handleMessage(Message message) {
        if (messageReceiver != null) {
            messageReceiver.onMessageReceived(message);
        }
    }

    /**
     * Getter & Setter
     */
    public MessageReceiver getMessageReceiver() {
        return messageReceiver;
    }

    public void setMessageReceiver(MessageReceiver messageReceiver) {
        this.messageReceiver = messageReceiver;
    }

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }
}