package org.sasehash.burgerwp;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.io.FileNotFoundException;
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
            checkValue("pref_run_away", settings);

            runAway = settings.getBoolean("pref_run_away", false);
            useBackgroundImage = settings.getBoolean("pref_bg_color_or_bg_image", false);
            backgroundColor = Color.parseColor(settings.getString("bg_color", "black"));
            if (useBackgroundImage) {
                try {
                    checkValue("pref_bg_image", settings);
                    String filename = settings.getString("pref_bg_image", null);
                    if (filename == null) {
                        throw new IllegalStateException("Failed to get ImageName with intent");
                    }
                    if (filename.equals("abc")) {
                        throw new IllegalStateException("Got Standard (invalid) filename");
                    }
                    Uri uri = Uri.parse(filename);
                    InputStream is = getContentResolver().openInputStream(uri);
                    if (is == null) {
                        throw new IllegalStateException("Could not open Background!");
                    }
                    backgroundImage = BitmapFactory.decodeStream(is);
                    try {
                        is.close();
                    } catch (Exception e) {
                        //well don't care about it
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    useBackgroundImage = false; //continue with color instead of background
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

        private void checkValue(String a, SharedPreferences settings) {
            if (!settings.contains(a)) {
                throw new IllegalStateException("Cannot read " + a + " Settings from anim_preferences!");
            }
        }

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
                } else {
                    //TODO function "going from left to right" is broken. will fix this another day
                    //objects.remove(td);
                    resetOnScreen(td);
                }
            }

        }

        public void moveObjects(long t) {
            for (ToDraw td : objects) {
                moveObject(td, t);
            }
        }

        private int modulo(int a, int m) {
            while (a < 0) {
                a += m;
            }
            while (a > m) {
                a -= m;
            }
            return a;
        }


        //NO ESCAPE FROM THIS SCREEN!
        private void resetOnScreen(ToDraw td) {
            //still in pic range ?
            // if ((td.getX() > -td.getWidth() || td.getX() > width - td.getWidth()) && (td.getY() > -td.getHeight() && td.getY() < height - td.getHeight())) {
            //checks if in the "needs double" margin
            //if (modulo(td.getX(),width)>width-td.getWidth() || modulo(td.getY(),height)>height-td.getHeight()) {
            //    doubles.add(new ToDraw(td.getTexture(), modulo(td.getX(), width), modulo(td.getY(), height), td.getCurrentMovementTime(), td.getMaxMovementTime(), td.getSelfDestroy(), td.getBouncing()));
            //} else {
            //first case : negative coordinates
            List<ToDraw> doublesToAdd = new ArrayList<>();
            if (td.getX() < 0 && td.getX() > -td.getWidth()) {
                ToDraw t = new ToDraw(td);
                t.setX(td.getX() + width);
                doublesToAdd.add(t);
            }
            if (td.getY() < 0 && td.getHeight() > -td.getHeight()) {
                ToDraw t = new ToDraw(td);
                t.setY(td.getY() + height);
                doublesToAdd.add(t);
            }
            if (td.getX() + td.getWidth() > width && td.getX() < width) {
                ToDraw t = new ToDraw(td);
                t.setX(td.getX() - width);
                doublesToAdd.add(t);
            }
            if (td.getY() + td.getHeight() > height && td.getY() < height) {
                ToDraw t = new ToDraw(td);
                t.setY(td.getY() - height);
                doublesToAdd.add(t);
            }
            if (doublesToAdd.isEmpty()) {
                td.setX(modulo(td.getX(), width));
                td.setY(modulo(td.getY(), height));
            } else {
                doubles.addAll(doublesToAdd);
            }
        }

        private void bounce(ToDraw td) {
            if (!isOnScreenX(td)) {
                td.bounceX();
            } else {
                td.bounceY();
            }
        }

        //bounce ^^
        private boolean isOnScreen(ToDraw td) {
            return isOnScreenX(td) && isOnScreenY(td);
        }

        private boolean isOnScreenY(ToDraw td) {
            return td.getY() == modulo(td.getY(), height - td.getHeight());
        }

        private boolean isOnScreenX(ToDraw td) {
            return td.getX() == modulo(td.getX(), width - td.getWidth());
        }

        private void spawnPizza() {
            spawnPizza(1);
        }

        private void spawnPizza(int i) {
            Bitmap pizzaTexture = BitmapFactory.decodeResource(getResources(), pizzaTextureID);
            objects.add(new ToDraw(pizzaTexture, 0, 0, 0, burgerRunningTime, true, 0));
        }

        private void draw() {
            long t = System.currentTimeMillis();
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                canvas.drawColor(backgroundColor);
                if (useBackgroundImage) {
                    canvas.drawBitmap(backgroundImage, 0, 0, p);
                }

                doubles.clear();
                for (ToDraw actual : objects) {
                    moveObject(actual, t - time);
                    drawOnCanvas(actual, canvas);
                }
                for (ToDraw actual : doubles) {
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

        public void drawOnCanvas(ToDraw actual, Canvas canvas) {
            //this gets the source rectangle. trust my strange calculations :P
            Rect source = new Rect(-Math.min(actual.getX(), 0), -Math.min(0, actual.getY()), actual.getWidth(), actual.getHeight());
            //this gets the destination of the rectangle
            Rect destination = new Rect(source);
            destination.offsetTo(Math.max(actual.getX(), 0), Math.max(actual.getY(), 0));
            canvas.drawBitmap(actual.getTexture(), source, destination, p);
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
            //TODO : make the object move in (a,b) !
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
         */
        private void raining(ToDraw td) {
            raining(td, 0.0, -1.0);
        }

        /*
         * Let the objects rain in the giving vector direction (to be put in the draw method)
         */
        private void raining(ToDraw td, double dirX, double dirY) {
            //check if not  in screen
            if (!(td.getX() > 0 && td.getX() < width && td.getY() > 0 && td.getY() < height)) {
                td.setX(0);
                //TODO : continue
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
