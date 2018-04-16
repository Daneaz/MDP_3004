package com.example.hanwe.mdp.Bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.example.hanwe.mdp.Configuration.Config;
import com.example.hanwe.mdp.Configuration.Protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothService extends Service{
    private static BluetoothService bcsInstance;

    private static final String ACTION = "BTLocalBroadcast";
    private static final String TAG = "BluetoothCommService";
    private static final String NAME = "BluetoothChat";

    // Unique UUID for this application
    private static final UUID MY_UUID = UUID.fromString(Protocol.UUID);
    //private static final UUID MY_UUID = UUID.fromString("53062b9d-a717-4c16-9a08-9b05f09aab8a");

    private BluetoothAdapter mAdapter;
    private BluetoothDevice mLastConnectedDevice;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    private int mState;                             //Current Connection State
    public static final int STATE_NONE = 0;         // Indicating Bluetooth service is on
    public static final int STATE_LISTEN = 1;       // Listening for incoming connections
    public static final int STATE_CONNECTING = 2;   // Initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;    // Connected to a remote device

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public BluetoothService() {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
    }

    public static BluetoothService getInstance() {
        if (bcsInstance == null)
            bcsInstance = new BluetoothService();

        return bcsInstance;
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private void setConnectionState(int state) {
        synchronized (this) {
            Log.d(Config.log_id, "setConnectionState() " + mState + " -> " + state);
            mState = state;
            Bundle bundle = new Bundle();
            bundle.putInt(Protocol.MESSAGE_TYPE, Protocol.MESSAGE_STATE_CHANGE);
            bundle.putInt(Protocol.MESSAGE_ARG1, state);
            Intent intent = new Intent(ACTION);
            intent.putExtras(bundle);
            sendMessage(intent);
        }
    }

    /**
     * Return the current connection state.
     */
    public int getConnectionState() {
        synchronized (this) {
            return mState;
        }
    }

    /**
     * Start the Bluetooth service. Specifically initialiseService AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public void initialiseService() {
        synchronized (this) {
            Log.d(Config.log_id, "Starting Bluetooth Service");

            // Cancel any thread attempting to make a connection
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }

            // Cancel any thread currently running a connection
            if (mConnectedThread != null) {
                mConnectedThread.cancel();
                mConnectedThread = null;
            }

            // Start the thread to listen on a BluetoothServerSocket
            if (mAcceptThread == null) {
                mAcceptThread = new AcceptThread();
                mAcceptThread.start();
            }
        }
    }

    /**
     * Stop the Bluetooth service. Specifically stop all threads.
     */
    public void stop() {
        synchronized (this) {
            Log.d(Config.log_id,"Stopping Bluetooth Service");

            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }

            if (mConnectedThread != null) {
                mConnectedThread.cancel();
                mConnectedThread = null;
            }

            if (mAcceptThread != null) {
                mAcceptThread.cancel();
                mAcceptThread = null;
            }

            setConnectionState(STATE_NONE);
        }
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public void connect(BluetoothDevice device) {
        synchronized (this) {
            Log.d(TAG, "Connecting to: " + device.getName());

            if (getConnectionState() == STATE_CONNECTING) {
                // Cancel any thread attempting to make a connection
                if (mConnectThread != null) {
                    mConnectThread.cancel();
                    mConnectThread = null;
                }
            } else if (getConnectionState() == STATE_CONNECTED) {
                // Cancel any thread currently running a connection
                if (mConnectedThread != null) {
                    mConnectedThread.cancel();
                    mConnectedThread = null;
                }
            }

            // Start the thread to connect with the given device
            mConnectThread = new ConnectThread(device);
            Bundle bundle = new Bundle();
            bundle.putInt(Protocol.MESSAGE_TYPE, Protocol.MESSAGE_DEVICE_NAME);
            bundle.putString(Protocol.DEVICE_NAME, device.getName());
            bundle.putString(Protocol.DEVICE_MAC, device.getAddress());
            Intent intent = new Intent(ACTION);
            intent.putExtras(bundle);
            Log.i(Config.log_id,"connecting, message");
            sendMessage(intent);
            mConnectThread.start();
            setConnectionState(STATE_CONNECTING);
        }
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        synchronized (this) {
            Log.d(Config.log_id, "Connected to " + device);

            // Cancel the thread that completed the connection
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }

            // Cancel any thread currently running a connection
            if (mConnectedThread != null) {
                mConnectedThread.cancel();
                mConnectedThread = null;
            }

            // Cancel the accept thread because we only want to connect to one device
            if (mAcceptThread != null) {
                mAcceptThread.cancel();
                mAcceptThread = null;
            }

            // Start the thread to manage the connection and perform transmissions
            mConnectedThread = new ConnectedThread(socket);
            mConnectedThread.start();
            // Send the name of the connected device back to the UI Activity

            Bundle bundle = new Bundle();
            bundle.putInt(Protocol.MESSAGE_TYPE, Protocol.MESSAGE_DEVICE_NAME);
            bundle.putString(Protocol.DEVICE_NAME, device.getName());
            bundle.putString(Protocol.DEVICE_MAC, device.getAddress());
            Intent intent = new Intent(ACTION);
            intent.putExtras(bundle);
            Log.i(Config.log_id,"connected, message");
            sendMessage(intent);
            setConnectionState(STATE_CONNECTED);
        }
    }

    public void disconnect() {
        synchronized (this) {
            Log.d(Config.log_id, "Disconnecting");
            initialiseService();
        }
    }
    /**
     * Send data to the remote device
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        synchronized (this) {
            if (mConnectedThread != null && getConnectionState() == STATE_CONNECTED)
                mConnectedThread.write(out);
        }
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void handleFailedConnection() {
        Intent intent = new Intent(ACTION);
        intent.putExtra(Protocol.MESSAGE_TYPE, Protocol.MESSAGE_TOAST);
        intent.putExtra(Protocol.TOAST,  "Unable to connect device");
        sendMessage(intent);

        // Re-initialise the service
        setConnectionState(STATE_NONE);
        initialiseService();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void handleLostConnection() {
        // Send a failure message back to the Activity
        Intent intent = new Intent(ACTION);
        intent.putExtra(Protocol.MESSAGE_TYPE, Protocol.MESSAGE_TOAST);
        intent.putExtra(Protocol.TOAST,  "Device connection was lost");
        sendMessage(intent);

        setConnectionState(STATE_NONE);
        //reconnect(mLastConnectedDevice);

        // Start the service over to restart listening mode
        initialiseService();
        //connect(mLastConnectedDevice);
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private BluetoothServerSocket mmServerSocket;

        AcceptThread() {
            // Create a new listening server socket
            try {
                mmServerSocket = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
                setConnectionState(STATE_LISTEN);
                Log.d(TAG, "AcceptThread created");

            } catch (IOException e) {
                Log.e(TAG, "Unable to listen for connection", e);
                initialiseService();
            }
        }

        public void run() {
            Log.d(TAG, "AcceptThread started");
            setConnectionState(STATE_LISTEN);
            BluetoothSocket socket;

            // Listen to the server socket if we're not connected
            while (getConnectionState() != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();

                } catch (IOException e) {
                    Log.e(TAG, "Unable to accept connection", e);
                    break;
                }

                /*if (socket != null && getConnectionState() == STATE_CONNECTING)
                    connected(socket, socket.getRemoteDevice());*/

                // If a connection was accepted
                if (socket != null) {
                    synchronized (this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice());
                                break;

                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close connection", e);
                                }
                                break;
                        }
                    }
                }
            }
        }

        void cancel() {
            try {
                mmServerSocket.close();

            } catch (IOException e) {
                Log.e(TAG, "Unable to close connection", e);
            }

            Log.d(TAG, "AcceptThread ended");
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private BluetoothDevice mmDevice;

        ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            setConnectionState(STATE_CONNECTING);

            try {
                mmSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                Log.d(TAG, "ConnectThread created");

            } catch (IOException e) {
                Log.e(TAG, "Unable to establish connection", e);
                handleFailedConnection();
            }
        }

        public void run() {
            Log.i(TAG, "ConnectThread started");
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();

            } catch (IOException e) {
                Log.e(TAG, "Unable to establish connection", e);

                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "Unable to close connection", e2);
                }

                // Start the service over to restart listening mode
                handleFailedConnection();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        void cancel() {
            try {
                mmSocket.close();

            } catch (IOException e) {
                Log.e(TAG, "Unable to close connection", e);
            }

            Log.d(TAG, "ConnectThread ended");
        }

        public void closeSocket() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private BluetoothSocket mmSocket;
        private InputStream mmInStream;
        private OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;

            // Get the BluetoothSocket input and output streams
            try {
                mmInStream = socket.getInputStream();
                mmOutStream = socket.getOutputStream();
                mLastConnectedDevice = mmSocket.getRemoteDevice();
                Log.d(TAG, "ConnectedThread created");

            } catch (IOException e) {
                Log.e(TAG, "Bluetooth connection is lost", e);
                handleLostConnection();
            }
        }

        public void run() {
            Log.i(TAG, "ConnectedThread started");
            setConnectionState(STATE_CONNECTED);
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI Activity
                    //String readMessage = new String(buffer, 0, bytes);
                    Intent intent = new Intent(ACTION);
                    Bundle bundle = new Bundle();
                    bundle.putInt(Protocol.MESSAGE_TYPE, Protocol.MESSAGE_READ);
                    bundle.putInt(Protocol.MESSAGE_BYTES, bytes);
                    bundle.putByteArray(Protocol.MESSAGE_BUFFER, buffer);
                    intent.putExtras(bundle);
                    sendMessage(intent);
                    Log.e(Config.log_id, "received bundle");

                } catch (IOException e) {
                    Log.e(TAG, "Bluetooth connection is lost", e);
                    mConnectThread = null;
                    mConnectedThread = null;
                    handleLostConnection();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                Intent intent = new Intent(ACTION);
                Bundle bundle = new Bundle();
                bundle.putInt(Protocol.MESSAGE_TYPE, Protocol.MESSAGE_WRITE);
                bundle.putByteArray(Protocol.MESSAGE_BUFFER, buffer);
                intent.putExtras(bundle);
                sendMessage(intent);

            } catch (IOException e) {
                Log.e(TAG, "Bluetooth connection is lost", e);
                handleLostConnection();
            }
        }

        void cancel() {
            try {
                mmSocket.close();

            } catch (IOException e) {
                Log.e(TAG, "Unable to close connection", e);
            }

            Log.d(TAG, "ConnectedThread ended");
        }
    }

    /*private void reconnect(BluetoothDevice device) {
        synchronized (this) {
            if (mLastConnectedDevice != null) {
                Log.d(Config.log_id, "Reconnecting to " + device);
                long start = System.currentTimeMillis();
                long now = start;
                // Attempt to reconnect during the timeout
                while (now - start <= BLUETOOTH_RECONNECTION_TIMEOUT &&
                        mState != STATE_CONNECTED) {
                    initialiseService();
                    connect(device);
                    now = System.currentTimeMillis();
                }
            }
            if (mState != STATE_CONNECTED)
                initialiseService();
        }
    }*/

    void sendMessage(Intent intent) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void sendText(String text, Context context) {
        if(bcsInstance != null && getConnectionState() == BluetoothService.STATE_CONNECTED) {
            if (text.length() > 0) {
                // Get the message bytes and tell the BluetoothChatService to write
                byte[] send = text.getBytes();
                bcsInstance.write(send);
            }
        } else {
            Toast.makeText(context, "Not connected to any Bluetooth Device!\nCommands not sent.", Toast.LENGTH_SHORT).show();
        }
    }
}