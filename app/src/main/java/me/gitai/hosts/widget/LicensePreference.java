package me.gitai.hosts.widget;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import me.gitai.hosts.R;
import me.gitai.library.widget.MaterialDialog;

/**
 * Created by dphdjy on 15-11-5.
 */
public class LicensePreference extends Preference implements Preference.OnPreferenceClickListener {
    private MaterialDialog licenseDialog;

    public LicensePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public LicensePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LicensePreference(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context){
        licenseDialog = new MaterialDialog(context)
                .setTitle(context.getString(R.string.dialog_licenses_title))
                .setMessage("sugar\numeng.analytics\nsystembartint\nxUtils\nRootTools\ncommons-io")
                .setNegativeButton(android.R.string.ok, null);

        setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        licenseDialog.show();
        return false;
    }
}
