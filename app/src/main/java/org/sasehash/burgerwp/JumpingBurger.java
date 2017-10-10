package org.sasehash.burgerwp;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
        private final int burgerRunningTime = 3000;
        private final int heartRunningTime = 1500;
        private final int burgerTextureID = R.drawable.burger;
        private final int heartTextureID = R.drawable.heart;
        private final int pizzaTextureID = R.drawable.pizza;
        private final int backgroundColor;
        private final int burgerCount = 1;
        /* Values needed internally */
        private Handler handler = new Handler();
        private boolean visibility = true;
        private long time;
        private Paint p = new Paint();
        private int width, height;
        private List<ToDraw> objects = new ArrayList<>();
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
                objects.add(new ToDraw(burgerTexture, 0, 0, new Movement() {
                    @Override
                    public int moveX(long time) {
                        return burgerSpeed - curr;
                    }

                    @Override
                    public int moveY(long time) {
                        return curr;
                    }
                }, 0, burgerRunningTime));
            }
        }

        private void checkValue(String a, SharedPreferences settings) {
            if (!settings.contains(a)) {
                throw new IllegalStateException("Cannot read " + a + " Settings from anim_preferences!");
            }
        }

        private void spawnPizza() {
            Bitmap pizzaTexture = BitmapFactory.decodeResource(getResources(), pizzaTextureID);
            objects.add(new ToDraw(pizzaTexture, width / 2, height / 2, new Movement() {
                //TODO: add better movementvalues
                @Override
                public int moveX(long time) {
                    return 1;
                }

                @Override
                public int moveY(long time) {
                    return 1;
                }
            }, 0, burgerRunningTime));
        }

        private void spawnHearts(int i) {
            //TODO : recheck if "decoding the resource each time it is needed" is a good idea
            Bitmap heartTexture = BitmapFactory.decodeResource(getResources(), heartTextureID);
            for (int j = 0; j < i; j++) {
                //get random location near burger
                final int sizeOfBurger = 50;
                int spawnAtX = objects.get(0).getX() + (int) (sizeOfBurger * Math.random()) + sizeOfBurger / 2;
                int spawnAtY = objects.get(0).getY() + (int) (sizeOfBurger * Math.random()) + sizeOfBurger / 2;
                //we don't want all the hearts to go in the same directions
                final double a = Math.random() * 2 - 1;
                final double b = Math.random() * 2 - 1;
                objects.add(new ToDraw(heartTexture, spawnAtX, spawnAtY, new Movement() {
                    @Override
                    public int moveX(long time) {
                        return (int) (a * heartSpeed * Math.sin(time));
                    }

                    @Override
                    public int moveY(long time) {
                        return (int) (b * heartSpeed * Math.cos(time));
                    }
                }, 0, heartRunningTime, true));
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
                if (useBackgroundImage) {
                    //TODO : draws background and then bitmap over it
                    canvas.drawBitmap(backgroundImage, 0, 0, p);
                }

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
