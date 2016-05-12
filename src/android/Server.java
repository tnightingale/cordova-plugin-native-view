package com.affinitybridge.websocketserver;

import android.util.Log;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.net.UnknownHostException;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class Server extends WebSocketServer {

    private static String TAG = "Server";

    ArrayList<ConnectionListener> listeners = new ArrayList<ConnectionListener>();

    public Server(int port) throws UnknownHostException {
        super(new InetSocketAddress(port));
    }

    public void addConnectionListener(ConnectionListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Log.d(TAG, conn.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the room!");
        for (ConnectionListener listener : this.listeners) {
            listener.onOpen(conn, handshake);
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        Log.d(TAG, conn + " has left the room!");
        for (ConnectionListener listener : this.listeners) {
            listener.onClose(conn, code, reason, remote);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        Log.d(TAG, conn + ": " + message);
        for (ConnectionListener listener : this.listeners) {
            listener.onMessage(conn, message);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
        if (conn != null) {
            // some errors like port binding failed may not be assignable to a specific websocket
            for (ConnectionListener listener : this.listeners) {
                listener.onError(conn, ex);
            }
        }
    }

    /**
     * Sends <var>text</var> to all currently connected WebSocket clients.
     *
     * @param text
     *            The String to send across the network.
     * @throws InterruptedException
     *             When socket related I/O errors occur.
     */
    public void sendToAll(String text) {
        Collection<WebSocket> con = connections();
        synchronized (con) {
            for(WebSocket c : con) {
                c.send(text);
            }
        }
    }

    interface ConnectionListener {
        void onOpen(WebSocket conn, ClientHandshake handshake);
        void onClose(WebSocket conn, int code, String reason, boolean remote);
        void onMessage(WebSocket conn, String message);
        void onError(WebSocket conn, Exception e);
    }
}
