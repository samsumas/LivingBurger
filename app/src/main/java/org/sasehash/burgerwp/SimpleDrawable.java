/*
 * Licensed under GPL 3.0
 */

/*
 * Licensed under GPL 3.0
 */

package org.sasehash.burgerwp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.DisplayMetrics;

public class SimpleDrawable implements IDrawable {

    /**
     * Speed in X - Direction in DP (DP are independent of screen size, which makes it better then pixels)
     */
    private float dirX = 1.0F;

    /**
     * Speed in Y - Direction in DP
     */
    private float dirY = 1.0F;

    /**
     * Rotation speed in degrees
     */
    private float rotation = 1.0F;

    private Matrix currentPosition = new Matrix();
    private Bitmap texture;
    private Paint p;
    private float middleX;
    private float middleY;
    private Context context;
    private int dpi;
    private float dpToPixels;

    public SimpleDrawable(Bitmap bmp, Context context) {
        this.texture = bmp;
        p = new Paint();
        p.setFilterBitmap(false);
        p.setColor(Color.BLACK);
        p.setStyle(Paint.Style.FILL);

        dpi = context.getResources().getDisplayMetrics().densityDpi;
        dpToPixels = context.getResources().getDisplayMetrics().density;


        middleX = texture.getScaledHeight(dpi) / 2;
        middleY = texture.getScaledWidth(dpi) / 2;

        this.context = context;
    }

    public SimpleDrawable(Context context) {
        this(BitmapFactory.decodeResource(context.getResources(), R.drawable.burger), context);
    }

    @Override
    public void draw(Canvas canvas) {
        //multiply with dpToPixel so burgers aren't faster on screens with low resolution
        currentPosition.postTranslate(dirX * dpToPixels, dirY * dpToPixels);
        currentPosition.preRotate(rotation, middleX, middleY);
        canvas.drawBitmap(texture, currentPosition, p);
    }

    @Override
    public boolean canBeReplaced() {
        float start[] = new float[]{0, 0};
        currentPosition.mapPoints(start);
        //px = dp * (dpi / 160)
        // dp = px * 160 /dpi
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return start[0] < -texture.getScaledHeight(dpi)  //exited left
                || start[0] > dm.widthPixels * 160 / dpi //exited right
                || start[1] < -texture.getScaledWidth(dpi) //exited up
                || start[1] > dm.heightPixels * 160 / dpi; //exited down

    }
}
