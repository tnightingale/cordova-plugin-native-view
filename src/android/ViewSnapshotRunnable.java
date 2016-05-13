package com.tnightingale.cordova.nativeview;

import android.graphics.Bitmap;

class ViewSnapshotRunnable implements Runnable {
    static final int SNAPSHOT_COMPLETE = 5;
    final TaskRunnableSnapshotMethods mFrameTask;

    interface TaskRunnableSnapshotMethods {
        void setViewSnapshotThread(Thread currentThread);
        Snapper getSnapper();
        void setFrameBitmap(Bitmap snapshot);
        void handleViewSnapshotState(int state);
    }

    ViewSnapshotRunnable(TaskRunnableSnapshotMethods snapshotTask) {
        mFrameTask = snapshotTask;
    }

    @Override
    public void run() {
        mFrameTask.setViewSnapshotThread(Thread.currentThread());
        Bitmap snapshot = mFrameTask.getSnapper().snap();
        mFrameTask.setFrameBitmap(snapshot);
        mFrameTask.handleViewSnapshotState(SNAPSHOT_COMPLETE);
    }
}