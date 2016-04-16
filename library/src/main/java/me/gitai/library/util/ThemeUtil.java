package me.gitai.library.util;

import android.content.Context;

import me.gitai.library.BaseApplication;

/**
 * Created by dphdjy on 15-10-22.
 */
public class ThemeUtil {
    public static int getColor(Context paramContext, int id, int arraykey) {
        return paramContext.getResources().getIntArray(arraykey)[SharedPreferencesUtil.getInstence(paramContext.getPackageName()).getInt(paramContext.getResources().getString(id), 0)];
    }
}
