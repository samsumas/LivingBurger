/*
 * Licensed under GPL 3.0
 */

package org.sasehash.burgerwp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import static org.sasehash.burgerwp.Type.BOOL;
import static org.sasehash.burgerwp.Type.FLOAT;
import static org.sasehash.burgerwp.Type.IMAGE;
import static org.sasehash.burgerwp.Type.INT;
import static org.sasehash.burgerwp.Type.LONG;

/**
 * Created by sami on 13/10/17.
 */

/**
 * Activity used for creating a config
 */

public class Configurator extends AppCompatActivity {
    private TableLayout v;
    private SharedPreferences.Editor newSettings;
    private static ArrayList<String> intentKeys = new ArrayList<>();
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
    //prefvalues[i] has the type prefvaluesType[i]
    private Type[] prefvaluesType = new Type[]{
            INT, BOOL, IMAGE, INT, INT, LONG, LONG, BOOL, BOOL, INT, FLOAT, FLOAT
    };
    private final int importIntentID = 703;

    //the image requested
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (importIntentID == requestCode) {
                importChanges(data);
            } else {
                String helper = intentKeys.get(requestCode);
                intentKeys.remove(requestCode);
                newSettings.putString(helper + "_image", data.getDataString());
                newSettings.putString(helper + "_isExternalResource", "true");
            }
        }
    }

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
        View.inflate(this, R.layout.buttons, superLayout);
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

    public void importChanges(Intent intent) {
        //FIXME this doesn't work
        try {
            InputStream inputStream = getContentResolver().openInputStream(intent.getData());
            if (inputStream ==null) {
                throw new IllegalStateException("InputStream is Null!");
            }
            Scanner scanner = new Scanner(inputStream);
            //delimiter : semicolons
            scanner.useDelimiter(";");
            newSettings.clear();
            Set<String> keys = new HashSet<>();
            while (scanner.hasNext()) {
                String key = scanner.next();
                keys.add(key);
                System.err.append("key :"+key);
                for (String curr : prefvalues) {
                    String read = scanner.next();
                    System.err.append("just got "+read);
                    newSettings.putString(key + "_" + curr, read);
                }
            }
            inputStream.close();
            scanner.close();
            //put the keys in the thingie
            newSettings.putStringSet("objects", keys);
            //reload activity
            startActivity(new Intent(this, this.getClass()));
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Could not read File!", Toast.LENGTH_SHORT).show();
        }

    }

    public void importChanges(View v) {
        //send out an Intent!
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, importIntentID);
    }

    public void exportChanges(View v) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> objectNames = settings.getStringSet("objects", null);
        StringBuilder output = new StringBuilder();
        char c = ';';

        if (objectNames == null) {
            throw new IllegalStateException("Could not read config!");
        }
        for (String s : objectNames) {
            doubleAppend(output, s);
            for (String curr: prefvalues) {
                doubleAppend(output, settings.getString(s+"_"+curr,"0"));
            }
            output.append('\n');
        }
        //TODO:ask the user for a destination
        String timeStamp = new java.util.Date().toString();
        String fileName = "customConfigLivingBurger" + timeStamp + ".csv";
        fileName = fileName.replace(':', '.');
        File exportDestination = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), fileName);
        try {
            FileWriter writer = new FileWriter(exportDestination);
            writer.write(output.toString());
            writer.close();
            Toast.makeText(this, "Wrote File to " + exportDestination.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error while writing File to " + exportDestination.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

    }

    private void doubleAppend(StringBuilder s, String s2) {
        s.append(s2);
        s.append(';');
    }


    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
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
        for (int i = 0; i < prefvalues.length; i++) {
            newSettings.putString("burger_" + prefvalues[i], burgerOptions[i]);
            newSettings.putString("pizza_" + prefvalues[i], pizzaOptions[i]);
        }
        newSettings.apply();
        //restart activity
        startActivity(new Intent(this, Configurator.class));
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
        //TODO : repalce options with better names
        String[] options = prefvalues;
        for (String s : options) {
            TextView tv = new TextView(this);
            tv.setText(s);
            tv.setGravity(View.TEXT_ALIGNMENT_CENTER);
            header.addView(tv);
        }
        v.addView(header);
    }

    private void addHeader() {
        addHeader(this.v);
    }

    private CompoundButton.OnCheckedChangeListener generateToggleButtonListener(final String s, final int i) {
        return new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                newSettings.putString(s + "_" + prefvalues[i], Boolean.toString(isChecked));
            }
        };
    }

    private TextWatcher generateEditTextListener(final String str, final int i) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //don't do anything
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //don't do anything
            }

            @Override
            public void afterTextChanged(Editable s) {
                String checkedValue = null;
                try {
                    switch (prefvaluesType[i]) {
                        case BOOL:
                            //bools doesn't use this method
                            break;
                        case FLOAT:
                            checkedValue = Float.toString(Float.parseFloat(s.toString()));
                            break;
                        case IMAGE:
                            //TODO : check if value is valid
                            break;
                        case INT:
                            checkedValue = Integer.toString(Integer.parseInt(s.toString()));
                            break;
                        case LONG:
                            checkedValue = Long.toString(Long.parseLong(s.toString()));
                            break;
                        default:
                            throw new IllegalStateException("Maybe you forgot to implement something");
                    }
                } catch (Exception e) {
                    Toast error = Toast.makeText(Configurator.this, "Incorrect value, must be " + prefvaluesType[i], Toast.LENGTH_SHORT);
                    error.show();
                    //bools doesn't use this method!
                    s = Editable.Factory.getInstance().newEditable("0");

                    return;
                }
                //put it in the editor
                if (checkedValue != null) {
                    newSettings.putString(str + "_" + prefvalues[i], checkedValue);
                } else {
                    newSettings.putString(str + "_" + prefvalues[i], s.toString());
                }
            }
        };
    }


    private void createTable(TableLayout v) {
        addHeader(v);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> rows = settings.getStringSet("objects", null);
        //a e s t h e t i c s
        v.setPadding(5, 5, 5, 5);
        v.setStretchAllColumns(true);
        //
        if (rows == null) {
            resetConfig(v);
            rows = settings.getStringSet("objects", null);
            if (rows == null) {
                throw new IllegalStateException("Settings not existing and generating new settings didn't work");
            }
        }
        int intentCounterHelper = -1;
        for (String s : rows) {
            final String helper = s;
            TableRow current = new TableRow(this);
            current.setGravity(View.TEXT_ALIGNMENT_CENTER);
            for (int i = 0; i < prefvalues.length; i++) {
                if (prefvaluesType[i] == BOOL) {
                    Switch tb = new Switch(this);
                    tb.setChecked(Boolean.parseBoolean(settings.getString(s + "_" + prefvalues[i], "false")));
                    tb.setOnCheckedChangeListener(generateToggleButtonListener(s, i));
                    current.addView(tb);
                    continue;
                }
                if (prefvaluesType[i] == IMAGE) {
                    ImageButton ib = new ImageButton(this);
                    try {
                        int id = Integer.parseInt(settings.getString(s + "_" + prefvalues[i], ""));
                        ib.setImageBitmap(BitmapFactory.decodeResource(getResources(), id));
                    } catch (Exception e) {
                        //maybe it was an uri
                        e.printStackTrace();
                        try {
                            ib.setImageBitmap(getBitmapFromUri(Uri.parse(settings.getString(s + "_" + prefvalues[i], ""))));
                        } catch (Exception e2) {
                            //ok use the burger
                            e2.printStackTrace();
                            ib.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.burger));
                        }
                    }
                    ib.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            intentKeys.add(helper);
                            startActivityForResult(intent, intentKeys.size() - 1);
                        }
                    });
                    current.addView(ib);
                    continue;
                }
                String curr = prefvalues[i];
                EditText et = new EditText(this);
                et.setText(settings.getString(s + "_" + curr, ""));
                et.addTextChangedListener(generateEditTextListener(curr, i));
                switch (prefvaluesType[i]) {
                    case INT:
                    case LONG:
                        final int numbersOnly = 0x1002;
                        et.setInputType(numbersOnly);
                        break;
                    case FLOAT:
                        final int floatNumbersOnly = 0x2002;
                        et.setInputType(floatNumbersOnly);
                        break;
                    case BOOL:
                        break;
                    case IMAGE:
                        break;
                }
                current.addView(et);
            }
            v.addView(current);
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }
}
