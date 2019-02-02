package com.mark.markcamera2;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.mark.markcameralib.RecordVideoView;


/**
 * <pre>
 *     author : Mark
 *     e-mail : makun.cai@aorise.org
 *     time   : 2018/09/27
 *     desc   : TODO
 *     version: 1.0
 * </pre>
 */
public class RecordActivity extends AppCompatActivity {
    private static final String TAG = RecordActivity.class.getSimpleName();
    private RecordVideoView mRecordVideoView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        mRecordVideoView = findViewById(R.id.recordVideoView);
        mRecordVideoView.setMode(getIntent().getIntExtra("mode",RecordVideoView.TAKE_PHOTO_RECORD));
        mRecordVideoView.setRecordVideoListener(new RecordVideoView.RecordVideoListener() {
            @Override
            public void onRecordCaptureFinish() {

            }

            @Override
            public void onRestart() {

            }

            @Override
            public void onRecordTaken(String recordFilePath) {
                Log.e(TAG, "onRecordTaken: "+recordFilePath );
            }

            @Override
            public void onPictureTaken(String pictureFilePath) {
                Log.e(TAG, "onPictureTaken: "+pictureFilePath );
            }

            @Override
            public void onEditPicture(String pictureFilePath) {
                Log.e(TAG, "onEditPicture: "+pictureFilePath );
            }

            @Override
            public void onEditRecord(String recordFilePath) {
                Log.e(TAG, "onEditRecord: "+recordFilePath );
                Intent intent = new Intent(RecordActivity.this,EditVideoActivity.class);
                intent.putExtra("path",recordFilePath);
                startActivity(intent);
            }

            @Override
            public void quit() {
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRecordVideoView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRecordVideoView.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRecordVideoView.onDestroy();
    }
}
