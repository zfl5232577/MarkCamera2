package com.mark.markcamera2;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.mark.markcameralib.EditImageView;
import com.mark.markcameralib.EditVideoView;

public class EditImageActivity extends AppCompatActivity {

    private EditImageView editImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_image);
        String path = getIntent().getStringExtra("path");
        editImageView = findViewById(R.id.editImageView);
        editImageView.setImageUrl(path);
        editImageView.setCallback(new EditImageView.EditImageViewCallback() {
            @Override
            public void cancelEdit() {
                finish();
            }

            @Override
            public void completeEdit(String outPath) {
                Toast.makeText(EditImageActivity.this, "图片编辑成功->"+outPath, Toast.LENGTH_SHORT).show();
                setResult(Activity.RESULT_OK);
                finish();
            }

            @Override
            public void editProgress(int progress) {

            }

            @Override
            public void editError() {
                Toast.makeText(EditImageActivity.this, "图片编辑不支持该格式", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
