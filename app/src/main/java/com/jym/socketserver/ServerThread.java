package com.jym.socketserver;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;

/**
 * Created by JYM on 2016/7/12.
 */
public class ServerThread implements Runnable {

    private Socket socket;
    private String TAG = this.getClass().getName();
    private int BUFF_SIZE = 1024 * 100;

    public ServerThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            InputStream innerInputStream = socket.getInputStream();
            OutputStream innerOutputStream = socket.getOutputStream();
            byte[] buff = new byte[BUFF_SIZE];
            int rc;
            ByteArrayOutputStream byteArrayOutputStream;

            /**
             * client会向proxy发送510，所以这里执行的结果是buff={5,1,0}
             * Caution: 这里不能跟下面的innerInputStream.read(buff, 0, 10);合并成innerInputStream.read(buff, 0, 13);
             *          我试过，大部分情况没影响，但是偶尔会出现重大bug（读不出外网ip），至于原因暂不详
             *          看来这种input和output类型的操作还是稳重一点，不要太心急
             */
            innerInputStream.read(buff, 0, 3);

            /**
             *  proxy向client发送应答{5,0}
             */
            byte[] firstAckMessage = new byte[]{5, 0};
            byte[] secondAckMessage = new byte[10];
            innerOutputStream.write(firstAckMessage);
            innerOutputStream.flush();

            /**
             *     client发送命令5101+目的地址（4Bytes）+目的端口（2Bytes)
             *     即{5,1,0,1,IPx1,IPx2,IPx3,IPx4,PORTx1,PORTx2} 一共10位
             *     例如发送给52.88.216.252服务器的80端口，那么这里buff就是{5,1,0,1,52,88,-40,-4,0,80}（这里每位都是byte，所以在-128~127之间，可以自己换算成0~255）
             */
            innerInputStream.read(buff, 0, 10);

            String IP = byte2int(buff[4]) + "." + byte2int(buff[5]) + "." + byte2int(buff[6]) + "." + byte2int(buff[7]);
            int port = byte2int(buff[8]) * 256 + byte2int(buff[9]);

            Log.e("ServerThread", "Connected to " + IP + ":" + port);
            Socket outerSocket = new Socket(IP, port);
            InputStream outerInputStream = outerSocket.getInputStream();
            OutputStream outerOutputStream = outerSocket.getOutputStream();

            /**
             * proxy 向 client 返回应答5+0+0+1+因特网套接字绑定的IP地址（4字节的16进制表示）+因特网套接字绑定的端口号（2字节的16进制表示）
             */
            byte ip1[] = new byte[4];
            int port1 = 0;
            ip1 = outerSocket.getLocalAddress().getAddress();
            port1 = outerSocket.getLocalPort();

            secondAckMessage[0] = 5;
            secondAckMessage[1] = 0;
            secondAckMessage[2] = 0;
            secondAckMessage[3] = 1;
            secondAckMessage[4] = ip1[0];
            secondAckMessage[5] = ip1[1];
            secondAckMessage[6] = ip1[2];
            secondAckMessage[7] = ip1[3];
            secondAckMessage[8] = (byte) (port1 >> 8);
            secondAckMessage[9] = (byte) (port1 & 0xff);
            innerOutputStream.write(secondAckMessage, 0, 10);
            innerOutputStream.flush();

            /**
             * 新线程：从外网不断读数据发到client
             */
            SocksResponseThread responseThread = new SocksResponseThread(outerInputStream, innerOutputStream);
            responseThread.start();

            /**
             * 本线程：从client不断读数据发到外网
             */
            byteArrayOutputStream = new ByteArrayOutputStream();
            while ((rc = innerInputStream.read(buff, 0, BUFF_SIZE)) > 0) {
                outerOutputStream.write(buff, 0, rc);
                byteArrayOutputStream.write(buff, 0, rc);
                outerOutputStream.flush();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public int byte2int(byte b) {
        return b & 0xff;
    }

}