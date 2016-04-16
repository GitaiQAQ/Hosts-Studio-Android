package me.gitai.hosts.utils;

import android.app.ProgressDialog;
import android.content.Context;

import me.gitai.hosts.R;

/**
 * Created by dphdjy on 15-11-5.
 */
public class ProgressDialogUtil {
    private ProgressDialog mProgressDialog;
    private Context ctx;

    public ProgressDialogUtil(Context ctx){
        this.ctx = ctx;

        mProgressDialog = new ProgressDialog(ctx);
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setMessage(ctx.getString(R.string.loading));
    }

    public ProgressDialogUtil setMessage(int i){
        mProgressDialog.setMessage(ctx.getString(i));
        return this;
    }

    public ProgressDialogUtil show(){
        mProgressDialog.show();
        return this;
    }

    public ProgressDialogUtil dismiss(){
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        return this;
    }
}
