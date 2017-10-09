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
        /* Values needed internally */
        private Handler handler = new Handler();
        private final Runnable drawRunner = new Runnable() {
            @Override
            public void run() {
                draw();
            }
        };
        private boolean visibility = true;
        private final double NEARLY_ZERO = 0.1;
        private long time;
        private Paint p = new Paint();
        private int width, height;
        private List<ToDraw> objects = new ArrayList<>();


        /* Values that can be tweaked */
        private final boolean runAway = false;
        private final int burgerSpeed = 5;
        private final int heartSpeed = 5;
        private final int burgerRunningTime = 3000;
        private final int heartRunningTime = 1500;

        private final int burgerTextureID = R.drawable.burger;
        private final int heartTextureID = R.drawable.heart;
        private final int backgroundColor = Color.WHITE;
        private final int burgerCount =1;

        public JumpingEngine() {
            p.setFilterBitmap(false);
            p.setStyle(Paint.Style.FILL);
            time = System.currentTimeMillis();
            Bitmap burgerTexture = BitmapFactory.decodeResource(getResources(), burgerTextureID);
            //using more then one burger isn't a good idea, they tended (in my testcase with 10 burgers) to overlap very quickly and
            //soon it looked like there were only 2 or three
            for (int i=0; i<burgerCount; i++) {
                final int curr =i;
                objects.add(new ToDraw(burgerTexture, 0, 0, new Movement() {
                    @Override
                    public int moveX(long time) {
                        return burgerSpeed-curr;
                    }

                    @Override
                    public int moveY(long time) {
                        return curr;
                    }
                }, 0, burgerRunningTime));
            }
        }

        private void spawnHearts(int i) {
            //TODO : recheck if "decoding the resource each time it is needed" is a good idea
            Bitmap heartTexture = BitmapFactory.decodeResource(getResources(), heartTextureID);
            for (int j=0; j<i; j++) {
                //get random location near burger
                final int sizeOfBurger=50;
                int spawnAtX = objects.get(0).getX() + (int) (sizeOfBurger * Math.random()) + sizeOfBurger/2;
                int spawnAtY = objects.get(0).getY() + (int) (sizeOfBurger * Math.random()) + sizeOfBurger/2;
                //we don't want all the hearts to go in the same directions
                final double a = Math.random()*2 -1;
                final double b = Math.random()*2 -1;
                objects.add(new ToDraw(heartTexture, spawnAtX, spawnAtY, new Movement() {
                    @Override
                    public int moveX(long time) {
                        return (int) (a*heartSpeed * Math.sin(time));
                    }

                    @Override
                    public int moveY(long time) {
                        return (int) (b*heartSpeed * Math.cos(time));
                    }
                }, 0, heartRunningTime,true));
            }
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
                canvas.drawColor(backgroundColor);

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
            for (ToDraw td : objects) {
                runAwayFromFinger(td, event);
            }
            /**
            //50 % chance to spawn hearts
            int toSpawn = (int) (Math.random()*2);
            if (toSpawn > 0) {
                //if there are hearts spawning, then 50 % chance for another heart (total 25%)
                toSpawn += (int) (Math.random() * 2);
                if (toSpawn > 1) {
                    //25% chance for 3 hearts (total not much)
                    toSpawn += (int) (Math.random() * 1.333);
                }
            }
            spawnHearts(toSpawn);
             **/
        }

        private void runAwayFromFinger(ToDraw td, MotionEvent event) {
            td.setCurrentMovementTime(0);
            td.setMaxMovementTime(burgerRunningTime);
            int dx = (int) Math.round(td.getX() - event.getX());
            int dy = (int) Math.round(td.getY() - event.getY());

            //this android runs from humans away, depending on the runAway value
            double size = Math.sqrt(dx * dx + dy * dy);
            double vecX = ((double) dx) / size;
            vecX *= burgerSpeed;
            double vecY = ((double) dy) / size;
            vecY *= burgerSpeed;
            if (!runAway) {
                vecX = -vecX;
                vecY = -vecY;
            }
            final int a = (int) Math.round(vecX);
            final int b = (int) Math.round(vecY);
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
