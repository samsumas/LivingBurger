package org.sasehash.burgerwp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.service.wallpaper.WallpaperService;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.util.List;

import java.util.ArrayList;

/**
 * Created by sami on 08/10/17.
 */

public class JumpingBurger extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new JumpingEngine();
    }

    private class JumpingEngine extends Engine {
        private Handler handler = new Handler();
        private final Runnable drawRunner = new Runnable() {
            @Override
            public void run() {
                draw();
            }
        };
        private boolean visibility = true;
        private int height, width;
        private final int extremaX = 5;
        private final int extremaY = 5;
        private final int translationTimeX = 6;
        private final int translationTimeY = 6;
        private final double rotationSteps = 1.0;
        private List<ToDraw> objects = new ArrayList<>();
        private Paint p = new Paint();
        private long time = 0;
        private final double NEARLY_ZERO = 0.1;
        private final boolean runAway = false;
        private final int androidSpeed = 5;
        private final int androidRunningTime = 3000;
        private final int textureID = R.drawable.burger;

        public JumpingEngine() {
            p.setFilterBitmap(false);
            p.setStyle(Paint.Style.FILL);
            time = System.currentTimeMillis();
            Bitmap b = BitmapFactory.decodeResource(getResources(), textureID );
            objects.add(new ToDraw(b, 50, 50, new Movement() {
                @Override
                public int moveX(long time) {
                    return 10;
                }

                @Override
                public int moveY(long time) {
                    return 0;
                }
            }, 0, 2000));
        }

        private void draw() {
            long t = System.currentTimeMillis();
            for (ToDraw td : objects) {
                td.move(t - time);
            }
            time = t;
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                canvas.drawColor(Color.WHITE);
                for (ToDraw actual : objects) {
                    canvas.drawBitmap(actual.getTexture(), actual.getX(), actual.getY(), p);
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas);
                }
            }
            handler.removeCallbacks(drawRunner);
            if (visibility) {
                handler.postDelayed(drawRunner, 40);
            }
        }


        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visibility = visible;
            if (visible) {
                handler.post(drawRunner);
            } else {
                handler.removeCallbacks(drawRunner);
            }
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            super.onTouchEvent(event);
            for (ToDraw td:objects) {
                    td.setCurrentMovementTime(0);
                    td.setMaxMovementTime(androidRunningTime);
                    int dx = (int) Math.round(td.getX() - event.getX());
                    int dy = (int) Math.round(td.getY() - event.getY());
                    //this android runs from humans away
                    double size = Math.sqrt(dx*dx+dy*dy);
                    double vecX = ((double) dx) / size;
                    vecX *= androidSpeed;
                    double vecY = ((double) dy) / size;
                    vecY *= androidSpeed;
                    if (!runAway) {
                        vecX = -vecX;
                        vecY = -vecY;
                    }
                    final int a=(int) Math.round(vecX);
                    final int b=(int) Math.round(vecY);
                    td.setMov(new Movement() {
                        @Override
                        public int moveX(long time) {
                            return a;
                        }

                        @Override
                        public int moveY(long time) {
                            return b;
                        }
                    });
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            this.visibility = false;
            handler.removeCallbacks(drawRunner);
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format,
                                     int width, int height) {
            this.width = width;
            this.height = height;
            super.onSurfaceChanged(holder, format, width, height);
        }
    }


}
