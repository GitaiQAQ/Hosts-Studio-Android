package me.gitai.library.util.jsevaluator.interfaces;

/**
 * Used in JavaScriptInterface to interact with JsRunner
 */
public interface CallJavaResultInterface {
	public void jsCallFinished(String value, Integer callIndex);
}
