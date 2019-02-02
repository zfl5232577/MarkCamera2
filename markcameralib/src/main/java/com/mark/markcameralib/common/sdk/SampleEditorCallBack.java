package com.mark.markcameralib.common.sdk;

import android.util.Log;

import com.aliyun.editor.EditorCallBack;

/**
 * <pre>
 *     author : Mark
 *     e-mail : makun.cai@aorise.org
 *     time   : 2019/02/01
 *     desc   : TODO
 *     version: 1.0
 * </pre>
 */
public class SampleEditorCallBack implements EditorCallBack {
    private static final String TAG = SampleEditorCallBack.class.getSimpleName();

    @Override
    public void onEnd(int i) {
        Log.e(TAG, "onEnd: " +i);
    }

    @Override
    public void onError(int i) {
        Log.e(TAG, "onError: " );
    }

    @Override
    public int onCustomRender(int i, int i1, int i2) {
        Log.e(TAG, "onCustomRender: " );
        return 0;
    }

    @Override
    public int onTextureRender(int i, int i1, int i2) {
        Log.e(TAG, "onTextureRender: " );
        return 0;
    }

    @Override
    public void onPlayProgress(long l, long l1) {
        Log.e(TAG, "onPlayProgress: " );
    }

    @Override
    public void onDataReady() {
        Log.e(TAG, "onDataReady: " );
    }
}
