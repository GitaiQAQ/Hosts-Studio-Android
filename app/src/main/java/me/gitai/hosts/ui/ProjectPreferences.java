package me.gitai.hosts.ui;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
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
import com.umeng.analytics.MobclickAgent;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import me.gitai.hosts.utils.ProgressDialogUtil;
import me.gitai.hosts.utils.XutilsHttpClient;
import me.gitai.hosts.*;
import me.gitai.library.ui.BasePreferenceActivity;
import me.gitai.library.util.SharedPreferencesUtil;
import me.gitai.library.util.ToastUtil;

/**
 * Created by dp on 15-8-22.
 */
public class ProjectPreferences extends BasePreferenceActivity {
    private PreferenceScreen preferenceScreen;
    private BitmapUtils bitmapUtils;
    private ProgressDialogUtil mProgressDialog;

    @Override
    protected void onResume() {
        super.onResume();
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

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setTitle(getString(R.string.preference_project_public_title));

        mProgressDialog = new ProgressDialogUtil(this).show();

        preferenceScreen = getPreferenceManager().createPreferenceScreen(this);

        bitmapUtils = new BitmapUtils(this);

        String project_type =getIntent().getExtras().getString("project_type");

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
