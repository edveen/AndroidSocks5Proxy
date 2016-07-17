package com.jym.socketserver;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private String TAG = "SocketServer";
    public static ConnectivityManager connectivityManager;
    public static NetworkInfo networkInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkInfo = connectivityManager.getActiveNetworkInfo();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket(34500); //这里随机选择了一个端口
                    Log.d(TAG, "Port=" + serverSocket.getLocalPort());
                    while (true) {
                        Socket socket = serverSocket.accept();//若获取不到会一直阻塞
                        new Thread(new ServerThread(socket)).start();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();

    }
}
