package me.gitai.library.util.jsevaluator.interfaces;

public interface WebViewWrapperInterface {
	public void loadJavaScript(String javascript);
	public void addJsInterface(Object obj, String name);
}
