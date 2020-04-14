package xyz.hiroshifuu.speechapp.activity;

import android.content.Context;
import android.view.View;
import android.view.WindowManager;

public class FloatingManager {

    private WindowManager mWindowManager;
    private volatile static FloatingManager mInstance;
    private Context mContext;

    public static FloatingManager getInstance(Context context) {
        if (mInstance == null) {
            synchronized(FloatingManager.class) {
                mInstance = new FloatingManager(context);
            }
        }
        return mInstance;
    }

    private FloatingManager(Context context) {
        mContext = context;
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
    }

    protected boolean addView(View view, WindowManager.LayoutParams params) {
        try {
            mWindowManager.addView(view, params);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    protected boolean removeView(View view) {
        try {
            mWindowManager.removeView(view);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    protected boolean updateView(View view, WindowManager.LayoutParams params) {
        try {
            mWindowManager.updateViewLayout(view, params);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}

