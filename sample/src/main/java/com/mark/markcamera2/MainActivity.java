package com.mark.markcamera2;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.mark.aoplibrary.annotation.CheckPermission;
import com.mark.aoplibrary.utils.MPermissionUtils;
import com.mark.markcameralib.RecordVideoView;

import io.reactivex.annotations.NonNull;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.bt_Photo).setOnClickListener(this);
        findViewById(R.id.bt_Record).setOnClickListener(this);
        findViewById(R.id.bt_all).setOnClickListener(this);
    }

    /**
     *  @see <a href="https://github.com/zfl5232577/MarkAop">切面框架</a>
     */
    @CheckPermission({Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    @Override
    public void onClick(View v) {
        int id = v.getId();
        Intent intent = new Intent(this, RecordActivity.class);
        int mode = RecordVideoView.TAKE_PHOTO_RECORD;
        if (id == R.id.bt_Photo) {
            mode = RecordVideoView.TAKE_PHOTO;
        } else if (id == R.id.bt_Record) {
            mode = RecordVideoView.TAKE_RECORD;
        } else if (id == R.id.bt_all) {
            mode = RecordVideoView.TAKE_PHOTO_RECORD;
        }
        intent.putExtra("mode", mode);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MPermissionUtils.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
