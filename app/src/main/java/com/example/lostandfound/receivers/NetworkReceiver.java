package com.example.lostandfound.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.example.lostandfound.MainActivity;
import com.example.lostandfound.utils.NetworkUtils;

public class NetworkReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            if (NetworkUtils.isNetworkAvailable(context)) {
                // Broadcast local intent so MainActivity can sync
                Intent syncIntent = new Intent("com.example.lostandfound.NETWORK_RESTORED");
                context.sendBroadcast(syncIntent);
            }
        }
    }
}
