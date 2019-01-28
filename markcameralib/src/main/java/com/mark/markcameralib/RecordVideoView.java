package com.mark.markcameralib;

import android.Manifest;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.aliyun.common.global.AliyunTag;
import com.aliyun.common.utils.CommonUtil;
import com.aliyun.recorder.AliyunRecorderCreator;
import com.aliyun.recorder.supply.AliyunIClipManager;
import com.aliyun.recorder.supply.AliyunIRecorder;
import com.aliyun.recorder.supply.EncoderInfoCallback;
import com.aliyun.recorder.supply.RecordCallback;
import com.aliyun.svideo.sdk.external.struct.common.VideoQuality;
import com.aliyun.svideo.sdk.external.struct.encoder.EncoderInfo;
import com.aliyun.svideo.sdk.external.struct.encoder.VideoCodecs;
import com.aliyun.svideo.sdk.external.struct.recorder.CameraParam;
import com.aliyun.svideo.sdk.external.struct.recorder.CameraType;
import com.aliyun.svideo.sdk.external.struct.recorder.MediaInfo;
import com.aliyun.svideo.sdk.external.struct.snap.AliyunSnapVideoParam;
import com.mark.markcameralib.common.utils.OrientationDetector;
import com.mark.markcameralib.common.utils.ToastUtils;
import com.mark.markcameralib.view.VideoPlayView;
import com.qu.preview.callback.OnFrameCallBack;
import com.qu.preview.callback.OnTextureIdCallBack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * <pre>
 *     author : Mark
 *     e-mail : makun.cai@aorise.org
 *     time   : 2018/09/19
 *     desc   : TODO
 *     version: 1.0
 * </pre>
 */
public class RecordVideoView extends FrameLayout {

    private static final String TAG = RecordVideoView.class.getSimpleName();
    private Context mContext;
    private GLSurfaceView mGLSurfaceView;
    private VideoPlayView mVideoPlayView;
    private FrameLayout showPicturrLayout;
    private ImageView ivShowPicture;
    private CaptureButton mCaptureButton;
    private ImageView ivSwapCamera;
    private RecordVideoListener mRecordVideoListener;

    private AliyunIRecorder recorder;
    private AliyunIClipManager clipManager;
    private com.aliyun.svideo.sdk.external.struct.recorder.CameraType cameraType
            = CameraType.BACK;

    //最小录制时长
    private static final int MIN_RECORD_TIME = 1;
    //最大录制时长
    private static final int MAX_RECORD_TIME = 10;
    //录制视频是否达到最大值
    private boolean isMaxDuration = false;
    private long startRecordingTime;
    private String mRecordFileDir = Constants.TEMP_PATH;
    private String mRecordFilePath;
    private String mPictureFilePath;

    //录制码率
    private int mBitrate = 0;
    //关键帧间隔
    private int mGop = 5;
    //视频质量
    private VideoQuality mVideoQuality = VideoQuality.HD;
    //视频比例
    private int mRatioMode = AliyunSnapVideoParam.RATIO_MODE_9_16;
    //编码方式
    private VideoCodecs mVideoCodec = VideoCodecs.H264_HARDWARE;

    //视频分辨率
    private int mResolutionMode = AliyunSnapVideoParam.RESOLUTION_720P;

    private static Handler mBackgroundHandler;
    private boolean isBrowse;
    private boolean isCancelRecord = false;
    private boolean isRecordSuccess = false;

    private int mMode;
    public static final int TAKE_PHOTO = 1;
    public static final int TAKE_RECORD = 2;
    public static final int TAKE_PHOTO_RECORD = 3;


    /**
     * 权限申请
     */
    String[] permission = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private OrientationDetector orientationDetector;
    private int rotation;
    private byte[] frameBytes;
    private int frameWidth;
    private int frameHeight;
    private boolean isOpenFailed;


    public RecordVideoView(@NonNull Context context) {
        this(context, null);
    }

    public RecordVideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecordVideoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        // 初始化各项组件
        FrameLayout.inflate(context, R.layout.layout_record_video_view, this);
        mGLSurfaceView = findViewById(R.id.glSurfaceView);
        mVideoPlayView = findViewById(R.id.videoPlayView);
        mCaptureButton = findViewById(R.id.captureButton);
        ivShowPicture = findViewById(R.id.iv_ShowPicture);
        showPicturrLayout = findViewById(R.id.iv_ShowPictureLayout);
        ivSwapCamera = findViewById(R.id.swap_camera);
        mCaptureButton.setCaptureListener(mCaptureListener);
        ivSwapCamera.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                swapCamera();
            }
        });
        // Attributes
//        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RecordVideoView, defStyleAttr, 0);
//        a.recycle();
        initVideoView();
    }

    private void initVideoView() {
        //初始化surfaceView

        initSurfaceView();
        initVideoPlayView();
//        initCountDownView();
//        initBeautyParam();
        initRecorder();
//        initRecordTimeView();
//        copyAssets();
//        initFaceUnity(getContext());
    }

    /**
     * 初始化surfaceView
     */
    private void initSurfaceView() {
        final GestureDetector gestureDetector = new GestureDetector(getContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapUp(MotionEvent e) {
                        float x = e.getX() / mGLSurfaceView.getWidth();
                        float y = e.getY() / mGLSurfaceView.getHeight();
                        recorder.setFocus(x, y);
                        return true;
                    }
                });
        mGLSurfaceView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });
    }

    private void initVideoPlayView() {
        mVideoPlayView.setOnStateChangeListener(new VideoPlayView.OnStateChangeListener() {
            @Override
            public void onSurfaceTextureDestroyed(SurfaceTexture surface) {

            }

            @Override
            public void onBuffering() {

            }

            @Override
            public void onPlaying() {
                mGLSurfaceView.setVisibility(GONE);
                showPicturrLayout.setVisibility(GONE);
            }

            @Override
            public void onSeek(int max, int progress) {

            }

            @Override
            public void onStop() {

            }

            @Override
            public void onPause() {

            }

            @Override
            public void onTextureViewAvaliable() {

            }

            @Override
            public void onVideoSizeChanged(int width, int height) {

            }

            @Override
            public void playFinish() {

            }

            @Override
            public void onPrepare() {

            }
        });
    }


    private void initRecorder() {
        recorder = AliyunRecorderCreator.getRecorderInstance(getContext());
        recorder.setDisplayView(mGLSurfaceView);
        clipManager = recorder.getClipManager();
        clipManager.setMinDuration(MIN_RECORD_TIME);
        //mediaInfo.setHWAutoSize(true);//硬编时自适应宽高为16的倍数
        setGop(mGop);
        setBitrate(mBitrate);
        setResolutionMode(mResolutionMode);
        setRatioMode(mRatioMode);
        setVideoQuality(mVideoQuality);
        setVideoCodec(mVideoCodec);
        cameraType = recorder.getCameraCount() == 1 ? com.aliyun.svideo.sdk.external.struct.recorder.CameraType.BACK
                : cameraType;
        recorder.setCamera(cameraType);
        recorder.setBeautyStatus(false);
        initOritationDetector();
        recorder.setFocusMode(CameraParam.FOCUS_MODE_AUTO);
        recorder.setOnFrameCallback(new OnFrameCallBack() {
            @Override
            public void onFrameBack(byte[] bytes, int width, int height, Camera.CameraInfo info) {
                //原始数据回调 NV21,这里获取原始数据主要是为了faceUnity高级美颜使用
                frameBytes = bytes;
                frameWidth = width;
                frameHeight = height;
            }

            @Override
            public Camera.Size onChoosePreviewSize(List<Camera.Size> supportedPreviewSizes,
                                                   Camera.Size preferredPreviewSizeForVideo) {

                return null;
            }

            @Override
            public void openFailed() {
                Log.e(AliyunTag.TAG, "openFailed----------");
                isOpenFailed = true;
            }
        });

        recorder.setRecordCallback(new RecordCallback() {
            @Override
            public void onComplete(final boolean validClip, final long clipDuration) {
                Log.e(TAG, "onComplete: ");
                if (isCancelRecord) {
                    isCancelRecord = false;
                    return;
                }
                post(new Runnable() {
                    @Override
                    public void run() {
                        if (clipManager != null) {
                            if (clipManager.getPartCount() > 0) {
                                //多段视频录制的话需要合成，单段不需要合成
                                mRecordFilePath = clipManager.getVideoPathList().get(0);
                                mVideoPlayView.setVisibility(VISIBLE);
                                mVideoPlayView.setDataSource(mRecordFilePath);
                            }
                        }

                    }
                });

            }

            /**
             * 合成完毕的回调
             * @param outputPath
             */
            @Override
            public void onFinish(final String outputPath) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "run: " + outputPath);
                        mRecordFilePath = outputPath;
                        mGLSurfaceView.setVisibility(GONE);
                        mVideoPlayView.setVisibility(VISIBLE);
                    }
                });

            }

            @Override
            public void onProgress(final long duration) {

            }

            @Override
            public void onMaxDuration() {
//                isMaxDuration = true;
            }

            @Override
            public void onError(int errorCode) {
                mCaptureButton.reStar();
            }

            @Override
            public void onInitReady() {
                Log.e(TAG, "onInitReady");
                post(new Runnable() {
                    @Override
                    public void run() {
//                        restoreConflictEffect();
//                        if (effectPaster != null) {
//                            addEffectToRecord(effectPaster.getPath());
//                        }
                    }
                });
            }

            @Override
            public void onDrawReady() {

            }

            @Override
            public void onPictureBack(final Bitmap bitmap) {
                if (isRecordSuccess){
                    post(new Runnable() {
                        @Override
                        public void run() {
                            showPicturrLayout.setVisibility(VISIBLE);
                            ivShowPicture.setImageBitmap(bitmap);
                        }
                    });
                    return;
                }
                if (rotation != 90 && bitmap != null) {
                    Matrix m = new Matrix();
                    m.setRotate((rotation - 90) % 360, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
                    final Bitmap mPictureBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                    savePicture(mPictureBitmap);
                    post(new Runnable() {
                        @Override
                        public void run() {
                            showPicturrLayout.setVisibility(VISIBLE);
                            ivShowPicture.setImageBitmap(mPictureBitmap);
                        }
                    });
                } else {
                    savePicture(bitmap);
                    post(new Runnable() {
                        @Override
                        public void run() {
                            showPicturrLayout.setVisibility(VISIBLE);
                            ivShowPicture.setImageBitmap(bitmap);
                        }
                    });
                }
                Log.e(TAG, "onPictureBack: ");
            }

            @Override
            public void onPictureDataBack(final byte[] data) {
                Log.e(TAG, "onPictureDataBack: ");
            }

        });
        recorder.setOnTextureIdCallback(new OnTextureIdCallBack() {
            @Override
            public int onTextureIdBack(int textureId, int textureWidth, int textureHeight, float[] matrix) {
//                if (isUseFaceUnity && faceInitResult) {
//                    /**
//                     * faceInitResult fix bug:反复退出进入会出现黑屏情况,原因是因为release之后还在调用渲染的接口,必须要保证release了之后不能再调用渲染接口
//                     */
//                    return faceUnityManager.draw(frameBytes, mFuImgNV21Bytes, textureId, frameWidth, frameHeight, mFrameId++, mControlView.getCameraType().getType());
//                }
                return textureId;
            }

            @Override
            public int onScaledIdBack(int scaledId, int textureWidth, int textureHeight, float[] matrix) {
                return scaledId;
            }
        });

        recorder.setEncoderInfoCallback(new EncoderInfoCallback() {
            @Override
            public void onEncoderInfoBack(EncoderInfo info) {
            }
        });
    }

    private void initOritationDetector() {
        orientationDetector = new OrientationDetector(getContext().getApplicationContext());
        orientationDetector.setOrientationChangedListener(new OrientationDetector.OrientationChangedListener() {
            @Override
            public void onOrientationChanged() {
                rotation = getPictureRotation();
                recorder.setRotation(rotation);
            }
        });
    }

    private int getPictureRotation() {
        int orientation = orientationDetector.getOrientation();
        int rotation = 90;
        if ((orientation >= 45) && (orientation < 135)) {
            rotation = 180;
        }
        if ((orientation >= 135) && (orientation < 225)) {
            rotation = 270;
        }
        if ((orientation >= 225) && (orientation < 315)) {
            rotation = 0;
        }
        if (cameraType == com.aliyun.svideo.sdk.external.struct.recorder.CameraType.FRONT) {
            if (rotation != 0) {
                rotation = 360 - rotation;
            }
        }
        return rotation;
    }

    public void swapCamera() {

    }

    public void setRecordVideoListener(RecordVideoListener recordVideoListener) {
        mRecordVideoListener = recordVideoListener;
    }


    public static Handler getBackgroundHandler() {
        if (mBackgroundHandler == null) {
            HandlerThread thread = new HandlerThread("background");
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }
        return mBackgroundHandler;
    }


    private void browseRecord(File file) {
    }

    private CaptureButton.CaptureListener mCaptureListener = new CaptureButton.CaptureListener() {
        @Override
        public void capture() {
            isBrowse = true;
            recorder.takePhoto(true);
            getBackgroundHandler().postAtFrontOfQueue(new Runnable() {
                @Override
                public void run() {
                    deleteFile();
                    getBackgroundHandler().removeCallbacksAndMessages(null);
                }
            });
        }

        @Override
        public void cancel() {
            Log.e(TAG, "cancel: ");
            isBrowse = false;
            showPicturrLayout.setVisibility(GONE);
            getBackgroundHandler().postAtFrontOfQueue(new Runnable() {
                @Override
                public void run() {
                    if (!TextUtils.isEmpty(mPictureFilePath)) {
                        File file = new File(mPictureFilePath);
                        if (file != null && file.exists()) {
                            file.delete();
                        }
                        mPictureFilePath = null;
                    }
                    getBackgroundHandler().removeCallbacksAndMessages(null);
                }
            });
        }

        @Override
        public void determine() {
            if (mRecordVideoListener != null && !TextUtils.isEmpty(mPictureFilePath)) {
                mRecordVideoListener.onPictureTaken(new File(mPictureFilePath));
            }
        }

        @Override
        public void quit() {
            if (mRecordVideoListener != null) {
                mRecordVideoListener.quit();
            }
        }

        @Override
        public void record() {
            isRecordSuccess = false;
            startRecord();
        }

        @Override
        public void rencordEnd() {
            isRecordSuccess = true;
            recorder.takePhoto(true);
            isBrowse = true;
            stopRecord();
        }

        @Override
        public void rencordFail() {
            Log.e(TAG, "rencordFail: ");
            //时间过短，录制失败，主动取消
            isCancelRecord = true;
            if (recorder != null) {
                recorder.cancelRecording();
            }
            getBackgroundHandler().post(new Runnable() {
                @Override
                public void run() {
                    deleteFile();
                    getBackgroundHandler().removeCallbacksAndMessages(null);
                }
            });
        }

        @Override
        public void getRecordResult() {
            if (mRecordVideoListener != null) {
                mRecordVideoListener.onRecordTaken(new File(mRecordFilePath));
            }
        }

        @Override
        public void deleteRecordResult() {
            Log.e(TAG, "deleteRecordResult: ");
            isBrowse = false;
            mGLSurfaceView.setVisibility(VISIBLE);
            mVideoPlayView.stop();
            mVideoPlayView.setVisibility(GONE);
            if (recorder != null) {
                recorder.startPreview();
            }
            getBackgroundHandler().post(new Runnable() {
                @Override
                public void run() {
                    deleteFile();
                }
            });
        }

        @Override
        public void scale(float scaleValue) {

        }
    };


    /**
     * 开始录制
     */
    private void startRecord() {
        if (CommonUtil.SDFreeSize() < 50 * 1000 * 1000) {
            ToastUtils.show(getContext(), getResources().getString(R.string.aliyun_no_free_memory));
            return;
        }
        if (isMaxDuration) {
            return;
        }
        if (recorder != null) {
            isStopToCompleteDuration = false;
            mRecordFilePath = mRecordFileDir
                    + File.separator + System.currentTimeMillis() + ".mp4";
            File file = new File(mRecordFileDir);
            if (!file.exists()) {
                file.mkdirs();
            }
            Log.e(TAG, "startRecord: ");
            recorder.setOutputPath(mRecordFilePath);
            recorder.startRecording();
            //记录开始录制的时间
            startRecordingTime = System.currentTimeMillis();
//                if (effectMv != null && !TextUtils.isEmpty(effectMv.getPath())) {
//                    if (recorder.getClipManager().getPartCount() == 0) {
//                        recorder.restartMv();
//                    } else {
//                        recorder.resumeMv();
//                    }
//
//                }
//                if (mControlView.getFlashType() == FlashType.ON
//                        && mControlView.getCameraType() == CameraType.BACK) {
//                    recorder.setLight(com.aliyun.svideo.sdk.external.struct.recorder.FlashType.TORCH);
//                }
        }
    }

    /**
     * 视频是是否正正在已经调用stopRecord到onComplete回调过程中这段时间，这段时间不可再次调用stopRecord
     */
    private boolean isStopToCompleteDuration;

    /**
     * 停止录制
     */
    private void stopRecord() {
        if (recorder != null && !isStopToCompleteDuration) {//
            isStopToCompleteDuration = true;
//            if (mControlView.getFlashType() == FlashType.ON
//                    && mControlView.getCameraType() == CameraType.BACK) {
//                recorder.setLight(com.aliyun.svideo.sdk.external.struct.recorder.FlashType.OFF);
//            }
//            //此处添加判断，progressBar弹出，也即当视频片段合成的时候，不调用stopRecording,
//            //否则在finishRecording的时候调用stopRecording，会导致finishRecording阻塞
//            //暂时规避，等待sdk解决该问题，取消该判断
//            if ((progressBar == null || !progressBar.isShowing()) ) {
            recorder.stopRecording();
//
//            }
//            if (effectMv != null && !TextUtils.isEmpty(effectMv.getPath())) {
//                recorder.pauseMv();
//            }

        }
    }

    private void savePicture(Bitmap bitmap) {
        File pictureFile = createPictureDir();
        if (bitmap != null) {
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(pictureFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        mPictureFilePath = pictureFile.getAbsolutePath();
    }

    private void deleteFile() {
        Log.e(TAG, "deleteFile: ");
        if (TextUtils.isEmpty(mRecordFilePath)) {
            return;
        }
        File file = new File(mRecordFilePath);
        if (file != null && file.exists()) {
            file.delete();
        }
        if (clipManager != null) {
            clipManager.deleteAllPart();
        }
    }

    public void start() {
        if (recorder != null) {
            recorder.startPreview();
        }
        if (isBrowse && !TextUtils.isEmpty(mRecordFilePath) && mVideoPlayView != null) {
            mVideoPlayView.start();
        }
        if (orientationDetector != null && orientationDetector.canDetectOrientation()) {
            orientationDetector.enable();
        }
    }

    public void stop() {
        if (recorder != null) {
            recorder.stopPreview();
        }
        if (isBrowse && !TextUtils.isEmpty(mRecordFilePath) && mVideoPlayView != null) {
            mVideoPlayView.pause();
        }
        if (orientationDetector != null) {
            orientationDetector.disable();
        }
    }

    public void onDestroy() {
        if (recorder != null) {
            recorder.destroy();
            recorder = null;
        }

        if (mVideoPlayView != null) {
            mVideoPlayView.destroy();
            mVideoPlayView = null;
        }

        if (orientationDetector != null) {
            orientationDetector.setOrientationChangedListener(null);
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

    @Mode
    public int getMode() {
        return mMode;
    }

    public void setMode(@Mode int mode) {
        mMode = mode;
        mCaptureButton.setMode(mode);
    }

    @IntDef({TAKE_PHOTO, TAKE_RECORD, TAKE_PHOTO_RECORD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Mode {
    }

    public void setFacing(int facing) {

    }

    /**
     * Gets the direction that the current camera faces.
     *
     * @return The camera facing.
     */
    public int getFacing() {
        //noinspection WrongConstant
        return 0;
    }


//    public void setAspectRatio(@NonNull AspectRatio ratio) {
//        mCameraView.setAspectRatio(ratio);
//    }
//
//    /**
//     * Gets the current aspect ratio of camera.
//     *
//     * @return The current {@link AspectRatio}. Can be {@code null} if no camera is opened yet.
//     */
//    @Nullable
//    public AspectRatio getAspectRatio() {
//        return mCameraView.getAspectRatio();
//    }

    /**
     * Enables or disables the continuous auto-focus mode. When the current camera doesn't support
     * auto-focus, calling this method will be ignored.
     *
     * @param autoFocus {@code true} to enable continuous auto-focus mode. {@code false} to
     *                  disable it.
     */
    public void setAutoFocus(boolean autoFocus) {
//        mCameraView.setAutoFocus(autoFocus);
    }

    /**
     * Returns whether the continuous auto-focus mode is enabled.
     *
     * @return {@code true} if the continuous auto-focus mode is enabled. {@code false} if it is
     * disabled, or if it is not supported by the current camera.
     */
//    public boolean getAutoFocus() {
//        return mCameraView.getAutoFocus();
//    }

//    /**
//     * Sets the flash mode.
//     *
//     * @param flash The desired flash mode.
//     */
//    public void setFlash(@CameraView.Flash int flash) {
//        mCameraView.setFlash(flash);
//    }


//    private File createRecordDir() {
//        File sampleDir = new File(mRecordFileDir);
//        if (!sampleDir.exists()) {
//            sampleDir.mkdirs();
//        }
//        // 创建文件
//        File file;
//        try {
//            file = File.createTempFile("record", ".mp4", sampleDir);
//        } catch (IOException e) {
//            e.printStackTrace();
//            file = new File(mRecordFileDir, "record" + System.currentTimeMillis() + ".mp4");
//            if (!file.exists()) {
//                file.mkdirs();
//            }
//        }
//        mRecordFilePath = file.getAbsolutePath();
//        return file;
//    }
    private File createPictureDir() {
        File sampleDir = new File(mRecordFileDir);
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }
        return new File(mRecordFileDir, System.currentTimeMillis() + ".jpg");
    }

    /**
     * 设置码率
     *
     * @param mBitrate
     */
    public void setBitrate(int mBitrate) {
        this.mBitrate = mBitrate;
        if (recorder != null) {
            recorder.setVideoBitrate(mBitrate);
        }
    }

    /**
     * 设置Gop
     *
     * @param mGop
     */
    public void setGop(int mGop) {
        this.mGop = mGop;
        if (recorder != null) {
            recorder.setGop(mGop);
        }
    }

    /**
     * 设置视频质量
     *
     * @param mVideoQuality
     */
    public void setVideoQuality(VideoQuality mVideoQuality) {
        this.mVideoQuality = mVideoQuality;
        if (recorder != null) {
            recorder.setVideoQuality(mVideoQuality);
        }
    }

    /**
     * 设置视频比例
     *
     * @param mRatioMode
     */
    public void setRatioMode(int mRatioMode) {
        this.mRatioMode = mRatioMode;
        if (recorder != null) {
            recorder.setMediaInfo(getMediaInfo());

        }
        if (mGLSurfaceView != null) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mGLSurfaceView.getLayoutParams();
            int screenWidth = getResources().getDisplayMetrics().widthPixels;

            int height = 0;
            switch (mRatioMode) {
                case AliyunSnapVideoParam.RATIO_MODE_1_1:
                    height = screenWidth;
                    break;
                case AliyunSnapVideoParam.RATIO_MODE_3_4:
                    height = screenWidth * 4 / 3;
                    break;
                case AliyunSnapVideoParam.RATIO_MODE_9_16:
                    height = screenWidth * 16 / 9;
                    break;
                default:
                    height = screenWidth * 16 / 9;
                    break;
            }
            params.height = height;
            mGLSurfaceView.setLayoutParams(params);
            if (mVideoPlayView != null) {
                mVideoPlayView.setLayoutParams(params);
                showPicturrLayout.setLayoutParams(params);
            }
            requestLayout();
        }

    }

    /**
     * 设置视频编码方式
     *
     * @param mVideoCodec
     */
    public void setVideoCodec(VideoCodecs mVideoCodec) {
        this.mVideoCodec = mVideoCodec;
        if (recorder != null) {
            recorder.setMediaInfo(getMediaInfo());
        }

    }

    /**
     * 设置视频码率
     *
     * @param mResolutionMode
     */
    public void setResolutionMode(int mResolutionMode) {
        this.mResolutionMode = mResolutionMode;
        if (recorder != null) {
            recorder.setMediaInfo(getMediaInfo());
        }
    }

    private MediaInfo getMediaInfo() {
        MediaInfo info = new MediaInfo();
        info.setFps(25);
        info.setVideoWidth(getVideoWidth());
        info.setVideoHeight(getVideoHeight());
        info.setVideoCodec(mVideoCodec);
        info.setCrf(0);
        return info;
    }

    /**
     * 获取拍摄视频宽度
     *
     * @return
     */
    private int getVideoWidth() {
        int width = 0;
        switch (mResolutionMode) {
            case AliyunSnapVideoParam.RESOLUTION_360P:
                width = 360;
                break;
            case AliyunSnapVideoParam.RESOLUTION_480P:
                width = 480;
                break;
            case AliyunSnapVideoParam.RESOLUTION_540P:
                width = 540;
                break;
            case AliyunSnapVideoParam.RESOLUTION_720P:
                width = 720;
                break;
            default:
                width = 720;
                break;
        }

        return width;
    }

    private int getVideoHeight() {
        int width = getVideoWidth();
        int height = 0;
        switch (mRatioMode) {
            case AliyunSnapVideoParam.RATIO_MODE_1_1:
                height = width;
                break;
            case AliyunSnapVideoParam.RATIO_MODE_3_4:
                height = width * 4 / 3;
                break;
            case AliyunSnapVideoParam.RATIO_MODE_9_16:
                height = width * 16 / 9;
                break;
            default:
                height = width;
                break;
        }
        return height;
    }

    public interface RecordVideoListener {
        void onRecordTaken(File recordFile);

        void onPictureTaken(File pictureFile);

        void quit();
    }

}

