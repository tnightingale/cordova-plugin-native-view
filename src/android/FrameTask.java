package com.affinitybridge.websocketserver;

import android.graphics.Bitmap;

import com.koushikdutta.async.http.WebSocket;

//import org.java_websocket.WebSocket;

class FrameTask implements
        ViewSnapshotRunnable.TaskRunnableSnapshotMethods,
        BitmapCompressRunnable.TaskRunnableCompressMethods,
        FrameSendRunnable.TaskRunnableSendMethods {

    private static FrameManager sFrameManager;

    private Snapper mSnapper;

    private WebSocket mConnection;

    private Bitmap mSnapshot;

    private byte[] mFrame;

    private Thread mUiThread;

    private Thread mCompressThread;

    private Thread mFrameSendThread;

    private Runnable mViewSnapshotRunnable;

    private Runnable mBitmapCompressRunnable;

    private Runnable mFrameSendRunnable;

    FrameTask() {

    }

    public void handleState(int state) {
        sFrameManager.handleState(this, state);
    }

    void initializeViewSnapshotTask(FrameManager frameManager, Snapper snapper, WebSocket connection) {
        sFrameManager = frameManager;
        mSnapper = snapper;
        mConnection = connection;
    }

    Runnable getViewSnapshotRunnable() {
        return mViewSnapshotRunnable;
    }

    Runnable getBitmapCompressRunnable() {
        return mBitmapCompressRunnable;
    }

    Runnable getFrameSendRunnable() {
        return mFrameSendRunnable;
    }

    public void setViewSnapshotThread(Thread uiThread) {
        synchronized (sFrameManager) {
            mUiThread = uiThread;
        }
    }

    public void setBitmapCompressThread(Thread compressThread) {
        synchronized (sFrameManager) {
            mCompressThread = compressThread;
        }
    }

    public void setFrameSendThread(Thread frameSendThread) {
        synchronized (sFrameManager) {
            mFrameSendThread = frameSendThread;
        }
    }

    public Snapper getSnapper() {
        return mSnapper;
    }

    public void setSnapper(Snapper snapper) {
        mSnapper = snapper;
        mViewSnapshotRunnable = new ViewSnapshotRunnable(this);
        mBitmapCompressRunnable = new BitmapCompressRunnable(this, snapper.getByteCount());
        mFrameSendRunnable = new FrameSendRunnable(this);
    }

    public void setFrameBitmap(Bitmap snapshot) {
        mSnapshot = snapshot;
    }

    public Bitmap getFrameBitmap() {
        return mSnapshot;
    }

    public void setFrame(byte[] frame) {
        mFrame = frame;
    }

    public byte[] getFrame() {
        return mFrame;
    }

    public WebSocket getConnection() {
        return mConnection;
    }

    public void handleViewSnapshotState(int state) {
        int outState;
        switch (state) {
            case ViewSnapshotRunnable.SNAPSHOT_COMPLETE:
                outState = FrameManager.SNAPSHOT_COMPLETE;
                handleState(outState);
                break;
        }
    }

    public void handleBitmapCompressState(int state) {
        int outState;
        switch (state) {
            case BitmapCompressRunnable.COMPRESS_COMPLETE:
                outState = FrameManager.COMPRESS_COMPLETE;
                handleState(outState);
                break;
        }
    }

    public void handleFrameSendState(int state) {
        int outState;
        switch (state) {
            case FrameSendRunnable.SEND_COMPLETE:
                outState = FrameManager.SEND_COMPLETE;
                handleState(outState);
                break;
        }
    }

    void recycle() {
        mSnapshot = null;
        mFrame = null;
    }
}