package com.mark.markcameralib;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
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

import com.aliyun.vod.common.utils.DensityUtil;
import com.mark.markcameralib.view.CutView;
import com.mark.markcameralib.view.SoftKeyBoardListener;
import com.mark.markcameralib.view.TouchView;
import com.mark.markcameralib.view.TuyaView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * <pre>
 *     author : Mark
 *     e-mail : makun.cai@aorise.org
 *     time   : 2019/01/30
 *     desc   : 图片编辑控件，涂鸦，添加表情，文字，图片裁剪
 *     version: 1.0
 * </pre>
 */
public class EditImageView extends FrameLayout {

    private static final String TAG = EditImageView.class.getSimpleName();
    private int[] drawableBg = new int[]{R.drawable.color1, R.drawable.color2, R.drawable.color3, R.drawable.color4, R.drawable.color5};
    private int[] colors = new int[]{R.color.color1, R.color.color2, R.color.color3, R.color.color4, R.color.color5};
    private int[] expressions = new int[]{R.mipmap.expression1, R.mipmap.expression2, R.mipmap.expression3, R.mipmap.expression4,
            R.mipmap.expression5, R.mipmap.expression6, R.mipmap.expression7, R.mipmap.expression8};

    private ImageView mImageView;
    private Bitmap mBitmap;
    private LinearLayout llTuYAColor;
    private View v_line;
    private LinearLayout ll_text_color;
    private RelativeLayout rl_touch_view;
    private RelativeLayout rl_edit_text;
    private RelativeLayout rl_cut;
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

    private EditImageViewCallback mCallback;

    private String imageUrl;
    private String outFileDir = Constants.TEMP_PATH;
    private boolean isTextColor = true;
    private boolean isFirstShowEditText;
    private InputMethodManager manager;

    private Handler mBackgroundHandler;
    private AlertDialog progressDialog;
    private TextView progressTextView;
    private int mImageWidth;
    private int mImageHeight;

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
            HandlerThread thread = new HandlerThread("edit_image");
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }
        return mBackgroundHandler;
    }

    public EditImageView(@NonNull Context context) {
        this(context, null);
    }

    public EditImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EditImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        initExpression();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.e(TAG, "onDetachedFromWindow:------------------------> ");
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
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
        FrameLayout.inflate(context, R.layout.markcamera_layout_edit_image_view, this);
        mImageView = findViewById(R.id.imageview);
        rl_tuya = findViewById(R.id.rl_tuya);
        tuyaView = findViewById(R.id.tuyaView);
        rl_touch_view = findViewById(R.id.rl_touch_view);
        layout_control = findViewById(R.id.layout_control);
        rl_cut = findViewById(R.id.rl_cut);
        cutView = findViewById(R.id.cutView);
        tv_cancel_cut = findViewById(R.id.tv_cancel_cut);
        tv_complete_cut = findViewById(R.id.tv_complete_cut);
        tv_cancel_edit = findViewById(R.id.tv_cancel_edit);
        tv_complete_edit = findViewById(R.id.tv_complete_edit);
        iv_tuya = findViewById(R.id.iv_tuya);
        iv_biaoqing = findViewById(R.id.iv_biaoqing);
        iv_wenzi = findViewById(R.id.iv_wenzi);
        iv_cut = findViewById(R.id.iv_cut);
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

        tv_cancel_cut.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                changeCutState(false);
            }
        });

        tv_complete_cut.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                cropImage();
            }
        });

        rl_back.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                tuyaView.backPath();
            }
        });

        tv_cancel_text_edit.setOnClickListener(new OnClickListener() {
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
                //TODO 编辑图片
                showProgressDialog("图片编辑中...");
                getBackgroundHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        rl_tuya.setDrawingCacheEnabled(true);
                        rl_tuya.buildDrawingCache();  //启用DrawingCache并创建位图
                        int bitmapHeight = mImageHeight * getWidth() / mImageWidth;
                        Bitmap bitmap = Bitmap.createBitmap(rl_tuya.getDrawingCache(), 0, (getHeight() - bitmapHeight) / 2, getWidth(), bitmapHeight); //创建一个DrawingCache的拷贝，因为DrawingCache得到的位图在禁用后会被回收
                        rl_tuya.setDrawingCacheEnabled(false);
                        rl_tuya.destroyDrawingCache();
                        Bitmap newmap = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(newmap);
                        canvas.drawBitmap(mBitmap, 0, 0, null);
                        canvas.drawBitmap(bitmap, 0, 0, null);
                        canvas.save(Canvas.ALL_SAVE_FLAG);
                        canvas.restore();
                        final File pictureFile = new File(outFileDir, System.currentTimeMillis() + ".jpg");
                        FileOutputStream fileOutputStream = null;
                        try {
                            fileOutputStream = new FileOutputStream(pictureFile);
                            newmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                            //编辑完成删除原图片
                            File souImage = new File(imageUrl);
                            if (souImage.exists()) {
                                souImage.delete();
                            }
                            post(new Runnable() {
                                @Override
                                public void run() {
                                    closeProgressDialog();
                                    if (mCallback != null) {
                                        mCallback.completeEdit(pictureFile.getAbsolutePath());
                                    }
                                }
                            });
                        } catch (FileNotFoundException e) {
                            closeProgressDialog();
                            if (mCallback != null) {
                                mCallback.editError();
                            }
                            e.printStackTrace();
                        } finally {
                            bitmap.recycle();
                            newmap.recycle();
                            try {
                                fileOutputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
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
    }

    private void cropImage() {
        showProgressDialog("图片裁剪中...");
        getBackgroundHandler().post(new Runnable() {
            @Override
            public void run() {
                float[] cutArr = cutView.getCutArr();
                float left = cutArr[0];
                float top = cutArr[1];
                float right = cutArr[2];
                float bottom = cutArr[3];
                if (left == 0 && top == 0 && right == cutView.getRectWidth() && bottom == cutView.getRectHeight()) {
                    changeCutState(false);
                    closeProgressDialog();
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
                int cropWidth = (int) (mImageWidth * (rightPro - leftPro));
                int cropHeight = (int) (mImageHeight * (bottomPro - topPro));
                int x = (int) (leftPro * mImageWidth);
                int y = (int) (topPro * mImageHeight);
                mBitmap = Bitmap.createBitmap(mBitmap, x, y, cropWidth, cropHeight);
                post(new Runnable() {
                    @Override
                    public void run() {
                        mImageView.setImageBitmap(mBitmap);
                        mImageWidth = mBitmap.getWidth();
                        mImageHeight = mBitmap.getHeight();
                        closeProgressDialog();
                        changeCutState(false);
                    }
                });
            }
        });
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
                    if (rl_cut.isShown()) {
                        changeCutState(false);
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

    private void enterCutAnima() {
        int width = mImageView.getWidth();
        float[] cutMarginArr = cutView.getMargin();
        float scale = (width - cutMarginArr[0] - cutMarginArr[2]) / width;
        ViewCompat.animate(mImageView).setListener(new ViewPropertyAnimatorListener() {
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
        ViewCompat.animate(mImageView).setListener(new ViewPropertyAnimatorListener() {
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
            relativeLayoutTuYa.setOnClickListener(new OnClickListener() {
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
            relativeLayoutText.setOnClickListener(new OnClickListener() {
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
            imageView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    rl_expression.setVisibility(View.GONE);
                    addExpressionToWindow(result);
                }
            });
            rl_expression.addView(imageView);
        }
    }

    public void setCallback(EditImageViewCallback callback) {
        mCallback = callback;
    }

    public void setOutFileDir(String outFileDir) {
        this.outFileDir = outFileDir;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        mBitmap = BitmapFactory.decodeFile(imageUrl);
        mImageView.setImageBitmap(mBitmap);
        getBackgroundHandler().post(new Runnable() {
            @Override
            public void run() {
                mImageWidth = mBitmap.getWidth();
                mImageHeight = mBitmap.getHeight();
            }
        });
    }

    public interface EditImageViewCallback {
        void cancelEdit();

        void completeEdit(String outPath);

        void editProgress(int progress);

        void editError();

    }

}
