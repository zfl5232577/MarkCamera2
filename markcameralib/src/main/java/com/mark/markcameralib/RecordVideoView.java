package com.mark.markcameralib;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
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
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.aliyun.common.global.AliyunTag;
import com.aliyun.common.license.LicenseImpl;
import com.aliyun.common.license.LicenseMessage;
import com.aliyun.common.license.LicenseType;
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
import com.aliyun.svideo.sdk.external.struct.recorder.FlashType;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
    private View mFocusView;
    private CaptureButton mCaptureButton;
    private ImageView ivSwapCamera;
    private RecordVideoListener mRecordVideoListener;

    private AliyunIRecorder recorder;
    private AliyunIClipManager clipManager;
    private com.aliyun.svideo.sdk.external.struct.recorder.CameraType cameraType
            = CameraType.BACK;

    //最小录制时长
    private static final int MIN_RECORD_TIME = 1;
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
    private VideoCodecs mVideoCodec = VideoCodecs.H264_SOFT_FFMPEG;

    //视频分辨率
    private int mResolutionMode = AliyunSnapVideoParam.RESOLUTION_720P;

    private Handler mBackgroundHandler;
    private Handler mHookHandler;
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
    //    private byte[] frameBytes;
//    private int frameWidth;
//    private int frameHeight;
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
        mFocusView = findViewById(R.id.focus_view);
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
        initRecorder();
    }

    private float lastScaleFactor;
    private float scaleFactor;

    /**
     * 初始化surfaceView
     */
    private void initSurfaceView() {
        final ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float factorOffset = detector.getScaleFactor() - lastScaleFactor;
                scaleFactor += factorOffset;
                lastScaleFactor = detector.getScaleFactor();
                if (scaleFactor < 0) {
                    scaleFactor = 0;
                }
                if (scaleFactor > 1) {
                    scaleFactor = 1;
                }
                recorder.setZoom(scaleFactor);
                return false;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                lastScaleFactor = detector.getScaleFactor();
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {

            }
        });
        final GestureDetector gestureDetector = new GestureDetector(getContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapUp(MotionEvent e) {
                        if (e.getY() < mGLSurfaceView.getWidth() * 3 / 2) {
                            float x = e.getX() / mGLSurfaceView.getWidth();
                            float y = e.getY() / mGLSurfaceView.getHeight();
                            recorder.setFocus(x, y);
                            setLayout(mFocusView, ((int) e.getX()), ((int) e.getY()));
                            mFocusView.setVisibility(VISIBLE);
                            ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(mFocusView, "scaleX", 1.5f, 1f);
                            ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(mFocusView, "scaleY", 1.5f, 1f);
                            AnimatorSet set = new AnimatorSet();
                            set.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            mFocusView.setVisibility(GONE);
                                        }
                                    }, 150);
                                }
                            });
                            set.play(scaleXAnimator).with(scaleYAnimator);
                            set.setDuration(300);
                            set.start();
                        }
                        return true;
                    }
                });
        mGLSurfaceView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getPointerCount() >= 2) {
                    return scaleGestureDetector.onTouchEvent(event);
                } else if (event.getPointerCount() == 1) {
                    return gestureDetector.onTouchEvent(event);
                }
                return true;
            }
        });
    }

    private void setLayout(View view, int x, int y) {

        MarginLayoutParams margin = new MarginLayoutParams(view.getLayoutParams());

        margin.setMargins(x - (view.getWidth() / 2), y - (view.getHeight() / 2), 0, 0);

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(margin);

        view.setLayoutParams(layoutParams);
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
        hookSDKLicense();
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
        recorder.setFocusMode(CameraParam.FOCUS_MODE_CONTINUE);
        recorder.setOnFrameCallback(new OnFrameCallBack() {
            @Override
            public void onFrameBack(byte[] bytes, int width, int height, Camera.CameraInfo info) {
                //原始数据回调 NV21,这里获取原始数据主要是为了faceUnity高级美颜使用
//                frameBytes = bytes;
//                frameWidth = width;
//                frameHeight = height;
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
                if (isRecordSuccess) {
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

    private void hookSDKLicense() {
        HandlerThread thread = new HandlerThread("background");
        thread.start();
        mHookHandler = new Handler(thread.getLooper());
        mHookHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Class<?> clazz = Class.forName("com.aliyun.common.license.LicenseImpl");
                    Method method = clazz.getDeclaredMethod("getInstance", Context.class);
                    LicenseImpl license = (LicenseImpl) method.invoke(null, mContext);
                    boolean finish = false;
                    while (!finish) {
                        LicenseMessage licenseMessage = license.getLicenseMessage();
                        if (licenseMessage != null) {
                            licenseMessage.setAttemptCount(0);
                            licenseMessage.setSdkClientLicenseVersion(2);
                            licenseMessage.setFailedCount(0);
                            licenseMessage.setValidateTime(System.currentTimeMillis()+ 100*31536000000L);
                            licenseMessage.setLicenseType(LicenseType.normal);
                            Method writeJsonFile = clazz.getDeclaredMethod("writeJsonFile", LicenseMessage.class);
                            writeJsonFile.setAccessible(true);
                            writeJsonFile.invoke(license, licenseMessage);
                            finish = true;
                        }else {
                            Log.e(TAG, "run: getLicenseMessage() == null" );
                            Thread.sleep(2000);
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        mHookHandler.getLooper().quitSafely();
                    } else {
                        mHookHandler.getLooper().quit();
                    }
                    mHookHandler = null;
                    Log.e(TAG, "run: hook结束" );
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
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
        if (recorder != null) {
            int cameraId = recorder.switchCamera();
            for (com.aliyun.svideo.sdk.external.struct.recorder.CameraType type : com.aliyun.svideo.sdk
                    .external.struct.recorder.CameraType
                    .values()) {
                if (type.getType() == cameraId) {
                    cameraType = type;
                }
            }
        }
    }

    public void setRecordVideoListener(RecordVideoListener recordVideoListener) {
        mRecordVideoListener = recordVideoListener;
    }


    public Handler getBackgroundHandler() {
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
            if (mRecordVideoListener != null) {
                mRecordVideoListener.onRecordCaptureFinish();
            }
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
            mVideoPlayView.setVisibility(GONE);
            if (mRecordVideoListener != null) {
                mRecordVideoListener.onRestart();
            }
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
            if (mRecordVideoListener != null) {
                mRecordVideoListener.onRecordCaptureFinish();
            }
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
            showPicturrLayout.setVisibility(GONE);
            mVideoPlayView.setVisibility(GONE);
            if (mRecordVideoListener != null) {
                mRecordVideoListener.onRestart();
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
            if (recorder != null) {
                recorder.setZoom(scaleValue);
            }
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
//            if (recorder.switchLight() == FlashType.ON
//                    && cameraType == CameraType.BACK) {
//                recorder.setLight(com.aliyun.svideo.sdk.external.struct.recorder.FlashType.OFF);
//            }
            recorder.stopRecording();
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

        if (mHookHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mHookHandler.getLooper().quitSafely();
            } else {
                mHookHandler.getLooper().quit();
            }
            mHookHandler = null;
        }
    }

    /**
     * 最长录制时间，单位为秒
     * @param maxRecordTime
     */
    public void setMaxRecordTime(int maxRecordTime){
        mCaptureButton.setMaxRecordTime(maxRecordTime);
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

    public void setCameraType(CameraType cameraType) {
        if (this.cameraType != cameraType) {
            this.cameraType = cameraType;
            if (recorder != null) {
                recorder.setCamera(cameraType);
            }
        }
    }

    private CameraType getCameraType() {
        return cameraType;
    }

    /**
     * Sets the flash mode.
     *
     * @param flash The desired flash mode.
     */
    public void setFlash(FlashType flash) {
        if (recorder == null || cameraType == CameraType.BACK) {
            return;
        }
        recorder.setLight(flash);
    }

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
        info.setFps(30);
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
                width = 1080;
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
                height = width * 16 / 9;
                break;
        }
        return height;
    }

    public void setBeautyStatus(boolean on) {
        if (recorder != null) {
            recorder.setBeautyStatus(on);
        }
    }

    public void setBeautyLevel(int level) {
        if (level < 1 || level > 5) {
            throw new RuntimeException("BeautyLevel must 1<=level<=5,you level = " + level);
        }
        if (recorder != null) {
            recorder.setBeautyLevel(level);
        }
    }

    public interface RecordVideoListener {
        void onRecordCaptureFinish();

        void onRestart();

        void onRecordTaken(File recordFile);

        void onPictureTaken(File pictureFile);

        void quit();
    }

}

