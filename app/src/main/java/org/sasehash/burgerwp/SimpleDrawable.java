/*
 * Licensed under GPL 3.0
 */

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
    private float dirX = 3.0F;

    /**
     * Speed in Y - Direction in DP
     */
    private float dirY = 3.0F;

    /**
     * Rotation speed in degrees
     */
    private float rotation = 1.0F;

    private Matrix currentPosition = new Matrix();
    private Matrix currentRotation = new Matrix();
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
        switch (isOutsideScreen()) {
            case ON_SCREEN:
                break;
            case TOP:
                dirY = Math.abs(dirX);
                break;
            case DOWN:
                dirY = -Math.abs(dirY);
                break;
            case LEFT:
                dirX = Math.abs(dirX);
                break;
            case RIGHT:
                dirX = -Math.abs(dirX);
                break;
        }
        //multiply with dpToPixel so burgers aren't faster on screens with low resolution
        currentPosition.postTranslate(dirX * dpToPixels, dirY * dpToPixels);
        currentRotation.preRotate(rotation, middleX, middleY);
        Matrix transformation = new Matrix(currentRotation);
        transformation.postConcat(currentPosition);
        canvas.drawBitmap(texture, transformation, p);
    }

    private position isOutsideScreen() {
        float start[] = new float[]{0, 0};
        currentPosition.mapPoints(start);

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        if (start[0] < 0) {
            return position.LEFT;
        }
        if (start[0] > dm.widthPixels - texture.getScaledWidth(dpi)) {
            return position.RIGHT;
        }
        if (start[1] < 0) {
            return position.TOP;
        }
        if (start[1] > dm.heightPixels - texture.getScaledWidth(dpi)) {
            return position.DOWN;
        }
        return position.ON_SCREEN;
    }

    @Override
    public boolean canBeReplaced() {
        return false;
    }
}
