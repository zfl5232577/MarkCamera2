package com.mark.markcamera2;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.mark.markcameralib.EditVideoView;

public class EditVideoActivity extends AppCompatActivity {

    private EditVideoView editVideoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_video);
        String path = getIntent().getStringExtra("path");
        editVideoView = findViewById(R.id.editVideoView);
        editVideoView.setVideoUrl(path);
        editVideoView.setCallback(new EditVideoView.EditVideoViewCallback() {
            @Override
            public void cancelEdit() {
                finish();
            }

            @Override
            public void completeEdit(String outPath) {
                Toast.makeText(EditVideoActivity.this, "视频编辑成功->"+outPath, Toast.LENGTH_SHORT).show();
                setResult(Activity.RESULT_OK);
                finish();
            }

            @Override
            public void editProgress(int progress) {

            }

            @Override
            public void editError() {
                Toast.makeText(EditVideoActivity.this, "视频编辑不支持该格式", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
