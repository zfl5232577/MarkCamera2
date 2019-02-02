package com.mark.markcameralib;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

public class CaptureButton extends View {

    public final String TAG = "CaptureButtom";

    private Paint mPaint;
    private Context mContext;

    private float btn_center_Y;
    private float btn_center_X;

    private float btn_inside_radius;
    private float btn_outside_radius;
    //before radius
    private float btn_before_inside_radius;
    private float btn_before_outside_radius;
    //after radius
    private float btn_after_inside_radius;
    private float btn_after_outside_radius;

    private float btn_return_length;
    private float btn_return_X;
    private float btn_return_Y;

    private float btn_left_X, btn_right_X, btn_result_radius;

    //state
    private int STATE_SELECTED;
    private final int STATE_LESSNESS = 0;
    private final int STATE_CAPTURED = 1;
    private final int STATE_RECORD = 2;
    private final int STATE_PICTURE_BROWSE = 3;
    private final int STATE_RECORD_BROWSE = 4;
    private final int STATE_READYQUIT = 5;
    private final int STATE_RECORDED = 6;
    private final int STATE_FINISH = 7;

    private boolean WriteFileFinish = false;

    private float key_down_Y;

    private RectF rectF;
    private float progress = 0;
    private boolean animating;
    private LongPressRunnable longPressRunnable = new LongPressRunnable();
    private RecordRunnable recordRunnable = new RecordRunnable();
    private ValueAnimator record_anim = ValueAnimator.ofFloat(0, 360);
    private CaptureListener mCaptureListener;
    private int mMaxRecordTime = 10;
    private boolean enableEdit = true;

    @RecordVideoView.Mode
    private int mMode;

    public CaptureButton(Context context) {
        this(context, null);
    }

    public CaptureButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CaptureButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        STATE_SELECTED = STATE_LESSNESS;
    }

    public void reStar() {
        STATE_SELECTED = STATE_LESSNESS;
        WriteFileFinish = false;
        requestLayout();
    }

    public void setWriteFileFinish(boolean writeFileFinish) {
        WriteFileFinish = writeFileFinish;
    }

    @RecordVideoView.Mode
    public int getMode() {
        return mMode;
    }

    public void setMode(@RecordVideoView.Mode int mode) {
        mMode = mode;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int width = widthSize;
        Log.i(TAG, "measureWidth = " + width);
        int height = (width / 9) * 4;
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        btn_center_X = getWidth() / 2;
        btn_center_Y = getHeight() / 2;

        btn_outside_radius = (float) (getWidth() / 9);
        btn_inside_radius = (float) (btn_outside_radius * 0.75);

        btn_before_outside_radius = (float) (getWidth() / 9);
        btn_before_inside_radius = (float) (btn_outside_radius * 0.75);
        btn_after_outside_radius = (float) (getWidth() / 6);
        btn_after_inside_radius = (float) (btn_outside_radius * 0.6);

        btn_return_length = (float) (btn_outside_radius * 0.35);
//        btn_result_radius = 80;
        btn_result_radius = (float) (getWidth() / 9);
        btn_left_X = getWidth() / 2;
        btn_right_X = getWidth() / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (STATE_SELECTED == STATE_LESSNESS || STATE_SELECTED == STATE_RECORD) {
            //draw capture button
            mPaint.setColor(0xFFEEEEEE);
            canvas.drawCircle(btn_center_X, btn_center_Y, btn_outside_radius, mPaint);
            mPaint.setColor(Color.WHITE);
            canvas.drawCircle(btn_center_X, btn_center_Y, btn_inside_radius, mPaint);
            mPaint.setTextSize(sp2px(14));
            mPaint.setTextAlign(Paint.Align.CENTER);
            String desc = "";
            if (mMode == RecordVideoView.TAKE_PHOTO) {
                desc = "轻触拍照";
            } else if (mMode == RecordVideoView.TAKE_RECORD) {
                desc = "长按摄像";
            } else {
                desc = "轻触拍照,长按摄像";
            }
            canvas.drawText(desc, btn_center_X, dp2px(14), mPaint);

            //draw Progress bar
            Paint paintArc = new Paint();
            paintArc.setAntiAlias(true);
            paintArc.setColor(0xFF00CC00);
            paintArc.setStyle(Paint.Style.STROKE);
            paintArc.setStrokeWidth(dp2px(5));

            rectF = new RectF(btn_center_X - (btn_after_outside_radius - dp2px(2.5f)),
                    btn_center_Y - (btn_after_outside_radius - dp2px(2.5f)),
                    btn_center_X + (btn_after_outside_radius - dp2px(2.5f)),
                    btn_center_Y + (btn_after_outside_radius - dp2px(2.5f)));
            canvas.drawArc(rectF, -90, progress, false, paintArc);

            //draw return button
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp2px(2));
            Path path = new Path();

            btn_return_X = ((getWidth() / 2) - btn_outside_radius) / 2;
            btn_return_Y = (getHeight() / 2 + dp2px(5));

            path.moveTo(btn_return_X - btn_return_length, btn_return_Y - btn_return_length);
            path.lineTo(btn_return_X, btn_return_Y);
            path.lineTo(btn_return_X + btn_return_length, btn_return_Y - btn_return_length);
            canvas.drawPath(path, paint);
        } else if (STATE_SELECTED == STATE_RECORD_BROWSE || STATE_SELECTED == STATE_PICTURE_BROWSE) {

            mPaint.setColor(0xFFEEEEEE);
            canvas.drawCircle(btn_left_X, btn_center_Y, btn_result_radius, mPaint);

            //left button
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp2px(1.5f));
            Path path = new Path();

            path.moveTo(btn_left_X - dp2px(1), btn_center_Y + dp2px(7));
            path.lineTo(btn_left_X + dp2px(7), btn_center_Y + dp2px(7));
            path.arcTo(new RectF(btn_left_X, btn_center_Y - dp2px(7), btn_left_X + dp2px(14), btn_center_Y + dp2px(7)), 90, -180);
            path.lineTo(btn_left_X - dp2px(7), btn_center_Y - dp2px(7));
            canvas.drawPath(path, paint);


            paint.setStyle(Paint.Style.FILL);
            path.reset();
            path.moveTo(btn_left_X - dp2px(7), btn_center_Y - dp2px(11));
            path.lineTo(btn_left_X - dp2px(7), btn_center_Y - dp2px(3));
            path.lineTo(btn_left_X - dp2px(11.5f), btn_center_Y - dp2px(7));
            path.close();
            canvas.drawPath(path, paint);

            //中间button

            if (enableEdit) {
                canvas.drawCircle(btn_center_X, btn_center_Y, btn_result_radius, mPaint);
                paint.setStyle(Paint.Style.STROKE);
                path.reset();
                path.moveTo(btn_center_X - dp2px(14), btn_center_Y);
                path.lineTo(btn_center_X - dp2px(7), btn_center_Y);
                canvas.drawPath(path, paint);
                path.reset();
                path.moveTo(btn_center_X - dp2px(5), btn_center_Y + dp2px(3));
                path.lineTo(btn_center_X - dp2px(5), btn_center_Y - dp2px(3));
                canvas.drawPath(path, paint);
                path.reset();
                path.moveTo(btn_center_X - dp2px(5), btn_center_Y);
                path.lineTo(btn_center_X + dp2px(14), btn_center_Y);
                canvas.drawPath(path, paint);

                path.moveTo(btn_center_X - dp2px(14), btn_center_Y + dp2px(11));
                path.lineTo(btn_center_X + dp2px(5), btn_center_Y + dp2px(11));
                canvas.drawPath(path, paint);
                path.reset();
                path.moveTo(btn_center_X + dp2px(5), btn_center_Y + dp2px(14));
                path.lineTo(btn_center_X + dp2px(5), btn_center_Y + dp2px(8));
                canvas.drawPath(path, paint);
                path.reset();
                path.moveTo(btn_center_X + dp2px(7), btn_center_Y + dp2px(11));
                path.lineTo(btn_center_X + dp2px(14), btn_center_Y + dp2px(11));
                canvas.drawPath(path, paint);

                path.moveTo(btn_center_X - dp2px(14), btn_center_Y - dp2px(11));
                path.lineTo(btn_center_X + dp2px(5), btn_center_Y - dp2px(11));
                canvas.drawPath(path, paint);
                path.reset();
                path.moveTo(btn_center_X + dp2px(5), btn_center_Y - dp2px(14));
                path.lineTo(btn_center_X + dp2px(5), btn_center_Y - dp2px(8));
                canvas.drawPath(path, paint);
                path.reset();
                path.moveTo(btn_center_X + dp2px(7), btn_center_Y - dp2px(11));
                path.lineTo(btn_center_X + dp2px(14), btn_center_Y - dp2px(11));
                canvas.drawPath(path, paint);
            }


            //右边button
            mPaint.setColor(Color.WHITE);
            canvas.drawCircle(btn_right_X, btn_center_Y, btn_result_radius, mPaint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(0xFF00CC00);
            paint.setStrokeWidth(dp2px(2));
            path.reset();
            path.moveTo(btn_right_X - dp2px(14), btn_center_Y);
            path.lineTo(btn_right_X - dp2px(4), btn_center_Y + dp2px(11));
            path.lineTo(btn_right_X + dp2px(15), btn_center_Y - dp2px(10));
            path.lineTo(btn_right_X - dp2px(4), btn_center_Y + dp2px(9));
            path.close();
            canvas.drawPath(path, paint);
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (animating) {
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (STATE_SELECTED == STATE_LESSNESS) {
                    if (event.getY() > btn_return_Y - dp2px(19) &&
                            event.getY() < btn_return_Y + dp2px(5) &&
                            event.getX() > btn_return_X - dp2px(19) &&
                            event.getX() < btn_return_X + dp2px(19)) {
                        STATE_SELECTED = STATE_READYQUIT;
                    } else if (event.getY() > btn_center_Y - btn_outside_radius &&
                            event.getY() < btn_center_Y + btn_outside_radius &&
                            event.getX() > btn_center_X - btn_outside_radius &&
                            event.getX() < btn_center_X + btn_outside_radius &&
                            event.getPointerCount() == 1
                            ) {

                        if (mMode == RecordVideoView.TAKE_PHOTO) {
                            STATE_SELECTED = STATE_CAPTURED;
                        } else {
                            key_down_Y = event.getY();
                            STATE_SELECTED = STATE_RECORD;
                            if (mCaptureListener != null) {
                                mCaptureListener.record();
                            }
                            Log.e(TAG, "onTouchEvent: 点击了");
                            postCheckForLongTouch();
                        }
                    }
                } else if (STATE_SELECTED == STATE_RECORD_BROWSE || STATE_SELECTED == STATE_PICTURE_BROWSE) {
                    if (event.getY() > btn_center_Y - btn_result_radius &&
                            event.getY() < btn_center_Y + btn_result_radius &&
                            event.getX() > btn_left_X - btn_result_radius &&
                            event.getX() < btn_left_X + btn_result_radius &&
                            event.getPointerCount() == 1
                            ) {
                        if (mCaptureListener != null) {
                            if (STATE_SELECTED == STATE_RECORD_BROWSE) {
                                mCaptureListener.deleteRecordResult();
                            } else if (STATE_SELECTED == STATE_PICTURE_BROWSE) {
                                mCaptureListener.cancel();
                            }
                        }
                        STATE_SELECTED = STATE_LESSNESS;
                        WriteFileFinish = false;
                        btn_left_X = btn_center_X;
                        btn_right_X = btn_center_X;
                        invalidate();
                    } else if (event.getY() > btn_center_Y - btn_result_radius &&
                            event.getY() < btn_center_Y + btn_result_radius &&
                            event.getX() > btn_right_X - btn_result_radius &&
                            event.getX() < btn_right_X + btn_result_radius &&
                            event.getPointerCount() == 1
                            ) {
                        if (STATE_SELECTED != STATE_FINISH && mCaptureListener != null) {
                            if (STATE_SELECTED == STATE_RECORD_BROWSE) {
                                if (WriteFileFinish) {
                                    mCaptureListener.getRecordResult();
                                } else {
                                    Toast.makeText(mContext, "文件正在合成", Toast.LENGTH_SHORT).show();
                                }
                            } else if (STATE_SELECTED == STATE_PICTURE_BROWSE) {
                                if (WriteFileFinish) {
                                    mCaptureListener.determine();
                                } else {
                                    Toast.makeText(mContext, "文件正在合成", Toast.LENGTH_SHORT).show();
                                }
                            }
                            STATE_SELECTED = STATE_FINISH;
                        }
                    } else if (enableEdit && event.getY() > btn_center_Y - btn_result_radius &&
                            event.getY() < btn_center_Y + btn_result_radius &&
                            event.getX() > btn_center_X - btn_result_radius &&
                            event.getX() < btn_center_X + btn_result_radius &&
                            event.getPointerCount() == 1) {
                        if (STATE_SELECTED != STATE_FINISH && mCaptureListener != null) {
                            if (STATE_SELECTED == STATE_RECORD_BROWSE) {
                                if (WriteFileFinish) {
                                    mCaptureListener.editVideo();
                                } else {
                                    Toast.makeText(mContext, "文件正在合成", Toast.LENGTH_SHORT).show();
                                }
                            } else if (STATE_SELECTED == STATE_PICTURE_BROWSE) {
                                if (WriteFileFinish) {
                                    mCaptureListener.editPicture();
                                } else {
                                    Toast.makeText(mContext, "文件正在合成", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (event.getY() > btn_center_Y - btn_outside_radius &&
                        event.getY() < btn_center_Y + btn_outside_radius &&
                        event.getX() > btn_center_X - btn_outside_radius &&
                        event.getX() < btn_center_X + btn_outside_radius &&
                        key_down_Y-event.getY() > btn_outside_radius
                        ) {
                }
                if (mCaptureListener != null) {
                    mCaptureListener.scale((key_down_Y - event.getY()-btn_outside_radius) / getWidth());
                }
                break;
            case MotionEvent.ACTION_UP:
                removeCallbacks(longPressRunnable);
                if (STATE_SELECTED == STATE_READYQUIT) {
                    if (event.getY() > btn_return_Y - dp2px(19) &&
                            event.getY() < btn_return_Y + dp2px(5) &&
                            event.getX() > btn_return_X - dp2px(19) &&
                            event.getX() < btn_return_X + dp2px(19)) {
                        STATE_SELECTED = STATE_LESSNESS;
                        if (mCaptureListener != null) {
                            mCaptureListener.quit();
                        }
                    }
                } else if (STATE_SELECTED == STATE_RECORD) {
                    Log.e(TAG, "onTouchEvent: " + record_anim.getCurrentPlayTime());
                    if (record_anim.getCurrentPlayTime() < 800) {
                        if (mMode == RecordVideoView.TAKE_RECORD) {
                            Toast.makeText(mContext, "拍摄时间太短，请重新拍摄", Toast.LENGTH_SHORT).show();
                            STATE_SELECTED = STATE_LESSNESS;
                        } else {
                            STATE_SELECTED = STATE_PICTURE_BROWSE;
                            if (mCaptureListener != null) {
                                mCaptureListener.capture();
                            }
                        }
                        if (record_anim.getCurrentPlayTime() == 0) {
                            removeCallbacks(recordRunnable);
                        }
                        if (mCaptureListener != null) {
                            mCaptureListener.rencordFail();
                        }
                    } else {
                        STATE_SELECTED = STATE_RECORD_BROWSE;
                        if (mCaptureListener != null) {
                            mCaptureListener.rencordEnd();
                        }
                    }
                    record_anim.cancel();
                    progress = 0;
                    invalidate();
                    startAnimation(btn_outside_radius, btn_before_outside_radius, btn_inside_radius, btn_before_inside_radius);
                    if (STATE_SELECTED == STATE_RECORD_BROWSE || STATE_SELECTED == STATE_PICTURE_BROWSE) {
                        captureOrRecordSuccess();
                    }
                } else if (STATE_SELECTED == STATE_CAPTURED) {
                    STATE_SELECTED = STATE_PICTURE_BROWSE;
                    if (mCaptureListener != null) {
                        mCaptureListener.capture();
                    }
                    captureOrRecordSuccess();
                }
                break;
        }
        return true;
    }

    private void postCheckForLongTouch() {
        postDelayed(longPressRunnable, 200);
    }


    private class LongPressRunnable implements Runnable {
        @Override
        public void run() {
            startAnimation(btn_before_outside_radius, btn_after_outside_radius, btn_before_inside_radius, btn_after_inside_radius);
        }
    }

    private void captureOrRecordSuccess() {
        animating = true;
        postDelayed(new Runnable() {
            @Override
            public void run() {
                captureAnimation(getWidth() / 5, (getWidth() / 5) * 4);
            }
        }, 300);
    }

    private class RecordRunnable implements Runnable {
        @Override
        public void run() {
            record_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (STATE_SELECTED == STATE_RECORD) {
                        progress = (float) animation.getAnimatedValue();
                    }
                    invalidate();
                }
            });
            record_anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if (STATE_SELECTED == STATE_RECORD) {
                        STATE_SELECTED = STATE_RECORD_BROWSE;
                        progress = 0;
                        invalidate();
                        startAnimation(btn_after_outside_radius, btn_before_outside_radius, btn_after_inside_radius, btn_before_inside_radius);
                        captureOrRecordSuccess();
                        if (mCaptureListener != null) {
                            mCaptureListener.rencordEnd();
                        }
                    }
                }
            });
            record_anim.setInterpolator(new LinearInterpolator());
            record_anim.setDuration(mMaxRecordTime * 1000);
            record_anim.start();
        }
    }

    public void setMaxRecordTime(int maxRecordTime) {
        if (maxRecordTime < 3) {
            throw new RuntimeException("maxRecordTime must >= 3 , your  maxRecordTime = maxRecordTime");
        }
        mMaxRecordTime = maxRecordTime;
    }

    private void startAnimation(float outside_start, float outside_end, float inside_start, float inside_end) {

        ValueAnimator outside_anim = ValueAnimator.ofFloat(outside_start, outside_end);
        ValueAnimator inside_anim = ValueAnimator.ofFloat(inside_start, inside_end);
        outside_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                btn_outside_radius = (float) animation.getAnimatedValue();
                invalidate();
            }

        });
        outside_anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (STATE_SELECTED == STATE_RECORD) {
                    postDelayed(recordRunnable, 200);
                }
            }
        });
        inside_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                btn_inside_radius = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        outside_anim.setDuration(200);
        inside_anim.setDuration(200);
        outside_anim.start();
        inside_anim.start();
    }

    private void captureAnimation(float left, float right) {
//        Toast.makeText(mContext,left+ " = "+right,Toast.LENGTH_SHORT).show();
        Log.i("CaptureButtom", left + "==" + right);
        ValueAnimator left_anim = ValueAnimator.ofFloat(btn_left_X, left);
        ValueAnimator right_anim = ValueAnimator.ofFloat(btn_right_X, right);
        left_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                btn_left_X = (float) animation.getAnimatedValue();
                Log.i("CJT", btn_left_X + "=====");
                invalidate();
            }

        });
        right_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                btn_right_X = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        left_anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animating = false;
            }
        });
        left_anim.setDuration(200);
        right_anim.setDuration(200);
        left_anim.start();
        right_anim.start();
    }

    public boolean isEnableEdit() {
        return enableEdit;
    }

    public void setEnableEdit(boolean enableEdit) {
        this.enableEdit = enableEdit;
    }

    public void setCaptureListener(CaptureListener mCaptureListener) {
        this.mCaptureListener = mCaptureListener;
    }


    public interface CaptureListener {
        public void capture();

        public void cancel();

        public void determine();

        public void quit();

        public void record();

        public void rencordEnd();

        public void rencordFail();

        public void getRecordResult();

        public void deleteRecordResult();

        public void scale(float scaleValue);

        void editVideo();

        void editPicture();
    }

    private int dp2px(float dpValue) {
        float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5F);
    }

    private int sp2px(float spValue) {
        float fontScale = mContext.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5F);
    }
}
