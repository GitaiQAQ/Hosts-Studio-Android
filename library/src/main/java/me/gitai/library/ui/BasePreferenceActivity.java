package me.gitai.library.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.readystatesoftware.systembartint.SystemBarTintManager;

import me.gitai.library.R;
import me.gitai.library.util.L;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class BasePreferenceActivity extends PreferenceActivity{
	private boolean mPaused;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		L.d();

		setContentView(R.layout.view_list_empty);
        getListView().setEmptyView(getWindow().findViewById(R.id.empty));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getListView().setPadding(0,0,0,0);
        }

		//initSystemBar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            setFitsSystemWindows(true);
            setTranslucentStatus(true);
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setStatusBarTintResource(R.color.primary_dark);
        }

        if (getActionBar() != null){
            getActionBar().setDisplayShowHomeEnabled(false);
        }
	}

	@Override
	protected void onResume() {
		super.onResume();
		L.d();

		mPaused = false;
	}

	@Override
    protected void onPostResume() {
        super.onPostResume();
        L.d();
    }

	@Override
	protected void onPause() {
		super.onPause();
		L.d();

		mPaused = true;
	}

	@Override
    protected void onDestroy() {
        super.onDestroy();
        L.d();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        L.d();
    }

    protected boolean isPaused() {
        return mPaused;
    }

    protected void onHomeClick() {
        if (isFinishing()) {
            return;
        }

        L.d();
        onBackPressed();
    }

    protected BasePreferenceActivity getActivity() {
        return this;
    }

    public void showProgressIndicator() {
        setProgressBarIndeterminateVisibility(true);
    }

    public void hideProgressIndicator() {
        setProgressBarIndeterminateVisibility(false);
    }

    public void setActionBarTitle(CharSequence text) {
        getActionBar().setTitle(text);
    }

    public void setActionBarTitle(int resId) {
        getActionBar().setTitle(resId);
    }

    public void setActionBarSubtitle(CharSequence text) {
        getActionBar().setSubtitle(text);
    }

    public void setActionBarSubtitle(int resId) {
        getActionBar().setSubtitle(resId);
    }

	@TargetApi(14)
	private void setFitsSystemWindows(boolean on) {
		((ViewGroup)findViewById(android.R.id.content)).getChildAt(0).setFitsSystemWindows(on);
	}

	@TargetApi(19)
	private void setTranslucentStatus(boolean on) {
		Window win = getWindow();
		WindowManager.LayoutParams winParams = win.getAttributes();
		final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
		if (on) {
			winParams.flags |= bits;
		} else {
			winParams.flags &= ~bits;
		}
		win.setAttributes(winParams);
	}
}
