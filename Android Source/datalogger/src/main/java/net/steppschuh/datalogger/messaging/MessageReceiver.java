package net.steppschuh.datalogger.messaging;

import android.os.Message;

public interface MessageReceiver {

    void onMessageReceived(Message message);

}
