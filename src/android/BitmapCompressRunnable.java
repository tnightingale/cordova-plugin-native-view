package com.affinitybridge.websocketserver;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

class BitmapCompressRunnable implements Runnable {
    static final String TAG = BitmapCompressRunnable.class.getName();
    static final int COMPRESS_COMPLETE = 6;

    ByteBuffer input;


    final TaskRunnableCompressMethods mFrameTask;

    ByteArrayOutputStream out = new ByteArrayOutputStream();

    interface TaskRunnableCompressMethods {
        void setBitmapCompressThread(Thread currentThread);
        Bitmap getFrameBitmap();
        void setFrame(byte[] frame);
        void handleBitmapCompressState(int state);
        Snapper getSnapper();
    }

    BitmapCompressRunnable(TaskRunnableCompressMethods compressTask, int byteCount) {
        mFrameTask = compressTask;
        input = ByteBuffer.allocate(byteCount);
        input.mark();
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mFrameTask.setBitmapCompressThread(Thread.currentThread());

//        boolean success = mFrameTask.getFrameBitmap().compress(Bitmap.CompressFormat.JPEG, 10, out);
//        if (!success) {
//            Log.e(TAG, "Error compressing bitmap.");
//        }

        Deflater compressor = new Deflater(Deflater.BEST_SPEED);
        DeflaterOutputStream dos = new DeflaterOutputStream(out, compressor);
        mFrameTask.getFrameBitmap().copyPixelsToBuffer(input);
        byte[] data = input.array();
        try {
            dos.write(data, 0, data.length);
            dos.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

//        byte[] bytes = out.toByteArray();
//        out.reset();

        byte[] frame = out.toByteArray();
        out.reset();

//        Log.i(TAG, "Inflated size: " + data.length + ", Deflated size: " + frame.length);

        mFrameTask.setFrame(frame);
        mFrameTask.handleBitmapCompressState(COMPRESS_COMPLETE);

        input.reset();
    }
}