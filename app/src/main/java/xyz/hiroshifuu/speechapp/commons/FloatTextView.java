package xyz.hiroshifuu.speechapp.commons;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.TextView;

public class FloatTextView extends android.support.v7.widget.AppCompatTextView {

    private float textLength = 0f;

    private float viewWidth = 0f;

    private float tx = 0f;

    private float ty = 0f;

    private float temp_tx1 = 0.0f;

    private float temp_tx2 = 0x0f;

    private boolean isStarting = false;

    private Paint paint = null;

    private String text = "";

    private float sudu;

    public FloatTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public void initScrollTextView(WindowManager windowManager, String text,
                                   float su) {

        paint = this.getPaint();

        this.text = text;
        this.sudu = su;
        textLength = paint.measureText(text);//
        viewWidth = this.getWidth();//
        if (viewWidth == 0) {

            Display display = windowManager.getDefaultDisplay();
            viewWidth = display.getWidth();//

        }
        tx = textLength;
        temp_tx1 = viewWidth + textLength;
        temp_tx2 = viewWidth + textLength * 2;//

        ty = this.getTextSize() + this.getPaddingTop();
    }

    public void starScroll() {

        isStarting = true;
        this.invalidate();// flash screen
    }


    public void stopScroll() {
        isStarting = false;
        this.invalidate();// flash screen
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isStarting) {

            paint.setARGB(255, 200, 200, 200);
            canvas.drawText(text, temp_tx1 - tx, ty, paint);
            tx += sudu;
            // 当文字滚动到屏幕的最左边
            if (tx > temp_tx2) {
                // 把文字设置到最右边开始
                tx = textLength;
            }
            this.invalidate();// flash screen
        }
        super.onDraw(canvas);
    }
}
