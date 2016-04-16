package me.gitai.library.util.jsevaluator;

import android.os.Handler;

import me.gitai.library.util.jsevaluator.interfaces.HandlerWrapperInterface;

public class HandlerWrapper implements HandlerWrapperInterface {
	private final Handler mHandler;

	public HandlerWrapper() {
		mHandler = new Handler();
	}

	@Override
	public void post(Runnable r) {
		mHandler.post(r);
	}
}
