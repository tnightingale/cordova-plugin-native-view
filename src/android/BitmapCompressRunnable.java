package com.affinitybridge.websocketserver;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

class BitmapCompressRunnable implements Runnable {
    static final String TAG = BitmapCompressRunnable.class.getName();
    static final int COMPRESS_COMPLETE = 6;

    private ByteArrayOutputStream out = new ByteArrayOutputStream();
    Deflater def = new Deflater(Deflater.BEST_SPEED);
    ByteBuffer input;
    byte[] output;
    DeflaterOutputStream dos = new DeflaterOutputStream(out, def);

    final TaskRunnableCompressMethods mFrameTask;

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
        output = new byte[byteCount];
        input.mark();
    }

    @Override
    public void run() {
//        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mFrameTask.setBitmapCompressThread(Thread.currentThread());

//        boolean success = mFrameTask.getFrameBitmap().compress(Bitmap.CompressFormat.JPEG, 10, out);
//        if (!success) {
//            Log.e(TAG, "Error compressing bitmap.");
//        }

        mFrameTask.getFrameBitmap().copyPixelsToBuffer(input);

        def.setInput(input.array());
        def.finish();
        input.reset();

        int bytesWritten = def.deflate(output);
        def.reset();

//        byte[] bytes = out.toByteArray();
//        out.reset();

        byte[] frame = Arrays.copyOfRange(output, 0, bytesWritten);
        Log.i(TAG, "Inflated size: " + output.length + ", Deflated size: " + bytesWritten + ", Frame size: " + frame.length);

        mFrameTask.setFrame(frame);
        mFrameTask.handleBitmapCompressState(COMPRESS_COMPLETE);
    }
}