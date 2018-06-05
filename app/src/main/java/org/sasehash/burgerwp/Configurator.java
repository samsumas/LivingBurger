/*
 * Licensed under GPL 3.0
 */

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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import static org.sasehash.burgerwp.Type.BOOL;
import static org.sasehash.burgerwp.Type.IMAGE;

/**
 * Activity used for creating a config
 */

public class Configurator extends AppCompatActivity {
    private TableLayout tabelle;
    private static SharedPreferences.Editor newSettings;
    private static SparseArray<String> intentKeys = new SparseArray<>();
    private static SparseArray<ImageView> buttonKeys = new SparseArray<>();
    private static int actualIntentKeysID = 500;
    private ArrayList<View> rowsList = new ArrayList<>();
//    public final static String[] preconfigurated = new String[]{
//            "standard",
//            "christmas",
//    };

    public ArrayList<String> prefvalues;
    public ArrayList<Type> prefvaluesType;

    //TODO : recheck
    public static ArrayList<String> getPrefvalues(Context context) {
        return new ArrayList<>(Arrays.asList(context.getResources().getStringArray(R.array.configuratorParametersName)));
    }

    /**
     * the authentic wallpaper
     */
    public static void resetConfig(Context c, SharedPreferences.Editor edit) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
        Set<String> deleteMe = settings.getStringSet("objects", new HashSet<String>());
        ArrayList<String> prefvalues = getPrefvalues(c);

        //delete old preference
        for (String s : deleteMe) {
            for (String curr : prefvalues) {
                edit.remove(s + "_" + curr);
            }
        }

        //contains name of object in first position
        String[] burgerOptions = c.getResources().getStringArray(R.array.configuratorParameterStandardBurgerValue);

        HashSet<String> set = new HashSet<String>();
        set.add(burgerOptions[0]);
        edit.putStringSet("objects", set);

        for (int i = 0; i < prefvalues.size(); i++) {
            edit.putString(burgerOptions[0] + "_" + prefvalues.get(i), burgerOptions[i + 1]);
        }
        //make sure the images are correct
        edit.putString(burgerOptions[0] + "_" + "image", Integer.toString(R.drawable.burger));
        edit.apply();
    }
    private final int importIntentID = 703;

    //the image requested

    /**
     * Is called (from the api) when the applications gets data (images, files) from the User with the OS-picker.
     *
     * @param requestCode id of intent
     * @param resultCode  successful or not
     * @param data        the requested data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (importIntentID == requestCode) {
                importChanges(data);
            } else if (intentKeys.get(requestCode) != null) {
                try {
                    String actualKey = intentKeys.get(requestCode);

                    //try to set the image in settings + in the button, it works (^o^)
                    Bitmap b = getBitmapFromUri(data.getData(), getApplicationContext());
                    File localBitmap = backupBitmapFromBitmap(b, actualKey, getApplicationContext());
                    buttonKeys.get(requestCode).setImageBitmap(b);
                    newSettings.putString(actualKey + "_image", localBitmap.getAbsolutePath());
                    newSettings.putString(actualKey + "_isExternalResource", "true");
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error while loading selected Image", Toast.LENGTH_SHORT).show();
                } finally {
                    //remove now unneeded values from the arrays
                    intentKeys.remove(requestCode);
                    buttonKeys.remove(requestCode);
                }
            }
        }
    }

    private ArrayList<Type> parseType() {
        ArrayList<Type> ret = new ArrayList<>();
        for (String s : getResources().getStringArray(R.array.configuratorParametersType)) {
            ret.add(Type.valueOf(s));
        }
        return ret;
    }

    //functions called when buttons are pressed
    public void cancelChanges(View v) {
        startActivity(new Intent(this, MainActivity.class));
    }

    /**
     * Applies the changes in newSettings to the apps settings.
     *
     * @param v needed for the api, idk what it is O:)
     */
    public void applyChanges(View v) {
        newSettings.apply();
        //close after applying
        cancelChanges(v);
    }

    /**
     * Import changes from a file, the intent contains the files uri
     *
     * @param intent contains the files uri, is given from the android api
     */
    public void importChanges(Intent intent) {
        try {
            if (intent.getData() == null) {
                //this might happen, just ignore it
                return;
            }
            InputStream inputStream = getContentResolver().openInputStream(intent.getData());
            if (inputStream == null) {
                throw new IllegalStateException("InputStream is Null!");
            }
            Scanner lineScanner = new Scanner(inputStream);
            //lineScanner.useDelimiter("\n");
            newSettings.clear();
            Set<String> keys = new HashSet<>();
            if (!lineScanner.hasNextLine()) {
                throw new IllegalStateException("CANNOT READ FILE!");
            }
            while (lineScanner.hasNextLine()) {
                String currLine = lineScanner.nextLine();
                Scanner scanner = new Scanner(currLine);
                scanner.useDelimiter(";");
                String key = scanner.next();
                keys.add(key);
                System.out.append("key :").append(key);
                for (String curr : prefvalues) {
                    String read = scanner.next();
                    //System.out.append("just got ").append(read);
                    newSettings.putString(key + "_" + curr, read);
                }
                //ignore the rest
                scanner.close();
            }
            //put the keys in the thingie
            newSettings.putStringSet("objects", keys);
            newSettings.apply();
            lineScanner.close();
            inputStream.close();
            //reload activity
            startActivity(new Intent(this, this.getClass()));
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Could not read File!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Send an Intent : the user has to choose a configuration file on his device to load it
     *
     * @param v needed by api
     */
    public void importChanges(View v) {
        //send out an Intent!
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, importIntentID);
    }

    /**
     * Save the current configuration to a file
     *
     * @param v needed by api
     */
    public void exportChanges(View v) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> objectNames = settings.getStringSet("objects", null);
        StringBuilder output = new StringBuilder();

        if (objectNames == null) {
            throw new IllegalStateException("Could not read config!");
        }
        for (String s : objectNames) {
            doubleAppend(output, s);
            for (String curr : prefvalues) {
                doubleAppend(output, settings.getString(s + "_" + curr, "0"));
            }
            output.append('\n');
        }
        //TODO:ask the user for a destination
        String timeStamp = new java.util.Date().toString();
        String fileName = "customConfigLivingBurger" + timeStamp + ".csv";
        fileName = fileName.replace(':', '.');
        File exportDestination = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), fileName);
        //System.out.append(output);
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
        s.append(s2).append(';');
    }

    public void resetConfig() {
        resetConfig(this, newSettings);
        //restart activity
        startActivity(new Intent(this, Configurator.class));
    }

    public void resetConfig(View v) {
        resetConfig();
    }


    private void loadChristmasConfig() {
        loadChristmasConfig(this, newSettings);
        //restart activity
        startActivity(new Intent(this, Configurator.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefvalues = getPrefvalues(this);
        prefvaluesType = parseType();

        newSettings = PreferenceManager.getDefaultSharedPreferences(this).edit();
        //this gonna be complicated, but you have a tree here :
        // linearlayout
        // |--scrolling settingpanel
        // |     |--table with settings
        // |--Buttons (in a Row) (apply reset default, export, import, add a row, remove a row etc...)

        ScrollView ultraScroller = new ScrollView(this);
        HorizontalScrollView scroller = new HorizontalScrollView(this);
        tabelle = new TableLayout(this);
        scroller.addView(tabelle);
        createTable(this.tabelle);


        LinearLayout superLayout = new LinearLayout(this);
        superLayout.setOrientation(LinearLayout.VERTICAL);
        superLayout.addView(scroller);
        View.inflate(this, R.layout.buttons, superLayout);

        ultraScroller.addView(superLayout);

        setContentView(ultraScroller);
    }

    private void loadChristmasConfig(Context c, SharedPreferences.Editor edit) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
        Set<String> deleteMe = settings.getStringSet("objects", null);
        ArrayList<String> prefvalues = getPrefvalues(this);

        //delete old preference
        if (deleteMe != null) {
            for (String s : deleteMe) {
                for (String curr : prefvalues) {
                    edit.remove(s + "_" + curr);
                }
            }
        }

        //set new Preferences
        String[] burgerOptions = new String[]{
                "20", "false", Integer.toString(R.drawable.noel), "0", "0", "0", "-1", "false", "true", "5", "0", "1.0", "true"
        };
        String[] pizzaOptions = new String[]{
                "20", "false", Integer.toString(R.drawable.pizza), "0", "0", "0", "-1", "false", "false", "5", "180", "1.0", "true"
        };
        Set<String> addMe = new HashSet<>();
        addMe.add("burger");
        addMe.add("pizza");
        edit.putStringSet("objects", addMe);
        for (int i = 0; i < prefvalues.size(); i++) {
            edit.putString("burger_" + prefvalues.get(i), burgerOptions[i]);
            edit.putString("pizza_" + prefvalues.get(i), pizzaOptions[i]);
        }
        edit.apply();
    }

    /**
     * adds the standard Header to current TableLayout, which may look like this:
     * bitmap,xpos,ypos,....and so on
     */
    private void addHeader(TableLayout v) {
        TableRow header = new TableRow(this);
        header.setPadding(5, 5, 5, 5);
        //TextView deleteTV = new TextView(this);
        //deleteTV.setText(R.string.Delete);
        //header.addView(deleteTV);
        for (String s : getResources().getStringArray(R.array.configuratorParameters)) {
            TextView tv = new TextView(this);
            //TODO : text isnt separated by space between rows :(
            tv.setText(s);
            tv.setPadding(5, 5, 5, 5);
            header.addView(tv);
        }
        v.addView(header);
    }

    /**
     * generate a Button Listener, like generateEditTextListener but for buttons (for boolean values)
     *
     * @param s the key
     * @param i the ith Item
     * @return
     */
    private CompoundButton.OnCheckedChangeListener generateToggleButtonListener(final String s, final int i) {
        return new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                newSettings.putString(s + "_" + prefvalues.get(i), Boolean.toString(isChecked));
            }
        };
    }

    /**
     * generate a Listener for a text field, so when you enter 5, the 5 is saved into the settings editor
     *
     * @param str the key
     * @param i   the ith Item
     * @return a textwatcher that checks if text is correct (a int can't contain chars, for example) and sets the new
     * value into the settingseditor
     */
    private TextWatcher generateEditTextListener(final String str, final int i) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                newSettings.putString(str + "_" + prefvalues.get(i), s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                String checkedValue = null;
                //put it in the editor
                try {
                    switch (prefvaluesType.get(i)) {
                        case FLOAT:
                            checkedValue = Float.toString(Float.parseFloat(s.toString()));
                            break;
                        case IMAGE:
                            //can be ignored here
                            break;
                        case INT:
                            checkedValue = Integer.toString(Integer.parseInt(s.toString()));
                            break;
                        case LONG:
                            checkedValue = Long.toString(Long.parseLong(s.toString()));
                            break;
                        case BOOL:
                            //bools doesn't use this method
                        default:
                            throw new IllegalStateException("Maybe you forgot to implement something");
                    }
                    if (checkedValue == null) {
                        throw new IllegalStateException("this listener is broken!");
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(Configurator.this, "Incorrect value, must be " + prefvaluesType.get(i), Toast.LENGTH_SHORT).show();
                    s.clear();
                    s.append(PreferenceManager.getDefaultSharedPreferences(Configurator.this).getString(str + "_" + prefvalues.get(i), "0"));
                }
            }
        };
    }

    /**
     * sets all the row  in the table
     *
     * @param tabelle the table which contains nothing before this call
     */
    private void createTable(TableLayout tabelle) {
//        ArrayAdapter<String> preConfigs = new ArrayAdapter<>(this, R.layout.selector, preconfigurated);
//        Spinner preConfigSelector = new Spinner(this);
//        preConfigSelector.setAdapter(preConfigs);
//        preConfigSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            private boolean first = true;
//
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                if (first) {
//                    first = false;
//                } else {
//                    //sorry didn't find anything beautifuler
//                    if (parent.getItemAtPosition(position).equals(preconfigurated[0])) {
//                        resetConfig();
//                    } else {
//                        if (parent.getItemAtPosition(position).equals(preconfigurated[1])) {
//                            loadChristmasConfig();
//                        } else {
//                            throw new IllegalStateException("Not implemented!");
//                        }
//                    }
//                }
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//
//            }
//        });
//
//        //add first row to this table, which is the preConfigSelector
//        tabelle.addView(preConfigSelector);
//
        //add header (name of options)
        addHeader(tabelle);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> rows = settings.getStringSet("objects", null);
        //a e s t h e t i c s
        tabelle.setPadding(5, 5, 5, 5);
        tabelle.setStretchAllColumns(true);
        //if no config, load standard config
        if (rows == null) {
            resetConfig();
            rows = settings.getStringSet("objects", null);
            if (rows == null) {
                throw new IllegalStateException("Settings not existing and generating new settings didn't work");
            }
        }
        //generate the rows with actual values, put them in the rowslist
        for (String s : rows) {
            TableRow current = getTableRow(settings, s);
            rowsList.add(current);
            tabelle.addView(current);
        }
    }

    private void chooseImage(String helper, ImageView iv) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        ++actualIntentKeysID;
        intentKeys.put(actualIntentKeysID, helper);
        buttonKeys.put(actualIntentKeysID, iv);
        startActivityForResult(intent, actualIntentKeysID);
    }

    @NonNull
    private TableRow getTableRow(final SharedPreferences settings, String s) {
        final String helper = s;
        TableRow current = new TableRow(this);
        Button deleteButton = new Button(this);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newSettings.remove(helper);
            }
        });
        //TODO : deletebutton
        //current.addView(deleteButton);
        for (int i = 0; i < prefvalues.size(); i++) {
            if (prefvaluesType.get(i) == BOOL) {
                Switch tb = new Switch(this);
                tb.setChecked(Boolean.parseBoolean(settings.getString(s + "_" + prefvalues.get(i), "false")));
                tb.setOnCheckedChangeListener(generateToggleButtonListener(s, i));
                current.addView(tb);
                continue;
            }
            if (prefvaluesType.get(i) == IMAGE) {
                final ImageView iv = new ImageView(this);
                try {
                    //try to load this image as internal resource
                    int id = Integer.parseInt(settings.getString(s + "_" + prefvalues.get(i), Integer.toString(R.drawable.burger)));
                    iv.setImageBitmap(BitmapFactory.decodeResource(getResources(), id));
                } catch (Exception e) {
                    //maybe it was an File, that means it is an external (on the sd card) resource
                    e.printStackTrace();
                    try {
                        Bitmap loadedImage = BitmapFactory.decodeFile(settings.getString(s + "_" + prefvalues.get(i), ""));
                        iv.setImageBitmap(loadedImage);
                    } catch (Exception e2) {
                        //ok use the burger, the content in this setting is invalid
                        e2.printStackTrace();
                        iv.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.burger));
                    }
                }
                iv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        chooseImage(helper, iv);
                    }
                });

                //TODO: move those 2 arrays to the Resources section
                final String[] vals = new String[]{
                        getResources().getString(R.string.choose_image),
                        "Burger",
                        "Pizza",
                        "Christmas Burger",
                };
                final int[] valResources = new int[]{
                        R.drawable.burger,
                        R.drawable.pizza,
                        R.drawable.noel,
                };
                //spinner with externalImage, internalImages
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.selector, vals);

                //TODO : make this spinner more beautiful
                Spinner spinner = new Spinner(this);
                spinner.setAdapter(arrayAdapter);
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    private boolean first = true;

                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        if (first) {
                            first = false;
                            return;
                        }
                        if (i == 0) {
                            chooseImage(helper, iv);
                        } else {
                            int valResource = valResources[i - 1];
                            iv.setImageBitmap(BitmapFactory.decodeResource(getResources(), valResource));
                            newSettings.putString(helper + "_image", Integer.toString(valResource));
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
                LinearLayout ll = new LinearLayout(this);
                ll.addView(spinner);
                ll.addView(iv);
                //current.addView(spinner);
                //current.addView(iv);
                current.addView(ll);
                continue;
            }
            EditText et = new EditText(this);
            et.setText(settings.getString(s + "_" + prefvalues.get(i), "0"));
            et.addTextChangedListener(generateEditTextListener(s, i));
            //the magical numbers limits the input in the fields, so you can only type number in a int field, per example
            switch (prefvaluesType.get(i)) {
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
        return current;
    }

    public void addRow(View v) {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> abc = p.getStringSet("objects", null);
        if (abc == null) {
            throw new IllegalStateException("broken settings! objects not found!");
        }
        String timeStamp = new java.util.Date().toString();
        abc.add(timeStamp);
        newSettings.putStringSet("objects", abc);
        View view = getTableRow(p, timeStamp);
        rowsList.add(view);
        tabelle.addView(view);
    }

    public void removeRow(View v) {

    }

    public void removeRow(String s, View v) {
        if (rowsList.size() < 1) {
            return;
        }
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> abc = p.getStringSet("objects", null);
        if (abc == null) {
            throw new IllegalStateException("broken settings! objects not found!");
        }
        //TODO : find better unique keys that are better then "rowslist.size()" !
        abc.remove(s);
        newSettings.putStringSet("objects", abc);
        tabelle.removeView(rowsList.get(rowsList.size() - 1));
        rowsList.remove(rowsList.size() - 1);
    }

    /**
     * Saves image in internal app storage, so you can retrieve the image later (eg after a restart)
     * /!\ overwrite existing images with same filenames
     *
     * @param uri      imageLocation
     * @param filename name of image (named like the key of the object)
     * @return the File containing the copy of the image
     * @throws IOException if something fails
     */
    private File backupBitmapFromUri(Uri uri, String filename) throws IOException {
        return backupBitmapFromBitmap(getBitmapFromUri(uri, getApplicationContext()), filename, getApplicationContext());
    }

    /**
     * Saves image in internal app storage, so you can retrieve the image later (eg after a restart)
     * /!\ overwrite existing images with same filenames
     * @param bmp Source
     * @param filename filename
     * @return the file with the backup saved in the internal storage
     * @throws IOException if something failed
     */
    public static File backupBitmapFromBitmap(Bitmap bmp, String filename, Context context) throws IOException {
        File backupLocation = new File(context.getFilesDir(), filename);
        //create File
        FileOutputStream fos = new FileOutputStream(backupLocation);
        //save image in it
        bmp.compress(Bitmap.CompressFormat.PNG, 0, fos);
        //close it
        fos.close();
        return backupLocation;
    }

    /**
     * Loads Bitmap from Uri, copypasted from api docs
     * @param uri the Uri
     * @return the bitmap loaded from the uri
     * @throws IOException if something fails
     */
    public static Bitmap getBitmapFromUri(Uri uri, Context context) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                context.getContentResolver().openFileDescriptor(uri, "r");
        if (parcelFileDescriptor == null) {
            throw new IOException("Could not read texture!");
        }
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();

        return image;
    }
}
