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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;

public class SimpleConfigurator extends AppCompatActivity {
    final int changeImageIntentId = 5;
    private ImageButton ibNeedsImage;
    private String updateEntryWithImage;

    public String getText() {
        return "Hello";
    }

    private void add(Context context) {
        Toast.makeText(context, "Working!", Toast.LENGTH_SHORT).show();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == changeImageIntentId) {
                try {
                    Bitmap b = Configurator.getBitmapFromUri(data.getData(), getApplicationContext());
                    ibNeedsImage.setImageBitmap(b);
                    SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                    File copy = Configurator.backupBitmapFromBitmap(b, updateEntryWithImage, getApplicationContext());
                    edit.putString(updateEntryWithImage + "_image", copy.getAbsolutePath());
                    edit.putString(updateEntryWithImage + "_isExternalResource", "true");
                    edit.apply();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Cannot open file!", Toast.LENGTH_SHORT).show();
                } finally {
                    ibNeedsImage = null;
                    updateEntryWithImage = null;
                }
            }
        }
    }

    private void getImage() {
    }

//    private void createCardViews()
//    {
//        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//        Set<String> objects = sp.getStringSet("objects", new HashSet<String>());
//        ViewGroup root = (ViewGroup) findViewById(R.id.linearLayout);
//        for (String s : objects) {
//            root = (LinearLayout) CardView.inflate(getApplicationContext(), R.layout.my_card_view, root);
//            CardView cv = (CardView)root.getChildAt(root.getChildCount()-1);
//            try {
//                //try to load this image as internal resource
//                int id = Integer.parseInt(sp.getString(s + "_image", "0"));
//                ib.setImageBitmap(BitmapFactory.decodeResource(getResources(), id));
//            } catch (Exception e) {
//                //maybe it was an File, that means it is an external (on the sd card) resource
//                e.printStackTrace();
//                try {
//                    Bitmap loadedImage = BitmapFactory.decodeFile(sp.getString(s + "_image", ""));
//                    ib.setImageBitmap(loadedImage);
//                } catch (Exception e2) {
//                    //ok use the burger, the content in this setting is invalid
//                    e2.printStackTrace();
//                    ib.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.burger));
//                }
//            }
//            updateEntryWithImage = s;
//            ibNeedsImage = ib;
//            ib.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//                    intent.setType("image/*");
//                    startActivityForResult(intent, changeImageIntentId);
//                }
//            });
//        }
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_configurator);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                add(view.getContext());
            }
        });
        //View.inflate(getApplicationContext(), R.layout.my_card_view, (ViewGroup) findViewById(R.id.linearLayout));
        View.inflate(getApplicationContext(), R.layout.binding, (ViewGroup) findViewById(R.id.linearLayout));
    }

}
