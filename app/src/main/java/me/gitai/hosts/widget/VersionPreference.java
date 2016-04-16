package me.gitai.hosts.widget;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;
import android.text.Html;
import android.util.AttributeSet;

import me.gitai.hosts.BuildConfig;
import me.gitai.hosts.R;
import me.gitai.hosts.tasks.UpdataTask;
import me.gitai.hosts.utils.ProgressDialogUtil;
import me.gitai.library.util.ToastUtil;

/**
 * Created by dphdjy on 15-11-5.
 */
public class VersionPreference extends Preference implements Preference.OnPreferenceClickListener {

    private boolean isLaster = false;

    public VersionPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public VersionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VersionPreference(Context context) {
        super(context);
        init();
    }


    private void init(){
        setSummary(
                String.format(
                        "%s %s-%s(%s)",
                        getContext().getString(R.string.app_name),
                        BuildConfig.VERSION_NAME,
                        BuildConfig.BUILD_TYPE,
                        BuildConfig.VERSION_CODE));

        setOnPreferenceClickListener(this);
    }


    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (!isLaster){
            new Updata().execute();
        }else{
            ToastUtil.show(getContext().getString(R.string.toast_application_is_up_to_date));
        }
        return false;
    }

    public class Updata extends UpdataTask {
        ProgressDialogUtil mProgressDialog;
        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialogUtil(getContext()).setMessage(R.string.updata).show();
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(final UpdataInfo result) {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
            //TODO:<
            if (result!=null && BuildConfig.VERSION_CODE < result.getVersionCode()) {
                AlertDialog.Builder builer = new AlertDialog.Builder(getContext());
                builer.setTitle(result.getTitle());
                builer.setMessage(Html.fromHtml(result.getMsg()));
                if (result.getUrl().length()>10){
                    builer.setPositiveButton(getContext().getString(R.string.updata), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Uri uri = Uri.parse(result.getUrl());
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);

                            getContext().startActivity(intent);
                            return;
                        }
                    });
                }
                if (result.getLevelCode() == 1){
                    builer.setNegativeButton(getContext().getString(R.string.btn_cancel), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                }
                AlertDialog dialog = builer.create();
                dialog.show();
            }else{
                isLaster = true;
                ToastUtil.show(getContext().getString(R.string.toast_application_is_up_to_date));
            }
            super.onPostExecute(result);
        }
    }
}
