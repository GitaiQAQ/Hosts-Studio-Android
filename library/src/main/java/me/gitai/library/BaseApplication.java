package me.gitai.library;

import android.app.Application;

import com.orm.SugarApp;

import me.gitai.library.util.L;
import me.gitai.library.util.SharedPreferencesUtil;
import me.gitai.library.util.ToastUtil;

public class BaseApplication extends SugarApp implements Thread.UncaughtExceptionHandler {
	protected static BaseApplication _this = null;

	@Override
	public void onCreate() {
		super.onCreate();
		_this = this;

		L.setLogcatEnable(this, true);
		L.setLogToFileEnable(true, this);

		ToastUtil.initialize(this);
		SharedPreferencesUtil.initialize(this);

		L.d();
	}

	public static BaseApplication getApplication() {
		return _this;
	}

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		L.e(ex);
		System.exit(1);
	}
}
