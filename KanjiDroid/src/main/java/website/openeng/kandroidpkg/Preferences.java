/***************************************************************************************
 * Copyright (c) 2009 Nicolas Raoul <nicolas.raoul@gmail.com>                           *
 * Copyright (c) 2009 Edu Zamora <edu.zasu@gmail.com>                                   *
 * Copyright (c) 2010 Norbert Nagold <norbert.nagold@gmail.com>                         *
 * Copyright (c) 2012 Kostas Spyropoulos <inigo.aldana@gmail.com>                       *
 * Copyright (c) 2015 Timothy Rae <perceptualchaos2@gmail.com>                          *
 *                                                                                      *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/

package website.openeng.kandroidpkg;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.support.annotation.NonNull;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.WindowManager.BadTokenException;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import website.openeng.themes.Themes;
import website.openeng.ui.AppCompatPreferenceActivity;
import website.openeng.ui.SeekBarPreference;
import website.openeng.anim.ActivityTransitionAnimation;
import website.openeng.kandroidpkg.exception.StorageAccessException;
import website.openeng.compat.CompatHelper;
import website.openeng.libkanji.Collection;
import website.openeng.libkanji.Utils;
import website.openeng.libkanji.hooks.ChessFilter;
import website.openeng.libkanji.hooks.HebrewFixFilter;
import website.openeng.libkanji.hooks.Hooks;
import website.openeng.preferences.NumberRangePreference;
import website.openeng.utils.LanguageUtil;
import website.openeng.utils.VersionUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import timber.log.Timber;

interface PreferenceContext {
    void addPreferencesFromResource(int preferencesResId);
    PreferenceScreen getPreferenceScreen();
}

/**
 * Preferences dialog.
 */
public class Preferences extends AppCompatPreferenceActivity implements PreferenceContext, OnSharedPreferenceChangeListener {

    private static final int DIALOG_HEBREW_FONT = 3;
    public static boolean COMING_FROM_ADD = false;

    /** Key of the language preference */
    public static final String LANGUAGE = "language";
    private Collection mCol;

    // Other variables
    private final HashMap<String, String> mOriginalSumarries = new HashMap<>();
    private static final String [] sCollectionPreferences = {"showEstimates", "showProgress",
            "learnCutoff", "timeLimit", "useCurrent", "newSpread", "dayOffset"};


    // ----------------------------------------------------------------------------
    // Overridden methods
    // ----------------------------------------------------------------------------

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Themes.setThemeLegacy(this);

        super.onCreate(savedInstanceState);
        mCol = CollectionHelper.getInstance().getCol(this);

        // Legacy code using intents instead of PreferenceFragments
        String action = getIntent().getAction();
        if (!CompatHelper.isHoneycomb()) {
            if (action == null) {
                // Headers screen
                addPreferencesFromResource(R.xml.preference_headers_legacy);
            } else {
                initSubscreen(action, this);
            }
            // Set the text for the summary of each of the preferences
            initAllPreferences(getPreferenceScreen());
        }

        // Add a home button to the actionbar
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }


    // Called only on Honeycomb and later
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }


    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SettingsFragment.class.getName().equals(fragmentName);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return false;
    }


    @SuppressWarnings("deprecation")
    @Override
    protected void onPause() {
        super.onPause();
        // Legacy code to register listener when not using PreferenceFragment
        if (!CompatHelper.isHoneycomb() && getPreferenceScreen() != null) {
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }
    }


    @SuppressWarnings("deprecation")
    @Override
    protected void onResume() {
        super.onResume();
        // Legacy code to register listener when not using PreferenceFragment
        if (!CompatHelper.isHoneycomb() && getPreferenceScreen() != null) {
            SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
            prefs.registerOnSharedPreferenceChangeListener(this);
            // syncAccount's summary can change while preferences are still open (user logs
            // in from preferences screen), so we need to update it here.
            updatePreference(prefs, "syncAccount", this);
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            Timber.i("Preferences:: onBackPressed()");
            closePreferences();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected MaterialDialog onCreateDialog(int id) {
        Resources res = getResources();
        MaterialDialog.Builder builder = new MaterialDialog.Builder(this);
        switch (id) {
            case DIALOG_HEBREW_FONT:
                builder.title(res.getString(R.string.fix_hebrew_text));
                builder.content(res.getString(R.string.fix_hebrew_instructions,
                        CollectionHelper.getCurrentKanjiDroidDirectory(this)));
                builder.callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(getResources().getString(
                                R.string.link_hebrew_font)));
                        startActivity(intent);
                    }
                });
                builder.positiveText(res.getString(R.string.fix_hebrew_download_font));
                builder.negativeText(R.string.dialog_cancel);
        }
        return builder.show();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updatePreference(sharedPreferences, key, this);
    }


    // ----------------------------------------------------------------------------
    // Class methods
    // ----------------------------------------------------------------------------

    private void initSubscreen(String action, PreferenceContext listener) {
        PreferenceScreen screen;
        switch (action) {
            case "website.openeng.kandroidpkg.prefs.general":
                listener.addPreferencesFromResource(R.xml.preferences_general);
                screen = listener.getPreferenceScreen();
                // Build languages
                initializeLanguageDialog(screen);
                // Check that input is valid before committing change in the collection path
                EditTextPreference collectionPathPreference = (EditTextPreference) screen.findPreference("deckPath");
                collectionPathPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, final Object newValue) {
                        final String newPath = (String) newValue;
                        try {
                            CollectionHelper.initializeKanjiDroidDirectory(newPath);
                            return true;
                        } catch (StorageAccessException e) {
                            Timber.e(e, "Could not initialize directory: %s", newPath);
                            Toast.makeText(getApplicationContext(), R.string.dialog_collection_path_not_dir, Toast.LENGTH_LONG).show();
                            return false;
                        }
                    }
                });
                break;
            case "website.openeng.kandroidpkg.prefs.reviewing":
                listener.addPreferencesFromResource(R.xml.preferences_reviewing);
                break;
            case "website.openeng.kandroidpkg.prefs.fonts":
                listener.addPreferencesFromResource(R.xml.preferences_fonts);
                screen = listener.getPreferenceScreen();
                initializeCustomFontsDialog(screen);
                break;
            case "website.openeng.kandroidpkg.prefs.gestures":
                listener.addPreferencesFromResource(R.xml.preferences_gestures);
                break;
            case "website.openeng.kandroidpkg.prefs.advanced":
                listener.addPreferencesFromResource(R.xml.preferences_advanced);
                screen = listener.getPreferenceScreen();
                // Force full sync option
                Preference fullSyncPreference = screen.findPreference("force_full_sync");
                fullSyncPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        if (mCol != null && mCol.getDb() != null) {
                            // TODO: Could be useful to show the full confirmation dialog
                            mCol.modSchemaNoCheck();
                            mCol.setMod();
                            Toast.makeText(getApplicationContext(), android.R.string.ok, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), R.string.vague_error, Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    }
                });
                // Workaround preferences
                removeUnnecessaryAdvancedPrefs(screen);
                break;
        }
    }


    /**
     * Loop over every preference in the list and set the summary text
     */
    private void initAllPreferences(PreferenceScreen screen) {
        for (int i = 0; i < screen.getPreferenceCount(); ++i) {
            Preference preference = screen.getPreference(i);
            if (preference instanceof PreferenceGroup) {
                PreferenceGroup preferenceGroup = (PreferenceGroup) preference;
                for (int j = 0; j < preferenceGroup.getPreferenceCount(); ++j) {
                    Preference nestedPreference = preferenceGroup.getPreference(j);
                    if (nestedPreference instanceof PreferenceGroup) {
                        PreferenceGroup nestedPreferenceGroup = (PreferenceGroup) nestedPreference;
                        for (int k = 0; k < nestedPreferenceGroup.getPreferenceCount(); ++k) {
                            initPreference(nestedPreferenceGroup.getPreference(k));
                        }
                    } else {
                        initPreference(preferenceGroup.getPreference(j));
                    }
                }
            } else {
                initPreference(preference);
            }
        }
    }



    private void initPreference(Preference pref) {
        // Load stored values from Preferences which are stored in the Collection
        if (Arrays.asList(sCollectionPreferences).contains(pref.getKey())) {
            if (mCol != null) {
                try {
                    JSONObject conf = mCol.getConf();
                    switch (pref.getKey()) {
                        case "showEstimates":
                            ((CheckBoxPreference)pref).setChecked(conf.getBoolean("estTimes"));
                            break;
                        case "showProgress":
                            ((CheckBoxPreference)pref).setChecked(conf.getBoolean("dueCounts"));
                            break;
                        case "learnCutoff":
                            ((NumberRangePreference)pref).setValue(conf.getInt("collapseTime") / 60);
                            break;
                        case "timeLimit":
                            ((NumberRangePreference)pref).setValue(conf.getInt("timeLim") / 60);
                            break;
                        case "useCurrent":
                            ((ListPreference)pref).setValueIndex(conf.optBoolean("addToCur", true) ? 0 : 1);
                            break;
                        case "newSpread":
                            ((ListPreference)pref).setValueIndex(conf.getInt("newSpread"));
                            break;
                        case "dayOffset":
                            Calendar calendar = new GregorianCalendar();
                            Timestamp timestamp = new Timestamp(mCol.getCrt() * 1000);
                            calendar.setTimeInMillis(timestamp.getTime());
                            ((SeekBarPreference)pref).setValue(calendar.get(Calendar.HOUR_OF_DAY));
                            break;
                    }
                } catch (JSONException | NumberFormatException e) {
                    throw new RuntimeException();
                }
            } else {
                // Disable Col preferences if Collection closed
                pref.setEnabled(false);
            }
        } else if ("minimumCardsDueForNotification".equals(pref.getKey())) {
            updateNotificationPreference((ListPreference) pref);
        }
        // Set the value from the summary cache
        CharSequence s = pref.getSummary();
        mOriginalSumarries.put(pref.getKey(), (s != null) ? s.toString() : "");
        // Update summary

        updateSummary(pref);
    }


    /**
     * Code which is run when a SharedPreference change has been detected
     * @param prefs instance of SharedPreferences
     * @param key key in prefs which is being updated
     * @param listener PreferenceActivity of PreferenceFragment which is hosting the preference
     */
    private void updatePreference(SharedPreferences prefs, String key, PreferenceContext listener) {
        try {
            PreferenceScreen screen = listener.getPreferenceScreen();
            Preference pref = screen.findPreference(key);
            // Handle special cases
            switch (key) {
                case "timeoutAnswer":
                    CheckBoxPreference keepScreenOn = (CheckBoxPreference) screen.findPreference("keepScreenOn");
                    keepScreenOn.setChecked(((CheckBoxPreference) pref).isChecked());
                    break;
                case LANGUAGE:
                    closePreferences();
                    break;
                case "convertFenText":
                    if (((CheckBoxPreference) pref).isChecked()) {
                        ChessFilter.install(Hooks.getInstance(getApplicationContext()));
                    } else {
                        ChessFilter.uninstall(Hooks.getInstance(getApplicationContext()));
                    }
                    break;
                case "fixHebrewText":
                    if (((CheckBoxPreference) pref).isChecked()) {
                        HebrewFixFilter.install(Hooks.getInstance(getApplicationContext()));
                        showDialog(DIALOG_HEBREW_FONT);
                    } else {
                        HebrewFixFilter.uninstall(Hooks.getInstance(getApplicationContext()));
                    }
                    break;
                case "showProgress":
                    mCol.getConf().put("dueCounts", ((CheckBoxPreference) pref).isChecked());
                    mCol.setMod();
                    break;
                case "showEstimates":
                    mCol.getConf().put("estTimes", ((CheckBoxPreference) pref).isChecked());
                    mCol.setMod();
                    break;
                case "newSpread":
                    mCol.getConf().put("newSpread", Integer.parseInt(((ListPreference) pref).getValue()));
                    mCol.setMod();
                    break;
                case "timeLimit":
                    mCol.getConf().put("timeLim", ((NumberRangePreference) pref).getValue() * 60);
                    mCol.setMod();
                    break;
                case "learnCutoff":
                    mCol.getConf().put("collapseTime", ((NumberRangePreference) pref).getValue() * 60);
                    mCol.setMod();
                    break;
                case "useCurrent":
                    mCol.getConf().put("addToCur", ((ListPreference) pref).getValue().equals("0"));
                    mCol.setMod();
                    break;
                case "dayOffset":
                    int hours = ((SeekBarPreference) pref).getValue();
                    Calendar date = new GregorianCalendar();
                    date.set(Calendar.HOUR_OF_DAY, hours);
                    mCol.setCrt(date.getTimeInMillis() / 1000);
                    mCol.setMod();
                    break;
                case "minimumCardsDueForNotification":
                    ListPreference listpref = (ListPreference) screen.findPreference("minimumCardsDueForNotification");
                    if (listpref != null) {
                        updateNotificationPreference(listpref);
                    }
                    break;
                case "reportErrorMode":
                    String value = prefs.getString("reportErrorMode", "");
                    KanjiDroidApp.getInstance().setAcraReportingMode(value);
                    break;
                case "syncAccount":
                    SharedPreferences preferences = KanjiDroidApp.getSharedPrefs(getBaseContext());
                    String username = preferences.getString("username", "");
                    Preference syncAccount = screen.findPreference("syncAccount");
                    if (syncAccount != null) {
                        if (TextUtils.isEmpty(username)) {
                            syncAccount.setSummary(R.string.sync_account_summ_logged_out);
                        } else {
                            syncAccount.setSummary(getString(R.string.sync_account_summ_logged_in, username));
                        }
                    }
                    break;
            }
            // Update the summary text to reflect new value
            updateSummary(pref);
        } catch (BadTokenException e) {
            Timber.e(e, "Preferences: BadTokenException on showDialog");
        } catch (NumberFormatException | JSONException e) {
            throw new RuntimeException();
        }
    }

    public void updateNotificationPreference(ListPreference listpref) {
        CharSequence[] entries = listpref.getEntries();
        CharSequence[] values = listpref.getEntryValues();
        for (int i = 0; i < entries.length; i++) {
            int value = Integer.parseInt(values[i].toString());
            if (entries[i].toString().contains("%d")) {
                entries[i] = String.format(entries[i].toString(), value);
            }
        }
        listpref.setEntries(entries);
        listpref.setSummary(listpref.getEntry().toString());
    }

    private void updateSummary(Preference pref) {
        if (pref == null || pref.getKey() == null) {
            return;
        }
        if (pref.getKey().equals("about_dialog_preference")) {
            pref.setSummary(getResources().getString(R.string.about_version) + " " + VersionUtils.getPkgVersionName());
        }
        // Get value text
        String value;
        try {
            if (pref instanceof  NumberRangePreference) {
                value = Integer.toString(((NumberRangePreference) pref).getValue());
            } else if (pref instanceof SeekBarPreference) {
                value = Integer.toString(((SeekBarPreference) pref).getValue());
            } else if (pref instanceof ListPreference) {
                value = ((ListPreference) pref).getEntry().toString();
            } else if (pref instanceof EditTextPreference) {
                value = ((EditTextPreference) pref).getText();
            } else {
                return;
            }
        } catch (NullPointerException e) {
            value = "";
        }
        // Get summary text
        String oldSummary = mOriginalSumarries.get(pref.getKey());
        // Replace summary text with value according to some rules
        if (oldSummary.equals("")) {
            pref.setSummary(value);
        } else if (value.equals("")) {
            pref.setSummary(oldSummary);
        } else if (pref.getKey().equals("minimumCardsDueForNotification")) {
            pref.setSummary(replaceStringIfNumeric(oldSummary, value));
        } else {
            pref.setSummary(replaceString(oldSummary, value));
        }
    }


    private String replaceString(String str, String value) {
        if (str.contains("XXX")) {
            return str.replace("XXX", value);
        } else {
            return str;
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private String replaceStringIfNumeric(String str, String value) {
        try {
            Double.parseDouble(value);
            return replaceString(str, value);
        } catch (NumberFormatException e){
            return value;
        }
    }

    private void initializeLanguageDialog(PreferenceScreen screen) {
        ListPreference languageSelection = (ListPreference) screen.findPreference(LANGUAGE);
        if (languageSelection != null) {
            Map<String, String> items = new TreeMap<>();
            for (String localeCode : LanguageUtil.APP_LANGUAGES) {
                Locale loc = LanguageUtil.getLocale(localeCode);
                items.put(loc.getDisplayName(), loc.toString());
            }
            CharSequence[] languageDialogLabels = new CharSequence[items.size() + 1];
            CharSequence[] languageDialogValues = new CharSequence[items.size() + 1];
            languageDialogLabels[0] = getResources().getString(R.string.language_system);
            languageDialogValues[0] = "";
            int i = 1;
            for (Map.Entry<String, String> e : items.entrySet()) {
                languageDialogLabels[i] = e.getKey();
                languageDialogValues[i] = e.getValue();
                i++;
            }

            languageSelection.setEntries(languageDialogLabels);
            languageSelection.setEntryValues(languageDialogValues);
        }
    }

    private void removeUnnecessaryAdvancedPrefs(PreferenceScreen screen) {
        PreferenceCategory plugins = (PreferenceCategory) screen.findPreference("category_plugins");
        // Disable the emoji/kana buttons to scroll preference if those keys don't exist
        if (!CompatHelper.hasKanaAndEmojiKeys()) {
            CheckBoxPreference emojiScrolling = (CheckBoxPreference) screen.findPreference("scrolling_buttons");
            if (emojiScrolling != null && plugins != null) {
                plugins.removePreference(emojiScrolling);
            }
        }
        // Disable the double scroll preference if no scrolling keys
        if (!CompatHelper.hasScrollKeys() && !CompatHelper.hasKanaAndEmojiKeys()) {
            CheckBoxPreference doubleScrolling = (CheckBoxPreference) screen.findPreference("double_scrolling");
            if (doubleScrolling != null && plugins != null) {
                plugins.removePreference(doubleScrolling);
            }
        }

        PreferenceCategory workarounds = (PreferenceCategory) screen.findPreference("category_workarounds");
        if (workarounds != null) {
            CheckBoxPreference inputWorkaround = (CheckBoxPreference) screen.findPreference("inputWorkaround");
            CheckBoxPreference longclickWorkaround = (CheckBoxPreference) screen.findPreference("textSelectionLongclickWorkaround");
            CheckBoxPreference fixHebrewText = (CheckBoxPreference) screen.findPreference("fixHebrewText");
            CheckBoxPreference fixArabicText = (CheckBoxPreference) screen.findPreference("fixArabicText");
            CheckBoxPreference safeDisplayMode = (CheckBoxPreference) screen.findPreference("safeDisplay");
            CompatHelper.removeHiddenPreferences(this.getApplicationContext());
            if (CompatHelper.getSdkVersion() >= 9) {
                workarounds.removePreference(fixArabicText);
            }
            if (CompatHelper.isHoneycomb()) {
                workarounds.removePreference(longclickWorkaround);
            }
            if (CompatHelper.getSdkVersion() >= 13) {
                workarounds.removePreference(safeDisplayMode);
            }
            if (CompatHelper.getSdkVersion() >= 15) {
                workarounds.removePreference(inputWorkaround);
            }
            if (CompatHelper.getSdkVersion() >= 16) {
                workarounds.removePreference(fixHebrewText);
                screen.removePreference(workarounds);     // group itself can be hidden for API 16
            }
        }
    }


    /** Initializes the list of custom fonts shown in the preferences. */
    private void initializeCustomFontsDialog(PreferenceScreen screen) {
        ListPreference defaultFontPreference = (ListPreference) screen.findPreference("defaultFont");
        if (defaultFontPreference != null) {
            defaultFontPreference.setEntries(getCustomFonts("System default"));
            defaultFontPreference.setEntryValues(getCustomFonts(""));
        }
        ListPreference browserEditorCustomFontsPreference = (ListPreference) screen.findPreference("browserEditorFont");
        browserEditorCustomFontsPreference.setEntries(getCustomFonts("System default"));
        browserEditorCustomFontsPreference.setEntryValues(getCustomFonts("", true));
    }


    /** Returns a list of the names of the installed custom fonts. */
    private String[] getCustomFonts(String defaultValue) {
        return getCustomFonts(defaultValue, false);
    }


    private String[] getCustomFonts(String defaultValue, boolean useFullPath) {
        List<KanjiFont> mFonts = Utils.getCustomFonts(this);
        int count = mFonts.size();
        Timber.d("There are %d custom fonts", count);
        String[] names = new String[count + 1];
        names[0] = defaultValue;
        if (useFullPath) {
            for (int index = 1; index < count + 1; ++index) {
                names[index] = mFonts.get(index - 1).getPath();
                Timber.d("Adding custom font: %s", names[index]);
            }
        } else {
            for (int index = 1; index < count + 1; ++index) {
                names[index] = mFonts.get(index - 1).getName();
                Timber.d("Adding custom font: %s", names[index]);
            }
        }
        return names;
    }


    private void closePreferences() {
        finish();
        ActivityTransitionAnimation.slide(this, ActivityTransitionAnimation.FADE);
        if (mCol != null && mCol.getDb()!= null) {
            mCol.save();
        }
    }

    // ----------------------------------------------------------------------------
    // Inner classes
    // ----------------------------------------------------------------------------

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SettingsFragment extends PreferenceFragment implements PreferenceContext, OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            String subscreen = getArguments().getString("subscreen");
            ((Preferences) getActivity()).initSubscreen(subscreen, this);
            ((Preferences) getActivity()).initAllPreferences(getPreferenceScreen());
        }

        @Override
        public void onResume() {
            super.onResume();
            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            prefs.registerOnSharedPreferenceChangeListener(this);
            // syncAccount's summary can change while preferences are still open (user logs
            // in from preferences screen), so we need to update it here.
            ((Preferences) getActivity()).updatePreference(prefs, "syncAccount", this);
        }

        @Override
        public void onPause() {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            ((Preferences) getActivity()).updatePreference(sharedPreferences, key, this);
        }
    }
}