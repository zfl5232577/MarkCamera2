package com.mark.markcamera2;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

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
    }
}
