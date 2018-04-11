package model.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Scanner;

/**
 * A singleton class for using the socket
 */
public class SocketMgr {

    private static SocketMgr mInstance;
    
    private Socket mSocket;
    private PrintWriter mSocketWriter;
    //private BufferedReader mSocketReader;
    private BufferedInputStream mSocketReader;
    private static final int PORT = 5182;
    //private static final String ADDRESS = "192.168.13.1";
    private static final String ADDRESS = "127.0.0.1";

//    private SocketMgr() { }

    public static SocketMgr getInstance() {
        if (mInstance == null)
            mInstance = new SocketMgr();
        return mInstance;
    }

    public void openConnection() {
        try {
            mSocket = new Socket(ADDRESS, PORT);
            //mSocket.setTcpNoDelay(true);
            mSocketWriter = new PrintWriter(mSocket.getOutputStream(), true);
            InputStream is = null;
            try {
                is = mSocket.getInputStream();
                mSocketReader = new BufferedInputStream(is);
            } catch(IOException e) {
                try {
                    mSocket.close();
                } catch(IOException e2) {
                    System.err.println("Socket not closed :"+e2);
                }
                return;
            }
            //mSocketReader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
            System.out.println("Socket connection successful");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Socket connection failed");

        }
    }

    public void closeConnection() {
        mSocketWriter.close();
        try {
            mSocketReader.close();
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Socket connection closed");
    }

    public boolean isConnected() {
        return mSocket != null && mSocket.isConnected();
    }

    public void sendMessage(String dest, String msg) {
        mSocketWriter.println(dest + msg);
       // System.out.println("Sent message: " + dest + msg);
    }

    public String receiveMessage(boolean sensor) {
        /*try {
            if (sensor)
                mSocket.setSoTimeout(3000);
            else
                mSocket.setSoTimeout(0);
        } catch (SocketException e) {

        }*/
        String data = "";
        try {
            int s = mSocketReader.read();
            if(s==-1)
                return null;
            data += ""+(char)s;
            int len = mSocketReader.available();
            //System.out.println("Len got : "+len);
            if(len > 0) {
                byte[] byteData = new byte[len];
                mSocketReader.read(byteData);
                data += new String(byteData);
            }
        	//System.out.println("Received message: " + data);
            return data.trim();
        }
        catch (SocketTimeoutException e) {
            System.out.println("Sensor reading timeout!!!");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;    	
    }

    /*public void clearInputBuffer() {
        String input;
        try {
            while ((input = mSocketReader.) != null) {
                System.out.println("Discarded message: " + input);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/
}
