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
        private List<IDrawable> IDrawableObjects = new ArrayList<>();
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
                    //ToDraw td = new ToDraw(texture, x, y, actualTime, totalTime, selfDestroy, bouncing, speed, rotation, scalingFactor, runsAway);
                    //TODO : rework this
                    SimpleDrawable td = new SimpleDrawable(texture, getBaseContext(), x, y, rotation);
                    IDrawableObjects.add(td);
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
            for (IDrawable id : IDrawableObjects) {
                id.event(event);
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
