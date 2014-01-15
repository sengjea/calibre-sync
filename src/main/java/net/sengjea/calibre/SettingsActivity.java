package net.sengjea.calibre;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preference);
        try {
            Preference prefAppVersionName = findPreference("pref_app_version_name");
            prefAppVersionName.setSummary(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (Exception e) {
            Logger.e(e);
        }
	}
}
