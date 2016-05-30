package net.steppschuh.datalogger.messaging.handler;

import android.os.Message;

public abstract class SinglePathMessageHandler extends MessageHandler {

    public SinglePathMessageHandler(String path) {
        super();
        super.getPaths().add(path);
    }

    @Override
    public void handleMessage(Message message) {
        super.handleMessage(message);
    }
}
