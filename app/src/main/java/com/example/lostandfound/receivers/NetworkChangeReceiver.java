package com.example.lostandfound.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import com.example.lostandfound.utils.NetworkUtils;

/**
 * BroadcastReceiver that listens for network connectivity changes.
 * When connectivity is restored, notifies the registered listener so the app
 * can trigger a Firebase sync.
 */
public class NetworkChangeReceiver extends BroadcastReceiver {

    public interface NetworkChangeListener {
        void onNetworkChanged(boolean isConnected);
    }

    private final NetworkChangeListener listener;

    public NetworkChangeReceiver(NetworkChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            boolean isConnected = NetworkUtils.isNetworkAvailable(context);
            if (listener != null) {
                listener.onNetworkChanged(isConnected);
            }
        }
    }
}
