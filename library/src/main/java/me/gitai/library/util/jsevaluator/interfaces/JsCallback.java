package me.gitai.library.util.jsevaluator.interfaces;

/**
 * Interface for passing code that will be executed after the JS has finished
 */

public interface JsCallback {
	public abstract void onResult(String value);
}
