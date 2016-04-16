package me.gitai.hosts.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ViewFlipper;

/**
 * Created by dphdjy on 15-11-5.
 */
public class WindowsViewFlipper extends ViewFlipper {
    public WindowsViewFlipper(Context context) {
        super(context);
    }

    public WindowsViewFlipper(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void addView(View child) {
        super.addView(child);
    }

    @Override
    public void addView(View child, int index) {
        super.addView(child, index);
    }

    public HostsView getView(int i){
        return (HostsView)getChildAt(i);
    }

    @Override
    public HostsView getCurrentView() {
        return (HostsView)super.getCurrentView();
    }
}
