package org.sasehash.burgerwp;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private static final int intentID = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    //preview the wallpaper and the possibility to set it as current wallpaper
    public void startPreview(View view) {
        Intent intent = new Intent(
                WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
        intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                new ComponentName(this, JumpingBurger.class));
        startActivity(intent);
    }

    //choose bgImage
    public void chooseImage(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, intentID);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == intentID) {
            if (resultCode == RESULT_OK) {
                //this intent contains the new wallpaper!
                //put it in the settings
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = settings.edit();
                //enable picture as wallpaper
                editor.putBoolean("pref_bg_color_or_bg_image", true);
                //put filepath of picture at the right place
                editor.putString("pref_bg_image", data.getDataString());
                editor.apply();
                if (!settings.contains("pref_bg_image")) {
                    throw new IllegalStateException("Could not set image to pref_bg_image");
                }
            } else {
                throw new IllegalStateException("Could not chose image");
            }
        } else {
            throw new IllegalStateException("Got an intent that wasn't for me");
        }
    }

    //start settings activity
    public void startSettings(View view) {
        startActivity(new Intent(this, SettingsActivity.class));
    }
}
