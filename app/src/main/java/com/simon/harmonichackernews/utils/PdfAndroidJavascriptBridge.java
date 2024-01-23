package com.simon.harmonichackernews.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.webkit.JavascriptInterface;

import androidx.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

// TODO Move this class out of here
public class PdfAndroidJavascriptBridge {
    private final File mFile;
    private @Nullable
    RandomAccessFile mRandomAccessFile;
    private final @Nullable Callbacks mCallback;
    private final Handler mHandler;

    public PdfAndroidJavascriptBridge(String filePath, @Nullable Callbacks callback) {
        mFile = new File(filePath);
        mCallback = callback;
        mHandler = new Handler(Looper.getMainLooper());
    }

    @JavascriptInterface
    public String getChunk(long begin, long end) {
        try {
            if (mRandomAccessFile == null) {
                mRandomAccessFile = new RandomAccessFile(mFile, "r");
            }
            final int bufferSize = (int) (end - begin);
            byte[] data = new byte[bufferSize];
            mRandomAccessFile.seek(begin);
            mRandomAccessFile.read(data);
            return Base64.encodeToString(data, Base64.DEFAULT);
        } catch (IOException e) {
            Log.e("Exception", e.toString());
            return "";
        }
    }

    @JavascriptInterface
    public long getSize() {
        return mFile.length();
    }

    @JavascriptInterface
    public void onLoad() {
        if (mCallback != null) {
            mHandler.post(mCallback::onLoad);
        }
    }

    @JavascriptInterface
    public void onFailure() {
        if (mCallback != null) {
            mHandler.post(mCallback::onFailure);
        }
    }

    public void cleanUp() {
        try {
            if (mRandomAccessFile != null) {
                mRandomAccessFile.close();
            }
        } catch (IOException e) {
            Log.e("Exception", e.toString());
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            cleanUp();
        } finally {
            super.finalize();
        }
    }

    public interface Callbacks {
        void onFailure();

        void onLoad();
    }
}
