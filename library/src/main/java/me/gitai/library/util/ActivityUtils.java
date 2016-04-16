package me.gitai.library.util;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;

/**
 * Created by dphdjy on 15-11-5.
 */
public class ActivityUtils {
    public static Activity findActivityFromContext(Context ctx){
        if (ctx != null){
            if (ctx instanceof Activity){
                return (Activity)ctx;
            }
            if(ctx instanceof ContextWrapper){
                ContextWrapper cw = (ContextWrapper)ctx;
                return findActivityFromContext(cw.getBaseContext());
            }
        }
        return null;
    }
}
