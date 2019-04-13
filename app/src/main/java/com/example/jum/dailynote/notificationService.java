package com.example.jum.dailynote;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class notificationService extends Service {
    public notificationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
