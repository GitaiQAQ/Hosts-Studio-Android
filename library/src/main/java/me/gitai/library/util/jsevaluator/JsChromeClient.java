package me.gitai.library.util.jsevaluator;

import android.webkit.ConsoleMessage;
import android.webkit.ConsoleMessage.MessageLevel;
import android.webkit.WebChromeClient;

import me.gitai.library.util.L;

import static android.webkit.ConsoleMessage.MessageLevel.*;

/**
 * Created by dphdjy on 15-10-31.
 */
public class JsChromeClient extends WebChromeClient {


    private class Console{
        public void log(String s){
            L.i(s);
        }
        public void err(String s){
            L.e(s);
        }
        public void error(String s){
            L.e(s);
        }
        public void debug(String s){
            L.d(s);
        }
        public void warn(String s){
            L.w(s);
        }
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        switch (consoleMessage.messageLevel()){
            case DEBUG:
                L.d(consoleMessage.message());
                break;
            case ERROR:
                L.e(consoleMessage.message());
                break;
            case LOG:
                L.i(consoleMessage.message());
                break;
            case WARNING:
                L.w(consoleMessage.message());
                break;
            case TIP:
                L.i(consoleMessage.message());
                break;
        }
        return super.onConsoleMessage(consoleMessage);
    }

    @Override
    public void onConsoleMessage(String message, int lineNumber, String sourceID) {
        super.onConsoleMessage(message, lineNumber, sourceID);
        L.i(message);
    }
}
