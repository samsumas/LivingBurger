/*
 * Licensed under GPL 3.0
 */

package org.sasehash.burgerwp;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;


public class Setting {
    private ArrayList<String> names;
    private ArrayList<Type> types;
    private ArrayList<String> values;

    public Setting(String[] names, Type[] types) {
        if (names.length != types.length) {
            throw new IllegalStateException("Bad Arguments for Setting");
        }
        this.names = new ArrayList<>(Arrays.asList(names));
        this.types = new ArrayList<>(Arrays.asList(types));
    }

    public ArrayList<String> getNames() {
        return names;
    }

    public ArrayList<String> getValues() {
        return values;
    }

    public int size() {
        return names.size();
    }

    public ArrayList<Type> getTypes() {
        return types;
    }
}
