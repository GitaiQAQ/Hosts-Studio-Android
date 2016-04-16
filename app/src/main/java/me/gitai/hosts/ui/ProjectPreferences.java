package me.gitai.hosts.ui;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.lidroid.xutils.BitmapUtils;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import me.gitai.hosts.utils.ProgressDialogUtil;
import me.gitai.hosts.utils.XutilsHttpClient;
import me.gitai.hosts.*;
import me.gitai.library.ui.BasePreferenceActivity;
import me.gitai.library.util.IOUtils;
import me.gitai.library.util.SharedPreferencesUtil;
import me.gitai.library.util.StringUtils;
import me.gitai.library.util.ToastUtil;

/**
 * Created by dp on 15-8-22.
 */
public class ProjectPreferences extends BasePreferenceActivity {
    private PreferenceScreen preferenceScreen;
    private BitmapUtils bitmapUtils;
    private ProgressDialogUtil mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setTitle(getString(R.string.preference_project_public_title));

        mProgressDialog = new ProgressDialogUtil(this).show();

        preferenceScreen = getPreferenceManager().createPreferenceScreen(this);

        String project_type = getIntent().getExtras().getString("project_type");
        String project_id = getIntent().getExtras().getString("project_id");

        if(project_type.equals("history") && !StringUtils.isEmpty(project_id)){
            getActionBar().setTitle("History: " + project_id.substring(0,6));

            final SimpleDateFormat DATE_FORMAT =
                    new SimpleDateFormat("yyyy-MM-dd");
            final SimpleDateFormat TIME_FORMAT =
                    new SimpleDateFormat("hh:mm:ss");

            File dir = new File(Constant.FILE_FOLDER_NAME + project_id);
            if (dir.exists()){
                File[] files = dir.listFiles();
                for (File file:files){
                    PreferenceCategory preferenceCategory;
                    String title = DATE_FORMAT.format(file.lastModified());
                    if (preferenceScreen.findPreference(title) != null){
                        preferenceCategory = (PreferenceCategory)preferenceScreen.findPreference(title);
                    }else {
                        preferenceCategory= new PreferenceCategory(this);
                        preferenceCategory.setTitle(title);
                        preferenceCategory.setKey(title);
                        preferenceScreen.addPreference(preferenceCategory);
                    }
                    Preference preference = new Preference(this);
                    preference.setTitle(StringUtils.getFilename(file.getName()));
                    preference.setSummary(String.format("%s %s",TIME_FORMAT.format(new Date(file.lastModified())),
                            IOUtils.byteCountToDisplaySize(file.length())));
                    Intent intent = new Intent(ProjectPreferences.this, MainActivity.class);
                    intent.setData(Uri.fromFile(file));
                    preference.setIntent(intent);
                    preferenceCategory.addPreference(preference);
                }
            }
            synchronized(preferenceScreen) {
                preferenceScreen.notifyAll();
                setPreferenceScreen(preferenceScreen);
            }
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
        }else{
            bitmapUtils = new BitmapUtils(this);
            String url = Constant.API_PROJECT_PUBLIC;
            if (project_type.equals("private")){
                getActionBar().setTitle(getString(R.string.preference_project_private_title));
                url = Constant.API_PROJECT_PRIVATE;
            }

            HttpUtils http = XutilsHttpClient.getInstence(ProjectPreferences.this);
            http.send(HttpRequest.HttpMethod.GET, url, new RequestCallBack<String>() {
                @Override
                public void onSuccess(ResponseInfo<String> responseInfo) {
                    JSONTokener jsonParser;
                    jsonParser = new JSONTokener(responseInfo.result);
                    try{
                        JSONArray objs = (JSONArray) jsonParser.nextValue();
                        for (int i = 0; i < objs.length(); i++) {
                            try{
                                JSONObject obj = (JSONObject) objs.get(i);
                                Preference preference = new Preference(ProjectPreferences.this);
                                preference.setTitle(obj.getString("title"));
                                preference.setSummary(obj.getString("description"));
                                preference.setKey(obj.getString("objectId"));
                                Intent intent = new Intent(ProjectPreferences.this, MainActivity.class);
                                intent.setData(Uri.parse(obj.getString("url")));
                                preference.setIntent(intent);

                                preferenceScreen.addPreference(preference);
                            }catch (Exception e){
                                ToastUtil.show(e.getLocalizedMessage());
                                finish();
                            }
                        }
                    }catch (Exception e){
                        ToastUtil.show(e.getLocalizedMessage());
                        finish();
                    }
                    synchronized(preferenceScreen){
                        preferenceScreen.notifyAll();
                        setPreferenceScreen(preferenceScreen);
                    }
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                    }
                }

                @Override
                public void onFailure(HttpException e, String s) {
                    ToastUtil.show(e.getLocalizedMessage());
                    finish();
                }
            });
        }
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
