package com.jym.socketserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.ArrayList;

/**
 * Created by JYM on 2016/7/16.
 */
public class NetworkStateReceiver extends BroadcastReceiver{

    public static ArrayList<Thread> threadsList = new ArrayList<Thread>();

    @Override
    public void onReceive(Context context, Intent intent) {

        if (MainActivity.networkInfo.isAvailable()) {
            for (Thread thread : threadsList) {
                thread.notify();
            }
            threadsList.clear();
        }
    }
}
