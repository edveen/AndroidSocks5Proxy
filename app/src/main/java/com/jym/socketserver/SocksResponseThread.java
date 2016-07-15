package com.jym.socketserver;

import android.util.Log;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by JYM on 2016/7/14.
 */
public class SocksResponseThread extends Thread {

    private InputStream in;
    private OutputStream out;
    private int BUFF_SIZE = 1024 * 10;

    public SocksResponseThread(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
    }

    @Override
    public void run() {
        int readbytes = 0;
        byte buf[] = new byte[BUFF_SIZE];
        while (true) {
            try {
                if (readbytes == -1) break;
                readbytes = in.read(buf, 0, BUFF_SIZE);
                if (readbytes > 0) {
                    out.write(buf, 0, readbytes);
                }
                out.flush();
            } catch (Exception e) {
                break;
            }
        }
    }
}