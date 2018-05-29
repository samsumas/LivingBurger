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

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class JumpingBurger extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new JumpingEngine();
    }

    private class JumpingEngine extends Engine {
        private final double NEARLY_ZERO = 0.1;
        /* Values that can be tweaked */
        private final int backgroundColor;
        private final  static int SLEEP_BETWEEN_TWO_FRAMES = 35;
        private Handler handler = new Handler();
        private boolean visibility = true;
        private long time;
        private Paint p = new Paint();
        private int width, height;
        private List<ToDraw> objects = new ArrayList<>();
        private List<IDrawable> IDrawableObjects = new ArrayList<>();
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
            /* Load values from preferences */
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(JumpingBurger.this);
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
                        //close inputstream
                        if (is != null) {
                            try {
                                is.close();
                            } catch (IOException e) {
                                //well don't care about it, can't close what isn't opened lol
                            }
                        }
                    } catch (java.lang.SecurityException e) {
                        //FIXME

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
            //load all objects from config
            loadConfig();
        }

        /**
         * stopUsingBackgroundImage
         */
        private void stopUsingBackgroundImage() {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(JumpingBurger.this).edit();
            editor.putBoolean("pref_bg_color_or_bg_image", false);
            editor.apply();
            useBackgroundImage = false;
        }

        /**
         * Loads config from sharedpreferences into the engine.
         */
        private void loadConfig() {
            //TODO : remove me (only used for testing)
            IDrawableObjects.add(new SimpleDrawable(getBaseContext()));
            //end of removeme

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(JumpingBurger.this);
            Set<String> objectNames = settings.getStringSet("objects", null);
            if (objectNames == null) {
                Configurator.resetConfig(JumpingBurger.this, settings.edit());
                objectNames = settings.getStringSet("objects", null);
            }
            for (String s : objectNames) {
                Bitmap texture = loadImage(settings, s);
                int count = Integer.parseInt(settings.getString(s + "_count", "1"));

                int x = Integer.parseInt(settings.getString(s + "_x", "0"));
                int y = Integer.parseInt(settings.getString(s + "_y", "0"));
                long actualTime = Long.parseLong(settings.getString(s + "_actualTime", "0"));
                long totalTime;
                totalTime = Long.parseLong(settings.getString(s + "_totalTime", "0"));
                if (totalTime < 0) {
                    //totalTime = -1 is a synonym for infinity (easier to type then typing 99999999999999999999 and trying avoiding overflows)
                    totalTime = Long.MAX_VALUE;
                }
                boolean selfDestroy = Boolean.parseBoolean(settings.getString(s + "_selfDestroy", "false"));
                boolean bouncing = Boolean.parseBoolean(settings.getString(s + "_bouncing", "false"));
                int speed = Integer.parseInt(settings.getString(s + "_speed", "0"));
                float rotation = Float.parseFloat(settings.getString(s + "_rotation", "0"));
                float scalingFactor = Float.parseFloat(settings.getString(s + "_scalingFactor", "1"));
                boolean runsAway = Boolean.parseBoolean(settings.getString(s + "_runsAway", "true"));
                String c = ";";
                for (int i = 0; i < count; i++) {
                    ToDraw td = new ToDraw(texture, x, y, actualTime, totalTime, selfDestroy, bouncing, speed, rotation, scalingFactor, runsAway);
                    final int abc = count;
                    final int abc2 = i;
                    objects.add(td);
                    td.setxVec(new Lambda() {
                        @Override
                        public int l(long x) {
                            return abc - abc2;
                        }
                    });
                    td.setyVec(new Lambda() {
                        @Override
                        public int l(long x) {
                            return abc2;
                        }
                    });
                    td.setrVec(new Lambda() {
                        @Override
                        public int l(long x) {
                            return 1;
                        }
                    });
                }
            }
        }


        private Bitmap loadImage(SharedPreferences settings, String s) {
            Bitmap texture;
            try {
                //load externalResource
                if (Boolean.parseBoolean(settings.getString(s + "_isExternalResource", "false"))) {
                    texture = BitmapFactory.decodeFile(settings.getString(s + "_image", ""));
                } else {
                    String id = settings.getString(s + "_image", null);
                    texture = BitmapFactory.decodeResource(getResources(), Integer.parseInt(id));
                }
            } catch (Exception e) {
                //when failling to load texture, use the burger one
                //FIXME
                texture = BitmapFactory.decodeResource(getResources(), R.drawable.burger);
                e.printStackTrace();
            }
            return texture;
        }

        /** loads an image from a URI, copy pasted from api doc
         *
         * @param uri the uri of image
         * @return image, the image as bitmap
         * @throws IOException when something failed
         */
        private Bitmap getBitmapFromUri(Uri uri) throws IOException {
            ParcelFileDescriptor parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(uri, "r");
            if (parcelFileDescriptor == null) {
                throw new IOException("FileDescriptor broken!");
            }
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();
            return image;
        }

        /**
         * checkValue, throws an error if value is not set
         *
         * @param a key of value
         * @param settings the actual settings
         */
        private void checkValue(String a, SharedPreferences settings) {
            if (!settings.contains(a)) {
                throw new IllegalStateException("Cannot read " + a + " Settings from anim_preferences!");
            }
        }

        /**
         * moveObject, moves a ToDraw according to his xVec/yVec/rVec
         *
         * @param td actual object that has to be moved (or not)
         * @param t time elapsed since last movement
         */
        public void moveObject(ToDraw td, long t) {
            long dt = t + td.getCurrentMovementTime();
            if (!td.timeLeft() || td.isVecNull()) {
                return;
            }
            td.addTo(td.getxVec(t), td.getyVec(t));
            td.addTo(td.getrVec(t));
            td.setCurrentMovementTime(dt);
            float rotationBackup = td.getRotation();
            if (!isOnScreen(td)) {
                if (td.getBouncing()) {
                    bounce(td);
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
                    resetOnScreen(td);
                }
            }

            if (Math.abs(rotationBackup - td.getRotation()) > NEARLY_ZERO) {
                throw new IllegalStateException("Rotation changed, from " + rotationBackup + " to " + td.getRotation());
            }

        }

        /**
         * Move all object with moveObject
         *
         * @param t
         */
        public void moveObjects(long t) {
            for (ToDraw td : objects) {
                moveObject(td, t);
            }
        }

        /**
         * Util module positiv
         *
         * @param a
         * @param m
         * @return
         */
        private int modulo(int a, int m) {
            return (a % m + m) % m;
        }

        /**
         * inCorner
         *
         * @param td
         * @return
         */
        private boolean inCorner(ToDraw td) {
            return (td.getX() < 0 || td.getX() + td.getWidth() > width)
                    && (td.getY() < 0 || td.getY() + td.getHeight() > height);
        }

        /**
         * outOfScreen
         *
         * @param td
         * @return
         */
        private boolean outOfScreen(ToDraw td) {
            return ((td.getX() < 0 && td.getX() + td.getWidth() > 0) || (td.getX() + td.getWidth() >= width && td.getX() <= width))
                    || ((td.getY() < 0 && td.getHeight() + td.getHeight() > 0) || (td.getY() + td.getHeight() >= height && td.getY() <= height));
        }

        /**
         * completyOutOfScreen
         *
         * @param td
         * @return
         */
        private boolean completelyOutOfScreen(ToDraw td) {
            return (td.getX() + td.getWidth() < 0 || td.getX() > width)
                    || (td.getY() + td.getHeight() < 0 || td.getY() > height);
        }

        /**
         * rectifyX
         *
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
         *
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
         *
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
         *
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
         * tiling and draw
         *
         * @param bmp
         * @param canvas
         */
        private void tilingAndDraw(Bitmap bmp, Canvas canvas) {
            int x = 0, y = 0;
            //we use tiling for background pictures that are too small
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
                if (useBackgroundImage) {
                    tilingAndDraw(backgroundImage, canvas);
                } else {
                    canvas.drawColor(backgroundColor);
                }

                //new implementation using interfaces to make writing code easier
                for (IDrawable id : IDrawableObjects) {
                    id.draw(canvas);
                }

                for (ToDraw actual : objects) {
                    doubles.clear();
                    moveObject(actual, t - time);
                    //draw the doubles before the reel objects, to keep the screen from flashing!
                    for (ToDraw td : doubles) {
                        canvas.drawBitmap(td.getTexture(), td.getManipulation(), p);
                    }
                    canvas.drawBitmap(actual.getTexture(), actual.getManipulation(), p);
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas);
                }
            }
            time = t;
            handler.removeCallbacks(drawRunner);
            if (visibility) {
                handler.postDelayed(drawRunner, SLEEP_BETWEEN_TWO_FRAMES);
            }
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
            int dx = Math.round(td.getX() - event.getX());
            int dy = Math.round(td.getY() - event.getY());

            //this android runs from humans away, depending on the runAway value
            double size = Math.sqrt(dx * dx + dy * dy);
            double vecX = ((double) dx) / size;
            vecX *= td.getSpeed();
            double vecY = ((double) dy) / size;
            vecY *= td.getSpeed();
            if (!td.isRunsAway()) {
                vecX = -vecX;
                vecY = -vecY;
            }
            final int a = (int) Math.round(vecX);
            final int b = (int) Math.round(vecY);
            td.resetMult();
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
            td.setrVec(new Lambda() {
                @Override
                public int l(long x) {
                    return 1;
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
