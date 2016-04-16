package me.gitai.hosts.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.gitai.hosts.entities.Host;
import me.gitai.hosts.widget.CheckableHostItem;
import me.gitai.library.util.L;
import me.gitai.library.util.StringUtils;
import me.gitai.library.util.ThreadUtils;
import me.gitai.library.util.ViewUtils;

public class ListHostsAdapter extends BaseAdapter {

    private List<Host> originalHosts = Collections.emptyList();
    private List<Host> filteredHosts = Collections.emptyList();
    private Context mAppContext;
    private HostsFilter mFilter = new HostsFilter();

    private int mIpMinWidth;
    private int mIpMaxWidth;

    public ListHostsAdapter(Context appContext){
        mAppContext = appContext;
    }

    public void updateHosts(List<Host> hosts) {
        if (hosts == null) {
            originalHosts = Collections.emptyList();
            filteredHosts = Collections.emptyList();
        } else {
            originalHosts = hosts;
            filteredHosts = hosts;
        }
        notifyDataSetChanged();
    }

    public void computeViewWidths(Context context) {
        // The IP column can be very large, especially when it holds ipv6 with 39 chars.
        // Its size has to be generated programmatically, as it should fit with the device's width.
        // A tablet can afford having a large column but not a phone.


        int screenWidth = ViewUtils.getScreenRawSize(context).x;

        // 1: Compute minimum width.
        // Min width must be between [100dp, 160dp]. If possible, 30% of screen width.
        int minWidth = screenWidth * 30 / 100;
        int minRange = ViewUtils.dpToPx(100f);
        int maxRange = ViewUtils.dpToPx(160f);

        if (minWidth < minRange) {
            minWidth = minRange;
        }
        if (minWidth > maxRange) {
            minWidth = maxRange;
        }

        // 2: Compute maximum width, usually 35% of screen width.
        int maxWidth = screenWidth * 35 / 100;
        if (maxWidth < minWidth) {
            maxWidth = minWidth;
        }

        L.d("Min width: " + minWidth + " - Max width: " + maxWidth);
        mIpMinWidth = minWidth;
        mIpMaxWidth = maxWidth;
    }

    @Override
    public int getCount() {
        return filteredHosts.size();
    }

    @Override
    public Host getItem(int position) {
        return filteredHosts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CheckableHostItem view;

        if (convertView == null) {
            view = new CheckableHostItem(mAppContext);
        } else {
            view = (CheckableHostItem) convertView;
        }

        Host host = getItem(position);
        view.init(host, mIpMinWidth, mIpMaxWidth);
        return view;
    }

	public Filter getFilter(boolean regex) {
        mRegex = regex;
		return mFilter;
	}

    private boolean mRegex;

    public class HostsFilter extends Filter{

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            String filterString = constraint.toString().toLowerCase();
            FilterResults filterResults = new FilterResults();
            final List<Host> list = originalHosts;
            List<Host> nlist = new ArrayList<>();

            if (!StringUtils.hasText(filterString)){
                nlist = list;
            }else{
                if (mRegex){
                    for (int i = 0; i < list.size(); i++) {
                        Host host = list.get(i);
                        if (host.matcher(filterString)){
                            nlist.add(host);
                        }
                    }
                }else{
                    for (int i = 0; i < list.size(); i++) {
                        Host host = list.get(i);
                        if (host.contains(filterString)){
                            nlist.add(host);
                        }
                    }
                }
            }
            filterResults.values = nlist;
            filterResults.count = nlist.size();
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredHosts = (List<Host>)results.values;
            notifyDataSetChanged();
        }
    }
}
