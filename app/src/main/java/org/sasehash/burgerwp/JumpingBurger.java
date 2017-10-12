/*
 * Licensed under GPL 3.0
 */

package org.sasehash.burgerwp;

import android.content.SharedPreferences;
import android.graphics.*;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sami on 08/10/17.
 */

public class JumpingBurger extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new JumpingEngine();
    }

    private class JumpingEngine extends Engine {
        private final double NEARLY_ZERO = 0.1;
        /* Values that can be tweaked */
        private final boolean runAway;
        private final int burgerSpeed = 5;
        private final int heartSpeed = 5;
        private final int burgerRunningTime = Integer.MAX_VALUE;
        private final int pizzaRunningTime = Integer.MAX_VALUE;
        private final int heartRunningTime = Integer.MAX_VALUE;
        private final int burgerTextureID = R.drawable.burger;
        private final int heartTextureID = R.drawable.heart;
        private final int pizzaTextureID = R.drawable.pizza;
        private final int backgroundColor;
        private final int burgerCount = 20;
        private final int pizzaCount = 20;
        private final int sleepBetweenRedraws = 35;
        /* Values needed internally */
        private Handler handler = new Handler();
        private boolean visibility = true;
        private long time;
        private Paint p = new Paint();
        private int width, height;
        private List<ToDraw> objects = new ArrayList<>();
        private List<ToDraw> doubles = new ArrayList<>();
        private Bitmap backgroundImage;
        private boolean useBackgroundImage;

        private final Runnable drawRunner = new Runnable() {
            @Override
            public void run() {
                draw();
            }
        };


        public JumpingEngine() {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(JumpingBurger.this);
            runAway = settings.getBoolean("pref_run_away", false);
            useBackgroundImage = settings.getBoolean("pref_bg_color_or_bg_image", false);
            backgroundColor = settings.getInt("bg_color_int", Color.BLACK);
            if (useBackgroundImage) {
                try {
                    String filename = settings.getString("pref_bg_image", null);
                    if (filename == null) {
                        throw new IllegalStateException("Failed to get ImageName with intent");
                    }
                    if (filename.equals("abc")) {
                        throw new IllegalStateException("Got Standard (invalid) filename");
                    }
                    Uri uri = Uri.parse(filename);
                    try {
                        InputStream is = getContentResolver().openInputStream(uri);
                        backgroundImage = BitmapFactory.decodeStream(is);
                        try {
                            is.close();
                        } catch (IOException e) {
                            //well don't care about it, can't close what isn't opened lol
                        }
                    } catch (java.lang.SecurityException e) {
                        //permission whatever,remove wrong preferences
                        stopUsingBackgroundImage();
                    }

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    //continue with color instead of background
                    stopUsingBackgroundImage();
                }
            }
            p.setFilterBitmap(false);
            p.setColor(Color.BLACK);
            p.setStyle(Paint.Style.FILL);
            time = System.currentTimeMillis();
            spawnPizza();
            Bitmap burgerTexture = BitmapFactory.decodeResource(getResources(), burgerTextureID);
            //using more then one burger isn't a good idea, they tended (in my testcase with 10 burgers) to overlap very quickly and
            //soon it looked like there were only 2 or three
            for (int i = 0; i < burgerCount; i++) {
                final int curr = i;
                ToDraw temp = new ToDraw(burgerTexture, 0, 0, 0, burgerRunningTime, false, Integer.MAX_VALUE);
                final int abc = i;
                //you don't need x perfectly superposed burgers
                temp.setxVec(new Lambda() {
                    @Override
                    public int l(long x) {
                        return burgerCount - abc;
                    }
                });
                temp.setyVec(new Lambda() {
                    @Override
                    public int l(long x) {
                        return abc;
                    }
                });
                objects.add(temp);
            }
            final Bitmap pizzaTexture = BitmapFactory.decodeResource(getResources(), pizzaTextureID);
            for (int i = 0; i < pizzaCount; i++) {
                ToDraw td = new ToDraw(pizzaTexture, 0, 0, 0, burgerRunningTime, true, 0);
                final int abc = i;
                td.setxVec(new Lambda() {
                    @Override
                    public int l(long x) {
                        return abc;
                    }
                });
                td.setyVec(new Lambda() {
                    @Override
                    public int l(long x) {
                        return pizzaCount - abc;
                    }
                });
                objects.add(td);
            }
        }

        /**
         * stopUsingBacjgroundImage
         */
        private void stopUsingBackgroundImage() {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(JumpingBurger.this).edit();
            editor.putBoolean("pref_bg_color_or_bg_image", false);
            editor.apply();
            useBackgroundImage = false;
        }

        /**
         * checkValue
         * @param a
         * @param settings
         */
        private void checkValue(String a, SharedPreferences settings) {
            if (!settings.contains(a)) {
                throw new IllegalStateException("Cannot read " + a + " Settings from anim_preferences!");
            }
        }

        /**
         * moveObject
         * @param td
         * @param t
         */
        public void moveObject(ToDraw td, long t) {
            long dt = t + td.getCurrentMovementTime();
            if (!td.timeLeft() || td.isVecNull()) {
                return;
            }
            td.addToX(td.getxVec(t));
            td.addToY(td.getyVec(t));
            td.setCurrentMovementTime(dt);
            if (!isOnScreen(td)) {
                if (td.getBouncing() > 0) {
                    bounce(td);
                    td.setBouncing(td.getBouncing() - 1);
                    //if off-screen (happens when you touch too much for a while), reset onScreen
                    if (td.getX() < 0) {
                        td.setX(0);
                    }
                    if (td.getY() < 0) {
                        td.setY(0);
                    }
                    if (td.getX() > width - td.getWidth()) {
                        td.setX(width - td.getWidth());
                    }
                    if (td.getY() > height - td.getHeight()) {
                        td.setY(height - td.getHeight());
                    }

                } else {
                    //TODO function "going from left to right" is broken. will fix this another day
                    //objects.remove(td);
                    resetOnScreen(td);
                }
            }

        }

        /**
         * Move all object with moveObject
         * @param t
         */
        public void moveObjects(long t) {
            for (ToDraw td : objects) {
                moveObject(td, t);
            }
        }

        /**
         * Util module positiv
         * @param a
         * @param m
         * @return
         */
        private int modulo(int a, int m) {
            return (a % m + m) % m;
        }

        /**
         * inCorner
         * @param td
         * @return
         */
        private boolean inCorner(ToDraw td) {
            return (td.getX() < 0 || td.getX() + td.getWidth() > width)
                    && (td.getY() < 0 || td.getY() + td.getHeight() > height);
        }

        /**
         * outOfScreen
         * @param td
         * @return
         */
        private boolean outOfScreen(ToDraw td) {
            return ((td.getX() < 0 && td.getX() + td.getWidth() > 0) || (td.getX() + td.getWidth() >= width && td.getX() <= width))
                    || ((td.getY() < 0 && td.getHeight() + td.getHeight() > 0) || (td.getY() + td.getHeight() >= height && td.getY() <= height));
        }

        /**
         * completyOutOfScreen
         * @param td
         * @return
         */
        private boolean completelyOutOfScreen(ToDraw td) {
            return (td.getX() + td.getWidth() < 0 || td.getX() > width)
                    || (td.getY() + td.getHeight() < 0 || td.getY() > height);
        }

        /**
         * rectifyX
         * @param td
         * @return
         */
        private int rectifyX(ToDraw td) {
            if (td.getX() < 0) {
                return td.getX() + width;
            }
            if (td.getX() + td.getWidth() > width) {
                return td.getX() - width;
            }
            return td.getX();
        }

        /**
         * rectifyY
         * @param td
         * @return
         */
        private int rectifyY(ToDraw td) {
            if (td.getY() < 0) {
                return td.getY() + height;
            }
            if (td.getY() + td.getHeight() > height) {
                return td.getY() - height;
            }
            return td.getY();
        }

        /**
         * resetOnScreen
         * @param td
         */
        private void resetOnScreen(ToDraw td) {
            if (completelyOutOfScreen(td)) {
                td.setX(modulo(td.getX(), width));
                td.setY(modulo(td.getY(), height));
            }
            if (outOfScreen(td)) {
                ToDraw t = new ToDraw(td);
                t.setX(rectifyX(t));
                t.setY(rectifyY(t));
                doubles.add(t);
            }
        }

        /**
         * bouce
         * @param td
         */
        private void bounce(ToDraw td) {
            if (!isOnScreenX(td)) {
                td.bounceX();
            }
            //no else because it makes burgers disappear in corners
            if (!isOnScreenY(td)) {
                td.bounceY();
            }
        }

        /**
         * check if picture is on the screen
         *
         * @param td
         * @return
         */
        private boolean isOnScreen(ToDraw td) {
            return isOnScreenX(td) && isOnScreenY(td);
        }

        /**
         * check if the picure is on the screen verticaly
         *
         * @param td
         * @return
         */
        private boolean isOnScreenY(ToDraw td) {
            return td.getY() == modulo(td.getY(), height - td.getHeight());
        }

        /**
         * check if the picture is on screen horizontaly
         *
         * @param td
         * @return
         */
        private boolean isOnScreenX(ToDraw td) {
            return td.getX() == modulo(td.getX(), width - td.getWidth());
        }

        /**
         * Spanw pizza with parameter 1
         */
        private void spawnPizza() {
            spawnPizza(1);
        }

        /**
         * Spawn pizza
         *
         * @param i
         */
        private void spawnPizza(int i) {
            Bitmap pizzaTexture = BitmapFactory.decodeResource(getResources(), pizzaTextureID);
            objects.add(new ToDraw(pizzaTexture, 0, 0, 0, burgerRunningTime, true, 0));
        }

        /**
         * tiling and draw
         *
         * @param bmp
         * @param canvas
         */
        private void tilingAndDraw(Bitmap bmp, Canvas canvas) {
            int x = 0, y = 0;
            //we use tiling for background pictures that are too big
            while (x < width && y < height) {
                canvas.drawBitmap(backgroundImage, x, y, p);
                //draw a column
                if (y + backgroundImage.getHeight() < height) {
                    y += backgroundImage.getHeight();
                    continue;
                }
                //next column
                y = 0;
                if (x + backgroundImage.getWidth() < width) {
                    x += backgroundImage.getWidth();
                    continue;
                }
                //nothing to do anymore
                break;
            }
        }

        /**
         * Draw
         */
        private void draw() {
            long t = System.currentTimeMillis();
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                canvas.drawColor(backgroundColor);
                if (useBackgroundImage) {
                    tilingAndDraw(backgroundImage, canvas);
                }
                for (ToDraw actual : objects) {
                    doubles.clear();
                    moveObject(actual, t - time);
                    //draw the doubles before the reel objects, to keep the screen from flashing!
                    for (ToDraw td : doubles) {
                        drawOnCanvas(td, canvas);
                    }
                    drawOnCanvas(actual, canvas);
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas);
                }
            }
            time = t;
            handler.removeCallbacks(drawRunner);
            if (visibility) {
                handler.postDelayed(drawRunner, sleepBetweenRedraws);
            }
        }

        //TODO : add something to rotate textures before
        public void drawOnCanvas(ToDraw actual, Canvas canvas) {
            //this gets the source rectangle. trust my strange calculations :P
            Rect source = new Rect(-Math.min(actual.getX(), 0), -Math.min(0, actual.getY()), actual.getWidth(), actual.getHeight());
            //this gets the destination of the rectangle
            Rect destination = new Rect(source);
            destination.offsetTo(Math.max(actual.getX(), 0), Math.max(actual.getY(), 0));

//            Picture pic = new Picture();
//            Canvas tempc = pic.beginRecording(width, height);
//            tempc.rotate(-30, destination.centerX(),destination.centerY());
//            tempc.drawBitmap(actual.getTexture(),0,0,p);
//            tempc.drawBitmap(actual.getTexture(), source, destination, p);
            //tempc.rotate(30, destination.centerX(),destination.centerY());
            //pic.endRecording();
            //pic.draw(canvas);

            canvas.drawBitmap(actual.getTexture(), source, destination, p);
        }


        /**
         * Event - onVisibilityChanged
         *
         * @param visible
         */
        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visibility = visible;
            if (visible) {
                handler.post(drawRunner);
            } else {
                handler.removeCallbacks(drawRunner);
            }
        }

        /**
         * Event - onTouchEvent
         *
         * @param event
         */
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

        /**
         * run away from Finger
         *
         * @param td
         * @param event
         */
        private void runAwayFromFinger(ToDraw td, MotionEvent event) {
            td.setCurrentMovementTime(0);
            td.setMaxMovementTime(burgerRunningTime);
            int dx = Math.round(td.getX() - event.getX());
            int dy = Math.round(td.getY() - event.getY());

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
            td.resetMultipliers();
            td.setxVec(new Lambda() {
                @Override
                public int l(long x) {
                    return a;
                }
            });
            td.setyVec(new Lambda() {
                @Override
                public int l(long x) {
                    return b;
                }
            });
        }

        /**
         * Let the objects rain down (to be put in the draw method)
         *
         * @param td
         */
        private void raining(ToDraw td) {
            raining(td, 0.0, -1.0);
        }

        /**
         * Let the objects rain in the giving vector direction (to be put in the draw method)
         *
         * @param td
         * @param dirX
         * @param dirY
         */
        private void raining(ToDraw td, double dirX, double dirY) {
            //check if not  in screen
            if (!(td.getX() > 0 && td.getX() < width && td.getY() > 0 && td.getY() < height)) {
                td.setX(0);
                //TODO : continue
            }
        }

        /**
         * onSurfaceDestroyed
         *
         * @param holder
         */
        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            this.visibility = false;
            handler.removeCallbacks(drawRunner);
        }

        /**
         * onSurfaceChanged
         *
         * @param holder
         * @param format
         * @param width
         * @param height
         */
        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format,
                                     int width, int height) {
            this.width = width;
            this.height = height;
            super.onSurfaceChanged(holder, format, width, height);
        }
    }


}
