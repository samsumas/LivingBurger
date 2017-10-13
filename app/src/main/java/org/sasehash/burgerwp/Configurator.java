/*
 * Licensed under GPL 3.0
 */

package org.sasehash.burgerwp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by sami on 13/10/17.
 */

/**
 * Activity used for creating a config
 */

public class Configurator extends AppCompatActivity {

    private TableLayout v;
    private SharedPreferences.Editor newSettings;
    private String[] prefvalues = new String[]{
            "count",
            "isExternalResource",
            "image",
            "x",
            "y",
            "actualTime",
            "totalTime",
            "selfDestroy",
            "bouncing",
            "speed",
            "rotation",
            "scalingFactor"
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        newSettings = PreferenceManager.getDefaultSharedPreferences(this).edit();
        //this gonna be complicated, but you have a tree here :
        // linearlayout
        // |--scrolling settingpanel
        // |     |--table with settings
        // |--Buttons (in a Row) (apply reset default, export, import, add a row, remove a row etc...)
        HorizontalScrollView scroller = new HorizontalScrollView(this);
        v = new TableLayout(this);
        scroller.addView(v);
        createTable(this.v);


        LinearLayout superLayout = new LinearLayout(this);
        superLayout.setOrientation(LinearLayout.VERTICAL);
        superLayout.addView(scroller);
        View buttons = new View(this);
        buttons.inflate(this, R.layout.buttons, superLayout);
        setContentView(superLayout);
    }

    //functions called when buttons are pressed
    public void cancelChanges(View v) {
        startActivity(new Intent(this, MainActivity.class));
    }

    public void applyChanges(View v) {
        newSettings.apply();
        //close after applying
        cancelChanges(v);
    }

    /**
     * Good time to define how the config should be saved
     * there is a "objects" key that contains a set with all the key with entries
     * One Entry contains :
     * /!\ example for accessing count : settings.getBoolean("nameofentry_count","");
     *
     * count (eg 5 to draw this object 5 times)
     * isExternalResource
     * image
     * x
     * y
     * actualTime
     * totalTime
     * selfDestroy
     * bouncing
     * speed
     * rotation
     * scalingFactor
     */
    /**
     * the authentic wallpaper
     *
     * @param v
     */
    public void resetConfig(View v) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> deleteMe = settings.getStringSet("objects", null);

        //delete old preference
        if (deleteMe != null) {
            for (String s : deleteMe) {
                for (String curr : prefvalues) {
                    newSettings.remove(s + "_" + curr);
                }
            }
        }

        //set new Preferences
        String[] burgerOptions = new String[]{
                "20", "false", Integer.toString(R.drawable.burger), "0", "0", "0", "-1", "false", "true", "5", "0", "1.0"
        };
        String[] pizzaOptions = new String[]{
                "20", "false", Integer.toString(R.drawable.pizza), "0", "0", "0", "-1", "false", "false", "5", "180", "1.0"
        };
        Set<String> addMe = new HashSet<String>();
        addMe.add("burger");
        addMe.add("pizza");
        newSettings.putStringSet("objects", addMe);
        for (int i = 1; i < prefvalues.length; i++) {
            newSettings.putString("burger_" + prefvalues[i], burgerOptions[i]);
            newSettings.putString("pizza_" + prefvalues[i], pizzaOptions[i]);
        }
        newSettings.apply();
    }

    /**help :
     * Contructor for a ToDraw. Note that you need to set xvec,yvec and rvec if you want your object to move!
     *
     * @param texture
     * @param x
     * @param y
     * @param currentMovementTime
     * @param maxMovementTime
     * @param selfDestroy
     * @param bouncing
     * @param speed
     * @param rotation
     * @param scaler
     */

    /**
     * adds the standard Header to current TableLayout, which may look like this:
     * bitmap,xpos,ypos,....and so on
     *
     * @return
     */
    private void addHeader(TableLayout v) {
        TableRow header = new TableRow(this);
        String[] options = new String[]{
                "Image", "x-Position", "y-Position", "actualTime(Default :0)", "TotalTime(Default :true)", "SelfDestroy(Default :false)",
                "bouncing(Default :-1)", "speed(Default :5)", "rotation(Default :0)", "scalingTo(Default: 1.0)"
        };
        for (String s : options) {
            TextView tv = new TextView(this);
            tv.setText(s);
            header.addView(tv);
        }
        v.addView(header);
    }

    private void addHeader() {
        addHeader(this.v);
    }

    private void createTable(TableLayout v) {
        addHeader(v);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> rows = settings.getStringSet("objects", null);
        if (rows == null) {
            resetConfig(v);
            rows = settings.getStringSet("objects", null);
            if (rows == null) {
                throw new IllegalStateException("Settings not existing and generating new settings didn't work");
            }
        }

        for (String s : rows) {
            TableRow current = new TableRow(this);
            //TODO : add image
            for (String curr : prefvalues) {
                EditText et = new EditText(this);
                et.setText(settings.getString(s + "_" + curr, ""));
                current.addView(et);
            }
            v.addView(current);
        }
    }
}
