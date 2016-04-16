package me.gitai.library.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import me.gitai.library.util.L;

/**
 * Created by dphdjy on 15-10-22.
 */
public class BaseService extends Service {
    @Override
    public void onCreate() {
        super.onCreate();
        L.d();
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        L.d();
    }

    @Override
    public IBinder onBind(Intent intent) {
        L.d();
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        L.d();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        L.d();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        L.d();
    }
}
