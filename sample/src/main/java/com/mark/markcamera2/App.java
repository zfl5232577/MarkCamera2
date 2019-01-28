package com.mark.markcamera2;

import android.app.Application;

import com.mark.aoplibrary.MarkAOPHelper;

/**
 * <pre>
 *     author : Mark
 *     e-mail : makun.cai@aorise.org
 *     time   : 2018/09/28
 *     desc   : TODO
 *     version: 1.0
 * </pre>
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        /**
         *  @see <a href="https://github.com/zfl5232577/MarkAop">切面框架</a>
         */
        MarkAOPHelper.getInstance().init(this);//初始化
    }
}
