package net.steppschuh.datalogger.message;

import android.os.Handler;
import android.os.Message;

public class MessageHandler extends Handler {

    private MessageReceiver messageReceiver;

    public MessageHandler(MessageReceiver messageReceiver) {
        this.messageReceiver = messageReceiver;
    }

    @Override
    public void handleMessage(Message message) {
        messageReceiver.onMessageReceived(message);
    }

}