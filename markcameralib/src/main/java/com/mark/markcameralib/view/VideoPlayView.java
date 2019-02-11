package com.mark.markcameralib.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;

/**
 * <pre>
 *     author : Mark
 *     e-mail : makun.cai@aorise.org
 *     time   : 2018/07/05
 *     desc   : 视频播放
 *     version: 1.0
 * </pre>
 */
@SuppressLint("NewApi")
public class VideoPlayView extends TextureView implements
        SurfaceTextureListener {

    boolean playFinished = false;//是否播放完毕,在onCompletion中值修改为true，表示播放完毕，不在调用onSeek

    private MediaPlayer mediaPlayer;
    private AudioManager mAudioManager;
    MediaState mediaState;
    private String videoUrl;
    private boolean looping = true;
    private boolean isSetWHFinish;
    private boolean isSurfcacePreparing = false;

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public interface OnStateChangeListener {
        public void onSurfaceTextureDestroyed(SurfaceTexture surface);

        public void onBuffering();

        public void onPlaying();

        public void onSeek(int max, int progress);

        public void onStop();

        public void onPause();

        public void onTextureViewAvaliable();

        public void onVideoSizeChanged(int width, int height);

        public void playFinish();

        public void onPrepare();
    }

    OnStateChangeListener onStateChangeListener;

    public void setOnStateChangeListener(
            OnStateChangeListener onStateChangeListener) {
        this.onStateChangeListener = onStateChangeListener;
    }

    private OnInfoListener onInfoListener = new OnInfoListener() {
        @Override
        public boolean onInfo(MediaPlayer mp, int what, int extra) {
            if (onStateChangeListener != null && mediaState != MediaState.PAUSE) {
                onStateChangeListener.onPlaying();
                if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                    onStateChangeListener.onBuffering();
                } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                    onStateChangeListener.onPlaying();
                }
            }
            return false;
        }
    };

    private OnBufferingUpdateListener bufferingUpdateListener = new OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            if (onStateChangeListener != null) {
                //在某些情况下视频一次性缓冲100%，有些手机竟然不调用OnInfo回调，比如小米2，导致缓冲指示器一直显示，所以在此处添加此代码
                if (percent == 100 && mediaState != MediaState.PAUSE) {
                    mediaState = MediaState.PLAYING;
                    onStateChangeListener.onPlaying();
                }
                if (mediaState == MediaState.PLAYING) {
                    if (playFinished)
                        return;
                    onStateChangeListener.onSeek(mediaPlayer.getDuration(),
                            mediaPlayer.getCurrentPosition());
                }
            }
        }
    };

    public VideoPlayView(Context context) {
        this(context, null);
    }

    public VideoPlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setSurfaceTextureListener(this);
        initMediaPlayer();
    }

    private void initMediaPlayer() {
        mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        mediaPlayer = new MediaPlayer();
        mediaPlayer
                .setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        playFinished = false;
                        mediaPlayer.start();
                        mediaState = MediaState.PLAYING;
                    }
                });
        mediaPlayer.setOnInfoListener(onInfoListener);
        mediaPlayer.setOnBufferingUpdateListener(bufferingUpdateListener);
        mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (onStateChangeListener != null) {
                    if (mediaState != MediaState.PLAYING)
                        return;
                    onStateChangeListener.playFinish();
                    playFinished = true;
                }
            }
        });

        mediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                if (onStateChangeListener != null) {
                    onStateChangeListener.onVideoSizeChanged(width, height);
                }
                if (isSetWHFinish) {
                    return;
                }
                isSetWHFinish = true;
                updateTextureViewSizeCenter(width, height);
            }
        });
        mediaPlayer.setOnErrorListener(new OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                mediaPlayer.reset();
                mediaState = MediaState.INIT;
                if (onStateChangeListener != null) {
                    onStateChangeListener.onStop();
                }
                return false;
            }
        });
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaState = MediaState.INIT;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
                                          int width, int height) {
        System.out.println("onSurfaceTextureAvailable onSurfaceTextureAvailable");
        isSurfcacePreparing = true;
        Surface surface = new Surface(surfaceTexture);
        mediaPlayer.reset();
        mediaPlayer.setSurface(surface);
        if (!TextUtils.isEmpty(videoUrl)) {
            play(videoUrl);
        }
        if (onStateChangeListener != null) {
            onStateChangeListener.onTextureViewAvaliable();
        }
    }

    public long getDuration() {
        if (mediaPlayer != null) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }

    public long getCurrentPos() {
        if (mediaPlayer != null) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public void stop() {
        try {
            if (mAudioManager != null) {
                mAudioManager.abandonAudioFocus(null);
            }
            if (mediaState == MediaState.INIT) {
                return;
            }
            if (mediaState == MediaState.PREPARING) {
                mediaPlayer.reset();
                mediaState = MediaState.INIT;
                return;
            }
            if (mediaState == MediaState.PAUSE) {
                mediaPlayer.stop();
                mediaPlayer.reset();
                mediaState = MediaState.INIT;
                return;
            }
            if (mediaState == MediaState.PLAYING) {
                mediaPlayer.pause();
                mediaPlayer.stop();
                mediaPlayer.reset();
                mediaState = MediaState.INIT;
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (null != mediaPlayer)
                mediaPlayer.reset();
            mediaState = MediaState.INIT;
        } finally {
            if (onStateChangeListener != null) {
                onStateChangeListener.onStop();
            }
        }
    }

    public void destroy() {
        mediaState = MediaState.RELEASE;
        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(null);
            mAudioManager = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }


    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        isSurfcacePreparing = false;
        if (onStateChangeListener != null) {
            onStateChangeListener.onSurfaceTextureDestroyed(surface);
        }
        return false;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
                                            int height) {
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    public void setDataSource(String dataSource) {
        if (TextUtils.isEmpty(dataSource)) {
            return;
        }
        videoUrl = dataSource;
        play(videoUrl);
    }

    public boolean isLooping() {
        return looping;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    private void play(String videoUrl) {
        play(videoUrl, looping);
    }

    private void play(String videoUrl, boolean looping) {
        isSetWHFinish = false;
        if (mediaPlayer == null || !isSurfcacePreparing) {
            return;
        }
        if (mediaState == MediaState.PREPARING) {
            stop();
            return;
        }
        if (mAudioManager != null) {
            mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }
        mediaPlayer.reset();
        mediaPlayer.setLooping(looping);
        try {
            mediaPlayer.setDataSource(videoUrl);
            mediaPlayer.prepareAsync();
            if (onStateChangeListener != null) {
                onStateChangeListener.onPrepare();
            }
            mediaState = MediaState.PREPARING;
        } catch (Exception e) {
            mediaPlayer.reset();
            mediaState = MediaState.INIT;
        }
    }

    public void seekTo(int seekPos){
        if (mediaPlayer == null) {
            return;
        }
        mediaPlayer.seekTo(seekPos);
    }

    public void pause() {
        if (mediaPlayer == null) {
            return;
        }
        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(null);
        }
        mediaPlayer.pause();
        mediaState = MediaState.PAUSE;
        if (onStateChangeListener != null) {
            onStateChangeListener.onPause();
        }
    }

    public void start() {
        if (mediaPlayer == null) {
            return;
        }
        if (mAudioManager != null) {
            mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }
        playFinished = false;
        mediaPlayer.start();
        mediaState = MediaState.PLAYING;
    }

    public enum MediaState {
        INIT, PREPARING, PLAYING, PAUSE, RELEASE;
    }

    public MediaState getState() {
        return mediaState;
    }

    private void updateTextureViewSizeCenter(int width, int height) {

        float sx = (float) getWidth() / (float) width;
        float sy = (float) getHeight() / (float) height;

        Matrix matrix = new Matrix();

        //第1步:把视频区移动到View区,使两者中心点重合.
        matrix.preTranslate((getWidth() - width) / 2, (getHeight() - height) / 2);

        //第2步:因为默认视频是fitXY的形式显示的,所以首先要缩放还原回来.
        matrix.preScale(width / (float) getWidth(), height / (float) getHeight());

        //第3步,等比例放大或缩小,直到视频区的一边和View一边相等.如果另一边和view的一边不相等，则留下空隙
        if (sx >= sy) {
            matrix.postScale(sy, sy, getWidth() / 2, getHeight() / 2);
        } else {
            matrix.postScale(sx, sx, getWidth() / 2, getHeight() / 2);
        }

        setTransform(matrix);
        postInvalidate();
    }

}
