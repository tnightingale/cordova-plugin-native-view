package com.affinitybridge.websocketserver;

import org.java_websocket.WebSocket;

//import android.graphics.Bitmap;
//import java.nio.ByteBuffer;

//import com.koushikdutta.async.http.WebSocket;

class FrameSendRunnable implements Runnable {
    static final int SEND_COMPLETE = 7;

    final TaskRunnableSendMethods mFrameTask;

    interface TaskRunnableSendMethods {
        void setFrameSendThread(Thread currentThread);
        byte[] getFrame();
        WebSocket getConnection();
        void handleFrameSendState(int state);
//        Bitmap getFrameBitmap();
    }

    FrameSendRunnable(TaskRunnableSendMethods sendTask) {
        mFrameTask = sendTask;
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mFrameTask.setFrameSendThread(Thread.currentThread());
        byte[] frame = mFrameTask.getFrame();

//        Bitmap snap = mFrameTask.getFrameBitmap();
//        ByteBuffer buffer = ByteBuffer.allocate(snap.getByteCount());
//        buffer.mark();
//        snap.copyPixelsToBuffer(buffer);
//        byte[] frame = buffer.array();
//        buffer.reset();

        WebSocket conn = mFrameTask.getConnection();
        conn.send(frame);
        mFrameTask.handleFrameSendState(SEND_COMPLETE);
    }
}