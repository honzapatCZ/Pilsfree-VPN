package com.honzapatCZ.PilsfreeVPN;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

public class SettFragment extends PreferenceFragmentCompat {


    public void onCreatePreferences(@Nullable Bundle savedInstace, String string){


        setPreferencesFromResource(R.xml.settings, string);

    }

}
