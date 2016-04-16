package me.gitai.hosts.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.umeng.analytics.MobclickAgent;

import org.apache.commons.io.FileUtils;

import java.io.File;

import me.gitai.hosts.BuildConfig;
import me.gitai.hosts.Constant;
import me.gitai.hosts.R;
import me.gitai.hosts.widget.AccessView;
import me.gitai.library.ui.BasePreferenceActivity;
import me.gitai.library.util.AndroidUtils;
import me.gitai.library.util.ToastUtil;
import me.gitai.library.widget.MaterialDialog;

/**
 * Created by dp on 15-8-22.
 */
public class ManagePreferences extends BasePreferenceActivity {
    private Preference clear_caches,project_private,project_diy;
    private Preference reset_settings;
    private AccessView accessView;

    public static final int RESULT_CODE = 1;

    @Override
    protected void onResume() {
        super.onResume();
        accessView.refesh();
        MobclickAgent.onResume(this);
    }

    @Override
    public void onPause(){
        super.onPause();
        MobclickAgent.onPause(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        project_private = findPreference(Constant.PREFERENCE_KEY_PROJECT_PRIVATE);
        project_diy = findPreference(Constant.PREFERENCE_KEY_PROJECT_DIY);

        accessView = new AccessView(this);
        accessView.setOnLoginStateChange(new AccessView.OnLoginStateChange() {
            @Override
            public void OnChange(boolean login) {
                project_private.setEnabled(login);
            }
        });
        getListView().addHeaderView(accessView);
        accessView.refesh();

        project_diy.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            View view_dialog;
            EditText et_url;

            @Override
            public boolean onPreferenceClick(Preference preference) {
                view_dialog = LayoutInflater.from(ManagePreferences.this).inflate(R.layout.dialog_hosts_diy, null);
                et_url = (EditText) view_dialog.findViewById(R.id.et_url);
                new MaterialDialog(ManagePreferences.this)
                        .setTitle(getString(R.string.dialog_hosts_diy_title))
                        .setContentView(view_dialog)
                        .setPositiveButton(getString(android.R.string.ok), new MaterialDialog.OnClickListener() {
                            @Override
                            public boolean onClick(View v, View MaterialDialog) {
                                Intent intent = new Intent(ManagePreferences.this, MainActivity.class);
                                intent.setData(Uri.parse(et_url.getText().toString()));
                                startActivity(intent);
                                finish();
                                return true;
                            }
                        })
                        .setNegativeButton(getString(android.R.string.cancel), null)
                        .show();
                return false;
            }
        });

        Intent intent = new Intent(ManagePreferences.this, MainActivity.class);
        intent.setData(Uri.parse("default"));
        findPreference(Constant.PREFERENCE_KEY_SYSTEM_DEFAULT).setIntent(intent);

        reset_settings = findPreference(Constant.PREFERENCE_KEY_RESET_SETTINGS);
        clear_caches = findPreference(Constant.PREFERENCE_KEY_CLEAR_CACHES);

        reset_settings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                i.setData(Uri.fromParts("package", BuildConfig.APPLICATION_ID,null));
                startActivity(i);
                return false;
            }
        });

        clear_caches.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try{
                    File cache = AndroidUtils.getCacheDir(ManagePreferences.this);
                    ToastUtil.show(FileUtils.sizeOfDirectory(cache)+"kb");
                    FileUtils.deleteDirectory(cache);
                }catch (Exception e){
                    ToastUtil.show(e.getLocalizedMessage());
                }
                return false;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
