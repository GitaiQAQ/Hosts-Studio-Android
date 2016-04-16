package me.gitai.hosts.ui;

import android.app.ActionBar;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import me.gitai.hosts.R;
import me.gitai.hosts.adapter.WindowListAdapter;
import me.gitai.hosts.entities.Host;
import me.gitai.hosts.widget.HostsView;
import me.gitai.hosts.widget.WindowsViewFlipper;
import me.gitai.library.ui.BaseActivity;
import me.gitai.library.util.FileUtils;
import me.gitai.library.util.IOUtils;
import me.gitai.library.util.MimeUtils;
import me.gitai.library.util.StringUtils;
import me.gitai.library.util.ToastUtil;
import me.gitai.library.widget.MaterialDialog;

/**
 * Created by dphdjy on 15-11-5.
 */
public class MainActivity extends BaseActivity{
    private WindowsViewFlipper mViewFlipper;
    private WindowListAdapter windowListAdapter;

    private List<Host> clipboard = new ArrayList<>();

    private String mUrl = "location";

    ActionBar.OnNavigationListener navigationListener = new ActionBar.OnNavigationListener() {
        @Override
        public boolean onNavigationItemSelected(int position, long id) {
            int oldPosition = mViewFlipper.getDisplayedChild();
            if (position != oldPosition){
                if (position >= mViewFlipper.getChildCount()){
                    doCreateView(null);
                }
                mViewFlipper.setDisplayedChild(position);
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i_getvalue =getIntent();

        Uri uri  = i_getvalue.getData();
        if (uri != null && StringUtils.hasText(uri.toString())){
            mUrl = uri.toString();
        }

        init();
    }

    private void init(){
        setContentView(R.layout.activity_main);
        initSystemBarTint();

        mViewFlipper = (WindowsViewFlipper)findViewById(R.id.viewFlipper);
        windowListAdapter = new WindowListAdapter(mViewFlipper);

        getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        getActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
        getActionBar().setListNavigationCallbacks(windowListAdapter, navigationListener);

        doCreateView(mUrl);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        try{
            String uri = intent.getData().toString();
            doCreateView(uri);
        }catch(Exception ex){
            ToastUtil.show(ex);
        }

    }

    private void doCreateView(String url){
        if (url != null){
            HostsView hostsView = new HostsView(this,url);

            registerForContextMenu(hostsView.getListView());
            mViewFlipper.addView(hostsView);
            mViewFlipper.setDisplayedChild(mViewFlipper.getChildCount() - 1);

            getActionBar().setSelectedNavigationItem(mViewFlipper.getChildCount() - 1);

            windowListAdapter.OnDate();
        }else{
            Intent open_intent = new Intent(Intent.ACTION_GET_CONTENT);
            open_intent.addCategory(Intent.CATEGORY_OPENABLE);
            open_intent.setType("text/plain");
            try{
                startActivityForResult(open_intent,200);
            }catch (Exception ex){
                ToastUtil.show("Please install a File Manager.");
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem act_select = menu.findItem(R.id.act_select);
        MenuItem hosts_delete_select = menu.findItem(R.id.hosts_select_delete);
        MenuItem act_paste = menu.findItem(R.id.hosts_select_paste);
        ListView hostsView = mViewFlipper.getCurrentView().getListView();
        if (hostsView.getCheckedItemCount() > 0) {
            act_select.setEnabled(true);
            act_select.setTitle(String.format(
                    getResources().getString(R.string.list_menu_selected),
                    hostsView.getCheckedItemCount()));
            hosts_delete_select.setTitle(String.format(getResources()
                    .getString(R.string.list_menu_selected_delete), hostsView
                    .getCheckedItemCount()));
        } else {
            act_select.setEnabled(false);
            hosts_delete_select.setTitle(getResources().getString(
                    R.string.list_menu_selected_none));
            act_select.setTitle(getResources().getString(
                    R.string.list_menu_selected_none));
        }

        if (clipboard.size() > 0) {
            act_paste.setEnabled(true);
        } else {
            act_paste.setEnabled(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.hosts_add:
                mViewFlipper.getCurrentView().add();
                break;
            case R.id.hosts_search:
                mViewFlipper.getCurrentView().showSearchView();
                break;
            case R.id.hosts_select_all:
                mViewFlipper.getCurrentView().selectAll();
                break;
            case R.id.hosts_select_by_same_ips:
                mViewFlipper.getCurrentView().selectBySameIps();
                break;
            case R.id.hosts_select_counter:
                mViewFlipper.getCurrentView().selectCounter();
                break;
            case R.id.hosts_select_none:
                mViewFlipper.getCurrentView().unSelect();
                break;
            case R.id.hosts_select_toggle:
                mViewFlipper.getCurrentView().selectToggle();
                break;
            case R.id.hosts_select_delete:
                mViewFlipper.getCurrentView().deleteSelect();
                break;
            case R.id.hosts_select_cut:
                clipboard = mViewFlipper.getCurrentView().cut();
                ToastUtil.showId(R.string.toast_hosts_cut, clipboard.size());
                break;
            case R.id.hosts_select_copy:
                clipboard = mViewFlipper.getCurrentView().copy();
                ToastUtil.showId(R.string.toast_hosts_copy, clipboard.size());
                break;
            case R.id.hosts_select_paste:
                mViewFlipper.getCurrentView().paste(clipboard);
                ToastUtil.showId(R.string.toast_hosts_paste, clipboard.size());
                break;
            case R.id.hosts_save:
                mViewFlipper.getCurrentView().saveHosts();
                break;
            case R.id.hosts_upload:
                mViewFlipper.getCurrentView().uploadHosts();
                break;
            case R.id.batch_ip_set:
                mViewFlipper.getCurrentView().batchIpSet();
                break;
            case R.id.hosts_backup:
                Intent backup_intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                backup_intent.addCategory(Intent.CATEGORY_OPENABLE);
                backup_intent.setType(MimeUtils.getMimeTypeFromExtension("txt"));
                backup_intent.putExtra("android.intent.extra.TITLE", IOUtils.createFileName("hosts"));
                try{
                    startActivityForResult(backup_intent,100);
                }catch(Exception ex){
                    ToastUtil.show("本功能不支持高度阉割版系统.");
                }

                break;
            case R.id.hosts_properties:
                new MaterialDialog(this)
                        .setTitle(R.string.dialog_properties_title)
                        .setMessage(String.format(getString(R.string.dialog_properties_msg),
                                mViewFlipper.getCurrentView().getTitle(),
                                mViewFlipper.getCurrentView().getUrl(),
                                mViewFlipper.getCurrentView().size()))
                        .setNegativeButton(android.R.string.ok, null)
                        .show();
                break;
            case R.id.action_settings:
                Intent intent = new Intent(this, ManagePreferences.class);
                startActivityForResult(intent, ManagePreferences.RESULT_CODE);
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.hosts_context, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int id = item.getItemId();
        int index = mViewFlipper.getCurrentView().getCurrPosition();
        switch (id){
            case R.id.cut:
                clipboard = mViewFlipper.getCurrentView().cut(index);
                break;
            case R.id.copy:
                clipboard = mViewFlipper.getCurrentView().copy(index);
                break;
            case R.id.paste:
                mViewFlipper.getCurrentView().paste(index,clipboard);
                ToastUtil.showId(R.string.toast_hosts_paste, clipboard.size());
                break;
            case R.id.add:
                mViewFlipper.getCurrentView().add(index);
                break;
            case R.id.edit:
                mViewFlipper.getCurrentView().edit(index);
                break;
            case R.id.delete:
                mViewFlipper.getCurrentView().removeHost(index);
                break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode){
            case 100:
                if (intent!=null){
                    try {
                        mViewFlipper.getCurrentView().saveAs(getContentResolver().openOutputStream(intent.getData()));
                    } catch (Exception e) {
                        ToastUtil.show(e);
                    }
                }
                break;
            case 200:
                if (intent!=null){
                    String path = FileUtils.getPath(this,intent.getData());
                    if (StringUtils.hasText(path)){
                        doCreateView("file://"+path);
                    }
                }
                break;
        }
    }
}
