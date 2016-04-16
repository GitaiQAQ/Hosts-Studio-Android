package me.gitai.hosts.adapter;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import me.gitai.hosts.R;
import me.gitai.hosts.widget.HostsView;
import me.gitai.hosts.widget.WindowsViewFlipper;
import me.gitai.library.util.ActivityUtils;

/**
 * Created by dphdjy on 15-11-5.
 */
public class WindowListAdapter extends BaseAdapter {
    private final WindowsViewFlipper mViewFlipper;

    public WindowListAdapter(WindowsViewFlipper mViewFlipper){
        this.mViewFlipper = mViewFlipper;
    }

    @Override
    public int getCount() {
        return mViewFlipper.getChildCount() + 1;
    }

    @Override
    public HostsView getItem(int i) {
        return mViewFlipper.getView(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup parent) {
        Activity act = ActivityUtils.findActivityFromContext(parent.getContext());
        View child = act.getLayoutInflater().inflate(R.layout.windows_list_item, parent, false);

        TextView tv_label = (TextView)child.findViewById(R.id.windows_label);

        if (i >= getCount() - 1){
            tv_label.setText("New +");
        }else{
            tv_label.setText(getItem(i).getTitle());
        }
        return child;
    }

    public void OnDate(){
        notifyDataSetChanged();
    }
}
