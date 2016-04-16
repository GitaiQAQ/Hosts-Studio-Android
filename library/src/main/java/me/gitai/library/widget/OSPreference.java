package me.gitai.library.widget;

import android.content.Context;
import android.os.Build;
import android.preference.Preference;
import android.util.AttributeSet;

/**
 * Created by dphdjy on 15-11-5.
 */
public class OSPreference extends Preference {

    public OSPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public OSPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OSPreference(Context context) {
        super(context);
        init();
    }

    private void init(){
        setSummary(String.format(
                "Android %s;%s/%s(%s)",
                Build.VERSION.RELEASE,
                Build.DEVICE,
                Build.ID,
                Build.PRODUCT));
    }
}
