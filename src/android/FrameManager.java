package com.affinitybridge.websocketserver;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import org.java_websocket.WebSocket;

//import com.koushikdutta.async.http.WebSocket;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class FrameManager {
    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    private static final int KEEP_ALIVE_TIME = 1;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

    static final int REQUEST_SNAPSHOT = 1;
    static final int SNAPSHOT_COMPLETE = 2;
    static final int COMPRESS_COMPLETE = 3;
    static final int SEND_COMPLETE = 4;

    private final BlockingQueue<Runnable> mCompressWorkQueue = new LinkedBlockingQueue<Runnable>();
    private final BlockingQueue<Runnable> mFrameSendQueue = new LinkedBlockingQueue<Runnable>();
    private final Queue<FrameTask> mFrameTaskQueue = new LinkedBlockingQueue<FrameTask>(NUMBER_OF_CORES * 2);

    private ThreadPoolExecutor mCompressThreadPool;
    private ThreadPoolExecutor mFrameSendThreadPool;

    private final Handler mHandler;

    private static FrameManager sInstance = new FrameManager();

    private FrameManager() {
        mCompressThreadPool = new ThreadPoolExecutor(1,1,//NUMBER_OF_CORES, NUMBER_OF_CORES,
                KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mCompressWorkQueue);

        mFrameSendThreadPool = new ThreadPoolExecutor(1,1, //NUMBER_OF_CORES, NUMBER_OF_CORES,
                KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mFrameSendQueue);

        for (int i = 0; i < NUMBER_OF_CORES * 2; i++) {
            mFrameTaskQueue.offer(new FrameTask());
        }

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                FrameTask frameTask = (FrameTask) inputMessage.obj;
                switch (inputMessage.what) {
                    case REQUEST_SNAPSHOT:
                        this.post(frameTask.getViewSnapshotRunnable());
                        break;
                    default:
                        super.handleMessage(inputMessage);
                }
            }
        };
    }

    public void handleState(FrameTask frameTask, int state) {
        switch (state) {
            case REQUEST_SNAPSHOT:
                Message requestMessage = mHandler.obtainMessage(state, frameTask);
                requestMessage.sendToTarget();
                break;

            case SNAPSHOT_COMPLETE:
                mCompressThreadPool.execute(frameTask.getBitmapCompressRunnable());
                break;

            case COMPRESS_COMPLETE:
                mFrameSendThreadPool.execute(frameTask.getFrameSendRunnable());
                break;

            case SEND_COMPLETE:
                recycleTask(frameTask);
                break;
        }
    }

    static public void setSnapper(Snapper snapper) {
        int count = 0;
        for (FrameTask frameTask : sInstance.mFrameTaskQueue) {
            Log.d("SetSnapper", "" + count++);
            frameTask.setSnapper(snapper);
        }
    }

    static public void getViewSnapshot(Snapper snapper, WebSocket connection) {
        FrameTask viewSnapshotTask = sInstance.mFrameTaskQueue.poll();

        if (null == viewSnapshotTask) {
            return;
        }

        viewSnapshotTask.initializeViewSnapshotTask(FrameManager.sInstance, snapper, connection);

        viewSnapshotTask.handleState(REQUEST_SNAPSHOT);

//        return viewSnapshotTask;
    }

    void recycleTask(FrameTask sendTask) {

        // Frees up memory in the task
        sendTask.recycle();

        // Puts the task object back into the queue for re-use.
        mFrameTaskQueue.offer(sendTask);
    }
}