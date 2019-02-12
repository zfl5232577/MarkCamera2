package com.mark.markcameralib;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aliyun.common.global.AliyunTag;
import com.aliyun.crop.AliyunCropCreator;
import com.aliyun.crop.struct.CropParam;
import com.aliyun.crop.supply.AliyunICrop;
import com.aliyun.crop.supply.CropCallback;
import com.aliyun.qupai.editor.AliyunIComposeCallBack;
import com.aliyun.qupai.editor.AliyunIEditor;
import com.aliyun.qupai.editor.impl.AliyunEditorFactory;
import com.aliyun.qupai.import_core.AliyunIImport;
import com.aliyun.qupai.import_core.AliyunImportCreator;
import com.aliyun.svideo.sdk.external.struct.common.AliyunDisplayMode;
import com.aliyun.svideo.sdk.external.struct.common.AliyunVideoParam;
import com.aliyun.svideo.sdk.external.struct.common.VideoDisplayMode;
import com.aliyun.svideo.sdk.external.struct.common.VideoQuality;
import com.aliyun.svideo.sdk.external.struct.encoder.VideoCodecs;
import com.aliyun.vod.common.utils.DensityUtil;
import com.mark.markcameralib.common.sdk.SampleEditorCallBack;
import com.mark.markcameralib.crop.FrameExtractor10;
import com.mark.markcameralib.crop.VideoTrimAdapter;
import com.mark.markcameralib.view.CutView;
import com.mark.markcameralib.view.HorizontalListView;
import com.mark.markcameralib.view.SoftKeyBoardListener;
import com.mark.markcameralib.view.TouchView;
import com.mark.markcameralib.view.TuyaView;
import com.mark.markcameralib.view.VideoPlayView;
import com.mark.markcameralib.view.VideoSliceSeekBar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * <pre>
 *     author : Mark
 *     e-mail : makun.cai@aorise.org
 *     time   : 2019/01/30
 *     desc   : 视频编辑控件，涂鸦，添加表情，文字，视频裁剪
 *     version: 1.0
 * </pre>
 */
public class EditVideoView extends FrameLayout {

    private static final String TAG = EditVideoView.class.getSimpleName();
    private int[] drawableBg = new int[]{R.drawable.color1, R.drawable.color2, R.drawable.color3, R.drawable.color4, R.drawable.color5};
    private int[] colors = new int[]{R.color.color1, R.color.color2, R.color.color3, R.color.color4, R.color.color5};
    private int[] expressions = new int[]{R.mipmap.expression1, R.mipmap.expression2, R.mipmap.expression3, R.mipmap.expression4,
            R.mipmap.expression5, R.mipmap.expression6, R.mipmap.expression7, R.mipmap.expression8};

    private VideoPlayView mVideoPlayView;
    private LinearLayout llTuYAColor;
    private View v_line;
    private LinearLayout ll_text_color;
    private RelativeLayout rl_touch_view;
    private RelativeLayout rl_edit_text;
    private RelativeLayout rl_cut;
    private RelativeLayout rl_clip;
    private HorizontalListView video_tailor_image_list;
    private VideoSliceSeekBar seek_bar;
    private TextView tv_video_time;
    private TextView tv_cancel_clip;
    private TextView tv_complete_clip;
    private EditText et_tag;
    private TextView tv_tag;
    private TextView tv_hint_delete;
    private RelativeLayout rl_tuya;
    private TuyaView tuyaView;
    private RelativeLayout layout_control;
    private TextView tv_cancel_edit;
    private TextView tv_complete_edit;
    private ImageView iv_tuya;
    private ImageView iv_biaoqing;
    private ImageView iv_wenzi;
    private ImageView iv_cut;
    private ImageView iv_clip;
    private CutView cutView;
    private TextView tv_cancel_cut;
    private TextView tv_complete_cut;
    private RelativeLayout rl_back;
    private RelativeLayout rl_expression;
    private TextView tv_cancel_text_edit;
    private TextView tv_complete_text_dit;
    private RelativeLayout rl_text_color_or_bg_color;
    private ImageView iv_text_color_or_bg_color;
    private int currentColorPosition;
    private int currentTextColorPosition;
    private int currentTextBgColorPosition = -1;
    private LinearLayout layout_control_tab;

    private EditVideoViewCallback mCallback;

    private String videoUrl;
    private String outFileDir = Constants.TEMP_PATH;
    private boolean isTextColor = true;
    private boolean isFirstShowEditText;
    private InputMethodManager manager;
    private AliyunICrop mAliyunCrop;
    private AliyunIEditor mAliyunEditor;
    private CropParam mCropParam;
    private AliyunVideoParam mVideoParam;

    private Handler mBackgroundHandler;
    private AlertDialog progressDialog;
    private TextView progressTextView;
    private int mVideoDuration;
    private int mVideoWidth;
    private int mVideoHeight;
    private FrameExtractor10 kFrame;
    private VideoTrimAdapter adapter;
    private long mStartTime;
    private long mEndTime;
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            long currentPlayPos = mVideoPlayView.getCurrentPos();
            Log.d(TAG, "currentPlayPos:" + currentPlayPos);
            if (currentPlayPos < mEndTime) {
                seek_bar.showFrameProgress(true);
                seek_bar.setFrameProgress(currentPlayPos / (float) mVideoDuration);
            } else {
                mVideoPlayView.seekTo((int) mStartTime);
            }
            postDelayed(this, 100);
        }
    };

    public void showProgressDialog(String dialogText) {

        if (progressDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setCancelable(false);
            View view = View.inflate(getContext(), R.layout.markcamera_dialog_loading, null);
            builder.setView(view);
            ProgressBar pb_loading = view.findViewById(R.id.pb_loading);
            progressTextView = view.findViewById(R.id.tv_hint);
            progressTextView.setText(dialogText);
            progressDialog = builder.create();
        } else {
            progressTextView.setText(dialogText);
        }
        progressDialog.show();
    }

    public void closeProgressDialog() {
        try {
            if (progressDialog != null) {
                progressDialog.cancel();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Handler getBackgroundHandler() {
        if (mBackgroundHandler == null) {
            HandlerThread thread = new HandlerThread("edit_video");
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }
        return mBackgroundHandler;
    }

    public EditVideoView(@NonNull Context context) {
        this(context, null);
    }

    public EditVideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EditVideoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        initExpression();
        setListViewHeight();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(mRunnable);
        Log.e(TAG, "onDetachedFromWindow:------------------------> ");
        if (mAliyunCrop != null) {
            mAliyunCrop.dispose();
            mAliyunCrop = null;
        }
        if (mAliyunEditor != null) {
            mAliyunEditor.onDestroy();
            mAliyunEditor = null;
        }

        if (mBackgroundHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBackgroundHandler.getLooper().quitSafely();
            } else {
                mBackgroundHandler.getLooper().quit();
            }
            mBackgroundHandler = null;
        }
    }

    private void init(Context context) {
        initView(context);
        initEvent();
        initColors();
        initManager(context);
        initAliyunICrop(context);
    }

    private void initAliyunICrop(Context context) {
        mAliyunCrop = AliyunCropCreator.createCropInstance(context);
        mVideoParam = new AliyunVideoParam.Builder()
                .gop(10)
                .frameRate(25)
                .videoQuality(VideoQuality.HD)
                .videoCodec(VideoCodecs.H264_SOFT_FFMPEG)
                .scaleMode(VideoDisplayMode.SCALE)
                .build();
    }

    private void initManager(Context context) {
        ((Activity) context).getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        manager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        SoftKeyBoardListener listener = new SoftKeyBoardListener((Activity) context);
        listener.setOnSoftKeyBoardChangeListener(new SoftKeyBoardListener.OnSoftKeyBoardChangeListener() {
            @Override
            public void keyBoardShow(int height) {
                Log.e(TAG, "keyBoardShow: " + height);
                ll_text_color.setTranslationY(-height);
            }

            @Override
            public void keyBoardHide(int height) {
                Log.e(TAG, "keyBoardHide: " + height);
                ll_text_color.setTranslationY(0);
            }
        });
    }

    private void initView(Context context) {
        FrameLayout.inflate(context, R.layout.markcamera_layout_edit_video_view, this);
        mVideoPlayView = findViewById(R.id.videoPlayView);
        rl_tuya = findViewById(R.id.rl_tuya);
        tuyaView = findViewById(R.id.tuyaView);
        rl_touch_view = findViewById(R.id.rl_touch_view);
        layout_control = findViewById(R.id.layout_control);
        rl_cut = findViewById(R.id.rl_cut);
        cutView = findViewById(R.id.cutView);
        rl_clip = findViewById(R.id.rl_clip);
        video_tailor_image_list = findViewById(R.id.video_tailor_image_list);
        seek_bar = findViewById(R.id.seek_bar);
        tv_video_time = findViewById(R.id.tv_video_time);
        tv_cancel_clip = findViewById(R.id.tv_cancel_clip);
        tv_complete_clip = findViewById(R.id.tv_complete_clip);
        tv_cancel_cut = findViewById(R.id.tv_cancel_cut);
        tv_complete_cut = findViewById(R.id.tv_complete_cut);
        tv_cancel_edit = findViewById(R.id.tv_cancel_edit);
        tv_complete_edit = findViewById(R.id.tv_complete_edit);
        iv_tuya = findViewById(R.id.iv_tuya);
        iv_biaoqing = findViewById(R.id.iv_biaoqing);
        iv_wenzi = findViewById(R.id.iv_wenzi);
        iv_cut = findViewById(R.id.iv_cut);
        iv_clip = findViewById(R.id.iv_clip);
        llTuYAColor = findViewById(R.id.llTuYAColor);
        rl_back = findViewById(R.id.rl_back);
        rl_expression = findViewById(R.id.rl_expression);
        rl_edit_text = findViewById(R.id.rl_edit_text);
        tv_cancel_text_edit = findViewById(R.id.tv_cancel_text_edit);
        tv_complete_text_dit = findViewById(R.id.tv_complete_text_dit);
        et_tag = findViewById(R.id.et_tag);
        tv_tag = findViewById(R.id.tv_tag);
        ll_text_color = findViewById(R.id.ll_text_color);
        rl_text_color_or_bg_color = findViewById(R.id.rl_text_color_or_bg_color);
        iv_text_color_or_bg_color = findViewById(R.id.iv_text_color_or_bg_color);
        tv_hint_delete = findViewById(R.id.tv_hint_delete);
        v_line = findViewById(R.id.v_line);
        layout_control_tab = findViewById(R.id.layout_control_tab);
    }

    private void setListViewHeight() {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) video_tailor_image_list.getLayoutParams();
        layoutParams.height = getWidth() / 8;
        video_tailor_image_list.setLayoutParams(layoutParams);
        seek_bar.setLayoutParams(layoutParams);
    }

    private void initEvent() {

        iv_tuya.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                changeTuYaState(!(llTuYAColor.getVisibility() == View.VISIBLE));
            }
        });

        iv_biaoqing.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                changeTuYaState(false);
                rl_expression.setVisibility(VISIBLE);
                rl_expression.setFocusable(true);
                rl_expression.setFocusableInTouchMode(true);
                rl_expression.requestFocus();
            }
        });
        iv_wenzi.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                changeTuYaState(false);
                changeTextState(!rl_edit_text.isShown());
            }
        });

        iv_cut.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                changeCutState(!rl_cut.isShown());
            }
        });

        iv_clip.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                changeClipState(!rl_clip.isShown());
            }
        });
        tv_cancel_cut.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                changeCutState(false);
            }
        });

        tv_complete_cut.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                cropVideo(true);
            }
        });

        rl_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tuyaView.backPath();
            }
        });

        tv_cancel_text_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeTextState(!rl_edit_text.isShown());
            }
        });

        et_tag.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                tv_tag.setText(s);
            }
        });

        tv_complete_text_dit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                tv_tag.setTextColor(getResources().getColor(colors[currentTextColorPosition]));
                if (currentTextBgColorPosition != -1) {
                    tv_tag.setBackgroundColor(getResources().getColor(colors[currentTextBgColorPosition]));
                }
                changeTextState(!rl_edit_text.isShown());
                if (et_tag.getText().length() > 0) {
                    addTextToWindow();
                }
            }
        });

        tv_complete_edit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO 编辑视频
//                mCropParam = new CropParam();
//                mCropParam.setVideoCodec(VideoCodecs.H264_SOFT_FFMPEG);
//                mCropParam.setInputPath(videoUrl);
//                mCropParam.setOutputPath(outFileDir+ File.separator+System.currentTimeMillis()+".mp4");
//                Bitmap bitmap = Bitmap.createBitmap(rl_tuya.getWidth(), rl_tuya.getHeight(), Bitmap.Config.ARGB_8888);
//                rl_tuya.draw(new Canvas(bitmap));
                showProgressDialog("视频编辑中...");
                getBackgroundHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        rl_tuya.setDrawingCacheEnabled(true);
                        rl_tuya.buildDrawingCache();  //启用DrawingCache并创建位图
                        int bitmapHeight =  mVideoHeight*getWidth()/mVideoWidth;
                        Bitmap bitmap = Bitmap.createBitmap(rl_tuya.getDrawingCache(),0,(getHeight()-bitmapHeight)/2,getWidth(),bitmapHeight); //创建一个DrawingCache的拷贝，因为DrawingCache得到的位图在禁用后会被回收
                        rl_tuya.setDrawingCacheEnabled(false);
                        rl_tuya.destroyDrawingCache();
                        final File pictureFile = new File(outFileDir, System.currentTimeMillis() + ".png");
                        FileOutputStream fileOutputStream = null;
                        try {
                            fileOutputStream = new FileOutputStream(pictureFile);
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                fileOutputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        mAliyunEditor.applyWaterMark(pictureFile.getAbsolutePath(), 1f, 1f, 0.5f, 0.5f);
                        final String outFilePath = outFileDir + File.separator + System.currentTimeMillis() + ".mp4";
                        mAliyunEditor.compose(mVideoParam, outFilePath, new AliyunIComposeCallBack() {
                            @Override
                            public void onComposeError(int i) {
                                post(new Runnable() {
                                    @Override
                                    public void run() {
                                        closeProgressDialog();
                                        if (mCallback != null) {
                                            mCallback.editError();
                                        }
                                    }
                                });
                            }

                            @Override
                            public void onComposeProgress(int i) {
                                if (mCallback != null) {
                                    mCallback.editProgress(i);
                                }
                            }

                            @Override
                            public void onComposeCompleted() {
                                pictureFile.delete();
                                post(new Runnable() {
                                    @Override
                                    public void run() {
                                        closeProgressDialog();
                                        if (mCallback != null) {
                                            mCallback.completeEdit(outFilePath);
                                        }
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });

        tv_cancel_edit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallback != null) {
                    mCallback.cancelEdit();
                }
            }
        });

        rl_text_color_or_bg_color.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                isTextColor = !isTextColor;
                if (isTextColor) {
                    iv_text_color_or_bg_color.setImageResource(R.mipmap.icon_text_color);
                    if (currentTextBgColorPosition >= 0) {
                        ViewGroup childView = (ViewGroup) ll_text_color.getChildAt(currentTextBgColorPosition);
                        childView.getChildAt(1).setVisibility(View.GONE);
                    }
                    ViewGroup childView1 = (ViewGroup) ll_text_color.getChildAt(currentTextColorPosition);
                    childView1.getChildAt(1).setVisibility(View.VISIBLE);
                    et_tag.setTextColor(getResources().getColor(colors[currentTextColorPosition]));
                } else {
                    iv_text_color_or_bg_color.setImageResource(R.mipmap.icon_text_bg_color);
                    ViewGroup childView = (ViewGroup) ll_text_color.getChildAt(currentTextColorPosition);
                    childView.getChildAt(1).setVisibility(View.GONE);
                    if (currentTextBgColorPosition >= 0) {
                        ViewGroup childView1 = (ViewGroup) ll_text_color.getChildAt(currentTextBgColorPosition);
                        childView1.getChildAt(1).setVisibility(View.VISIBLE);
                        et_tag.getPaint().bgColor = getResources().getColor(colors[currentTextBgColorPosition]);
                        et_tag.setTextColor(Color.TRANSPARENT);
                        et_tag.setTextColor(getResources().getColor(colors[currentTextColorPosition]));
                    }
                }
            }
        });

        seek_bar.setSeekBarChangeListener(new VideoSliceSeekBar.SeekBarChangeListener() {
            @Override
            public void seekBarValueChanged(float leftThumb, float rightThumb, int whitchSide) {
                long seekPos = 0;
                if (whitchSide == 0) {
                    seekPos = (long) (mVideoDuration * leftThumb / 100);
                    mStartTime = seekPos;
                } else if (whitchSide == 1) {
                    seekPos = (long) (mVideoDuration * rightThumb / 100);
                    mEndTime = seekPos;
                }
                tv_video_time.setText((float) (mEndTime - mStartTime) / 1000 + "s");
                if (mVideoPlayView != null) {
                    mVideoPlayView.seekTo((int) seekPos);
                }
            }

            @Override
            public void onSeekStart() {
                if (mVideoPlayView != null) {
                    mVideoPlayView.pause();
                }
            }

            @Override
            public void onSeekEnd() {
                if (mVideoPlayView != null) {
                    mVideoPlayView.seekTo((int) mStartTime);
                    mVideoPlayView.start();
                }
            }
        });

        video_tailor_image_list.setOnScrollCallBack(new HorizontalListView.OnScrollCallBack() {
            @Override
            public void onScrollDistance(Long count, int distanceX) {

            }
        });

        tv_cancel_clip.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                changeClipState(false);
            }
        });

        tv_complete_clip.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                cropVideo(false);
            }
        });

    }

    private void cropVideo(boolean isCut) {
        showProgressDialog("视频裁剪中...");
        CropParam cropParam = new CropParam();
        cropParam.setVideoCodec(VideoCodecs.H264_SOFT_FFMPEG);
        cropParam.setGop(10);
        cropParam.setCrf(0);
        cropParam.setScaleMode(VideoDisplayMode.SCALE);
        cropParam.setInputPath(videoUrl);
        final String cutOutPath = outFileDir + File.separator + "cut" + System.currentTimeMillis() + ".mp4";
        cropParam.setOutputPath(cutOutPath);

        if (isCut){

        float[] cutArr = cutView.getCutArr();
        float left = cutArr[0];
        float top = cutArr[1];
        float right = cutArr[2];
        float bottom = cutArr[3];
        if (left == 0 && top == 0 && right == 0 && bottom == 0) {
            changeCutState(false);
            return;
        }
        int cutWidth = cutView.getRectWidth();
        int cutHeight = cutView.getRectHeight();
        //计算宽高缩放比
        float leftPro = left / cutWidth;
        float topPro = top / cutHeight;
        float rightPro = right / cutWidth;
        float bottomPro = bottom / cutHeight;

        //得到裁剪位置
        int cropWidth = (int) (mVideoWidth * (rightPro - leftPro));
        int cropHeight = (int) (mVideoHeight * (bottomPro - topPro));
        int x = (int) (leftPro * mVideoWidth);
        int y = (int) (topPro * mVideoHeight);
        Rect rect = new Rect(x, y, x + cropWidth, y + cropHeight);
        cropParam.setOutputWidth(cropWidth);
        cropParam.setOutputHeight(cropHeight);
        cropParam.setCropRect(rect);
        cropParam.setStartTime(0);
        cropParam.setEndTime(mVideoDuration * 1000);
        }else {
            cropParam.setOutputWidth(mVideoWidth);
            cropParam.setOutputHeight(mVideoHeight);
            cropParam.setStartTime(mStartTime*1000);
            cropParam.setEndTime(mEndTime * 1000);
        }
        mAliyunCrop.setCropParam(cropParam);
        mAliyunCrop.setCropCallback(new CropCallback() {
            @Override
            public void onProgress(int i) {
                Log.e(TAG, "onProgress: " + i);
            }

            @Override
            public void onError(int i) {
                Log.e(TAG, "onError: " + i);
                closeProgressDialog();
            }

            @Override
            public void onComplete(long l) {
                closeProgressDialog();
                changeCutState(false);
                changeClipState(false);
                setVideoUrl(cutOutPath);
            }

            @Override
            public void onCancelComplete() {

            }
        });
        mAliyunCrop.startCrop();
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.e(TAG, "dispatchKeyEvent: =============" + rl_expression.isShown());
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_BACK:
                // 处理自己的逻辑break;
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (rl_expression.isShown()) {
                        rl_expression.setVisibility(GONE);
                        return true;
                    }
                    if (rl_edit_text.isShown()) {
                        rl_edit_text.setVisibility(GONE);
                        return true;
                    }
                    if(rl_cut.isShown()){
                        changeCutState(false);
                        return true;
                    }
                    if (rl_clip.isShown()){
                        changeClipState(false);
                        return true;
                    }
                }
        }
        return super.dispatchKeyEvent(event);
    }

    private void changeTuYaState(boolean flag) {
        if (flag) {
            tuyaView.setDrawMode(flag);
            tuyaView.setNewPaintColor(getResources().getColor(colors[currentColorPosition]));
            iv_tuya.setImageResource(R.mipmap.tuya_pen_click);
            llTuYAColor.setVisibility(View.VISIBLE);
            v_line.setVisibility(View.VISIBLE);
        } else {
            tuyaView.setDrawMode(flag);
            llTuYAColor.setVisibility(View.GONE);
            v_line.setVisibility(View.GONE);
            iv_tuya.setImageResource(R.mipmap.tuya_pen);
        }
    }

    /**
     * 更改文字输入状态的界面
     */
    private void changeTextState(boolean flag) {
        if (flag) {
            popupEditText();
            rl_edit_text.setVisibility(VISIBLE);
        } else {
            rl_edit_text.setVisibility(GONE);
            manager.hideSoftInputFromWindow(et_tag.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }


    /**
     * 更改裁剪大小的界面
     */
    private void changeCutState(boolean flag) {
        if (flag) {
            enterCutAnima();
        } else {
            rl_cut.setVisibility(GONE);
            tv_cancel_edit.setVisibility(VISIBLE);
            tv_complete_edit.setVisibility(VISIBLE);
            layout_control_tab.setVisibility(VISIBLE);
            exitCutAnima();
        }
    }

    private void changeClipState(boolean flag) {
        if (flag) {
            rl_clip.setVisibility(VISIBLE);
            rl_clip.setFocusable(true);
            rl_clip.setFocusableInTouchMode(true);
            rl_clip.requestFocus();
            tv_cancel_edit.setVisibility(GONE);
            tv_complete_edit.setVisibility(GONE);
            layout_control_tab.setVisibility(GONE);
            post(mRunnable);
            tv_video_time.setText((float) (mEndTime - mStartTime) / 1000 + "s");
        } else {
            rl_clip.setVisibility(GONE);
            tv_cancel_edit.setVisibility(VISIBLE);
            tv_complete_edit.setVisibility(VISIBLE);
            layout_control_tab.setVisibility(VISIBLE);
            removeCallbacks(mRunnable);
        }
    }

    private void enterCutAnima() {
        int width = mVideoPlayView.getWidth();
        float[] cutMarginArr = cutView.getMargin();
        float scale = (width - cutMarginArr[0] - cutMarginArr[2]) / width;
        ViewCompat.animate(mVideoPlayView).setListener(new ViewPropertyAnimatorListener() {
            @Override
            public void onAnimationStart(View view) {

            }

            @Override
            public void onAnimationEnd(View view) {
                rl_cut.setVisibility(VISIBLE);
                rl_cut.setFocusable(true);
                rl_cut.setFocusableInTouchMode(true);
                rl_cut.requestFocus();
                tv_cancel_edit.setVisibility(GONE);
                tv_complete_edit.setVisibility(GONE);
                layout_control_tab.setVisibility(GONE);
            }

            @Override
            public void onAnimationCancel(View view) {

            }
        }).scaleY(scale).scaleX(scale).translationY((cutMarginArr[1] - cutMarginArr[3]) / 2).setDuration(300).start();
    }

    private void exitCutAnima() {
        ViewCompat.animate(mVideoPlayView).setListener(new ViewPropertyAnimatorListener() {
            @Override
            public void onAnimationStart(View view) {

            }

            @Override
            public void onAnimationEnd(View view) {

            }

            @Override
            public void onAnimationCancel(View view) {

            }
        }).scaleY(1).scaleX(1).translationY(0).setDuration(300).start();
    }

    /**
     * 添加文字到界面上
     */
    private void addTextToWindow() {

        TouchView touchView = new TouchView(getContext());
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(tv_tag.getWidth(), tv_tag.getHeight());
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        touchView.setLayoutParams(layoutParams);
        Bitmap bitmap = Bitmap.createBitmap(tv_tag.getWidth(), tv_tag.getHeight(), Bitmap.Config.ARGB_8888);
        tv_tag.draw(new Canvas(bitmap));
        ViewCompat.setBackground(touchView, new BitmapDrawable(bitmap));
        touchView.setLimitsX(0, getWidth());
        touchView.setLimitsY(0, getHeight() - DensityUtil.dip2px(getContext(), 100) / 2);
        touchView.setOnLimitsListener(new TouchView.OnLimitsListener() {
            @Override
            public void OnOutLimits(float x, float y) {
                tv_hint_delete.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.mipmap.icon_delete_red), null, null);
                tv_hint_delete.setTextColor(Color.RED);
            }

            @Override
            public void OnInnerLimits(float x, float y) {
                tv_hint_delete.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.mipmap.icon_delete), null, null);
                tv_hint_delete.setTextColor(Color.WHITE);
            }
        });
        touchView.setOnTouchListener(new TouchView.OnTouchListener() {
            @Override
            public void onDown(TouchView view, MotionEvent event) {
                tv_hint_delete.setVisibility(View.VISIBLE);
                changeMode(false);
            }

            @Override
            public void onMove(TouchView view, MotionEvent event) {

            }

            @Override
            public void onUp(TouchView view, MotionEvent event) {
                tv_hint_delete.setVisibility(View.GONE);
                changeMode(true);
                if (view.isOutLimits()) {
                    rl_touch_view.removeView(view);
                }
            }
        });

        rl_touch_view.addView(touchView);

        et_tag.setText("");
        tv_tag.setText("");
    }


    /**
     * 添加表情到界面上
     */
    private void addExpressionToWindow(int result) {

        TouchView touchView = new TouchView(getContext());
        touchView.setBackgroundResource(result);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(DensityUtil.dip2px(getContext(), 100), DensityUtil.dip2px(getContext(), 100));
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        touchView.setLayoutParams(layoutParams);

        touchView.setLimitsX(0, getWidth());
        touchView.setLimitsY(0, getHeight() - DensityUtil.dip2px(getContext(), 100) / 2);
        touchView.setOnLimitsListener(new TouchView.OnLimitsListener() {
            @Override
            public void OnOutLimits(float x, float y) {
                tv_hint_delete.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.mipmap.icon_delete_red), null, null);
                tv_hint_delete.setTextColor(Color.RED);
            }

            @Override
            public void OnInnerLimits(float x, float y) {
                tv_hint_delete.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.mipmap.icon_delete), null, null);
                tv_hint_delete.setTextColor(Color.WHITE);
            }
        });
        touchView.setOnTouchListener(new TouchView.OnTouchListener() {
            @Override
            public void onDown(TouchView view, MotionEvent event) {
                tv_hint_delete.setVisibility(View.VISIBLE);
                changeMode(false);
            }

            @Override
            public void onMove(TouchView view, MotionEvent event) {

            }

            @Override
            public void onUp(TouchView view, MotionEvent event) {
                tv_hint_delete.setVisibility(View.GONE);
                changeMode(true);
                if (view.isOutLimits()) {
                    rl_touch_view.removeView(view);
                }
            }
        });

        rl_touch_view.addView(touchView);
    }

    /**
     * 弹出键盘
     */
    public void popupEditText() {
        isFirstShowEditText = true;
        et_tag.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (isFirstShowEditText) {
                    isFirstShowEditText = false;
                    et_tag.setFocusable(true);
                    et_tag.setFocusableInTouchMode(true);
                    et_tag.requestFocus();
                    isFirstShowEditText = !manager.showSoftInput(et_tag, 0);
                }
            }
        });
    }

    //更改界面模式
    private void changeMode(boolean flag) {
        if (flag) {
            tv_cancel_edit.setVisibility(View.VISIBLE);
            tv_complete_edit.setVisibility(View.VISIBLE);
            layout_control_tab.setVisibility(View.VISIBLE);
        } else {
            tv_cancel_edit.setVisibility(View.GONE);
            tv_complete_edit.setVisibility(View.GONE);
            layout_control_tab.setVisibility(View.GONE);
        }
    }

    /**
     * 初始化底部颜色选择器
     * 两个颜色选择器，一个涂鸦颜色，一个字体颜色
     */
    private void initColors() {
        int dp20 = (int) getResources().getDimension(R.dimen.dp20);
        int dp25 = (int) getResources().getDimension(R.dimen.dp25);
        for (int x = 0; x < drawableBg.length; x++) {
            RelativeLayout relativeLayoutTuYa = new RelativeLayout(getContext());
            RelativeLayout relativeLayoutText = new RelativeLayout(getContext());
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);
            layoutParams.weight = 1;
            relativeLayoutTuYa.setLayoutParams(layoutParams);
            relativeLayoutText.setLayoutParams(layoutParams);

            View viewTuYa = new View(getContext());
            View viewText = new View(getContext());
            viewTuYa.setBackgroundDrawable(getResources().getDrawable(drawableBg[x]));
            viewText.setBackgroundDrawable(getResources().getDrawable(drawableBg[x]));
            RelativeLayout.LayoutParams layoutParams1 = new RelativeLayout.LayoutParams(dp20, dp20);
            layoutParams1.addRule(RelativeLayout.CENTER_IN_PARENT);
            viewTuYa.setLayoutParams(layoutParams1);
            viewText.setLayoutParams(layoutParams1);
            relativeLayoutTuYa.addView(viewTuYa);
            relativeLayoutText.addView(viewText);

            final View viewTuYa2 = new View(getContext());
            final View viewText2 = new View(getContext());
            viewTuYa2.setBackgroundResource(R.mipmap.color_click);
            viewText2.setBackgroundResource(R.mipmap.color_click);
            RelativeLayout.LayoutParams layoutParams2 = new RelativeLayout.LayoutParams(dp25, dp25);
            layoutParams2.addRule(RelativeLayout.CENTER_IN_PARENT);
            viewTuYa2.setLayoutParams(layoutParams2);
            viewText2.setLayoutParams(layoutParams2);
            if (x != 0) {
                viewTuYa2.setVisibility(View.GONE);
                viewText2.setVisibility(View.GONE);
            }
            relativeLayoutTuYa.addView(viewTuYa2);
            relativeLayoutText.addView(viewText2);

            final int position = x;
            relativeLayoutTuYa.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentColorPosition != position) {
                        viewTuYa2.setVisibility(View.VISIBLE);
                        ViewGroup parent = (ViewGroup) v.getParent();
                        ViewGroup childView = (ViewGroup) parent.getChildAt(currentColorPosition);
                        childView.getChildAt(1).setVisibility(View.GONE);
                        tuyaView.setNewPaintColor(getResources().getColor(colors[position]));
                        currentColorPosition = position;
                    }
                }
            });
            relativeLayoutText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isTextColor) {
                        if (currentTextColorPosition != position) {
                            viewText2.setVisibility(View.VISIBLE);
                            ViewGroup parent = (ViewGroup) v.getParent();
                            ViewGroup childView = (ViewGroup) parent.getChildAt(currentTextColorPosition);
                            childView.getChildAt(1).setVisibility(View.GONE);
                            et_tag.setTextColor(getResources().getColor(colors[position]));
                            currentTextColorPosition = position;
                        }
                    } else {
                        if (currentTextBgColorPosition != position) {
                            viewText2.setVisibility(View.VISIBLE);
                            ViewGroup parent = (ViewGroup) v.getParent();
                            if (currentTextBgColorPosition >= 0) {
                                ViewGroup childView = (ViewGroup) parent.getChildAt(currentTextBgColorPosition);
                                childView.getChildAt(1).setVisibility(View.GONE);
                            }
                            et_tag.getPaint().bgColor = getResources().getColor(colors[position]);
                            et_tag.setTextColor(Color.TRANSPARENT);
                            et_tag.setTextColor(getResources().getColor(colors[currentTextColorPosition]));
                            currentTextBgColorPosition = position;
                        }
                    }
                }
            });

            llTuYAColor.addView(relativeLayoutTuYa, x);
            ll_text_color.addView(relativeLayoutText, x);
        }
    }

    /**
     * 初始化表情
     */
    private void initExpression() {
        if (rl_expression.getChildCount() > 0) {
            return;
        }
        int dp80 = (int) getResources().getDimension(R.dimen.dp80);
        int dp10 = (int) getResources().getDimension(R.dimen.dp10);
        for (int x = 0; x < expressions.length; x++) {
            ImageView imageView = new ImageView(getContext());
            imageView.setPadding(dp10, dp10, dp10, dp10);
            final int result = expressions[x];
            imageView.setImageResource(result);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(getWidth() / 4, dp80));
            imageView.setX(x % 4 * getWidth() / 4);
            imageView.setY(x / 4 * dp80);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    rl_expression.setVisibility(View.GONE);
                    addExpressionToWindow(result);
                }
            });
            rl_expression.addView(imageView);
        }
    }

    public void setCallback(EditVideoViewCallback callback) {
        mCallback = callback;
    }

    public void setOutFileDir(String outFileDir) {
        this.outFileDir = outFileDir;
    }

    public void setVideoUrl(final String videoUrl) {
        this.videoUrl = videoUrl;
        mVideoPlayView.setDataSource(videoUrl);
        getBackgroundHandler().post(new Runnable() {
            @Override
            public void run() {
                AliyunIImport aliyunIImport = AliyunImportCreator.getImportInstance(getContext());
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mVideoDuration = 0;
                mVideoWidth = 0;
                mVideoHeight = 0;
                try {
                    mmr.setDataSource(videoUrl);
                    aliyunIImport.setVideoParam(mVideoParam);
                    mVideoWidth = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                    mVideoHeight = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                    mVideoDuration = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                } catch (Exception e) {
                    Log.e(AliyunTag.TAG, "video invalid, return");
                    return;
                }
                mmr.release();
                if (mVideoHeight > 1280 || mVideoWidth > 1280) {
                    if (mVideoWidth >= mVideoHeight) {
                        mVideoParam.setOutputHeight((int) (mVideoHeight / ((float) mVideoWidth / 1280)));
                        mVideoParam.setOutputWidth(1280);
                    } else {
                        mVideoParam.setOutputWidth((int) (mVideoWidth / ((float) mVideoWidth / 1280)));
                        mVideoParam.setOutputHeight(1280);
                    }
                } else {
                    mVideoParam.setOutputHeight(mVideoHeight);
                    mVideoParam.setOutputWidth(mVideoWidth);
                }
                aliyunIImport.addVideo(videoUrl, 0, mVideoDuration, null, AliyunDisplayMode.DEFAULT);
                Uri projectUri = Uri.fromFile(new File(aliyunIImport.generateProjectConfigure()));
                mAliyunEditor = AliyunEditorFactory.creatAliyunEditor(projectUri, new SampleEditorCallBack());
                mAliyunEditor.init(null, getContext());

                kFrame = new FrameExtractor10();
                kFrame.setDataSource(videoUrl);
                mStartTime = 0;
                mEndTime = mVideoDuration;
                post(new Runnable() {
                    @Override
                    public void run() {
                        int minDiff = (int) (2000 / (float) mVideoDuration * 100) + 1;
                        seek_bar.setProgressMinDiff(minDiff > 100 ? 100 : minDiff);
                        adapter = new VideoTrimAdapter(getContext(), mVideoDuration, kFrame, seek_bar);
                        video_tailor_image_list.setAdapter(adapter);
                    }
                });
            }
        });
    }

    public interface EditVideoViewCallback {
        void cancelEdit();

        void completeEdit(String outPath);

        void editProgress(int progress);

        void editError();

    }

}
