package com.mark.markcamera2;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

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
    private RecordVideoView mRecordVideoView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        mRecordVideoView = findViewById(R.id.recordVideoView);
        mRecordVideoView.setMode(getIntent().getIntExtra("mode",RecordVideoView.TAKE_PHOTO_RECORD));
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
