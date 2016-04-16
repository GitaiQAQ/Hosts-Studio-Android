package me.gitai.hosts.widget;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import me.gitai.hosts.Constant;
import me.gitai.hosts.R;
import me.gitai.hosts.adapter.ListHostsAdapter;
import me.gitai.hosts.entities.Host;
import me.gitai.hosts.entities.User;
import me.gitai.hosts.tasks.UploadTask;
import me.gitai.hosts.utils.HostsManager;
import me.gitai.hosts.utils.ProgressDialogUtil;
import me.gitai.library.util.IOUtils;
import me.gitai.library.util.StringUtils;
import me.gitai.library.util.ToastUtil;
import me.gitai.library.widget.MaterialDialog;

/**
 * Created by gitai on 15-11-5.
 */
public class HostsView extends LinearLayout
        implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, SearchView.OnQueryTextListener, SearchView.OnCloseListener {
    private ProgressDialogUtil mProgressDialog;

    private ListView mListView;
    private ListHostsAdapter mAdapter;

    private HostsManager hostsManager;

    private MaterialDialog addMaterialDialog,editMaterialDialog,uploadMaterialDialog,batchIpSetMaterialDialog;

    private View searchView;
    private SearchView et_search;
    private CheckBox cb_regex;
    private int currPosition = -1;
    private String UID;

    public HostsView(Context context) {
        super(context);
        init();
    }

    public HostsView(Context context, String url) {
        super(context);
        init();
        loadHosts(url);
    }

    public HostsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HostsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.view_hosts, this, true);

        mProgressDialog = new ProgressDialogUtil(getContext());

        mListView = (ListView) findViewById(R.id.listHosts);
        mListView.setEmptyView(findViewById(R.id.listEmptyLayout));
        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);

        mAdapter = new ListHostsAdapter(getContext());

        mListView.setAdapter(mAdapter);
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mListView.setFastScrollEnabled(true);
        mListView.setItemsCanFocus(false);

        searchView = findViewById(R.id.searchView);

        et_search = (SearchView)findViewById(R.id.et_search);
        et_search.setOnQueryTextListener(this);
        et_search.setOnCloseListener(this);

        cb_regex  = (CheckBox)findViewById(R.id.cb_regex);

        addMaterialDialog = new MaterialDialog(getContext())
                .setContentView(R.layout.dialog_hosts_add)
                .setTitle(R.string.dialog_host_add_title)
                .setNegativeButton(android.R.string.cancel,null);
        editMaterialDialog = new MaterialDialog(getContext())
                .setContentView(R.layout.dialog_hosts_edit)
                .setTitle(R.string.dialog_host_edit_title)
                .setNegativeButton(android.R.string.cancel,null);
        uploadMaterialDialog = new MaterialDialog(getContext())
                .setTitle(R.string.dialog_upload_title)
                .setNegativeButton(android.R.string.cancel,null);
        batchIpSetMaterialDialog = new MaterialDialog(getContext())
                .setTitle(R.string.dialog_host_edit_title)
                .setContentView(R.layout.dialog_hosts_edit, new MaterialDialog.OnViewInflateListener() {
                    @Override
                    public boolean onInflate(View v) {
                        ((EditText)v.findViewById(R.id.et_hostname)).setEnabled(false);
                        ((EditText)v.findViewById(R.id.et_commit)).setVisibility(View.GONE);
                        return false;
                    }
                });
    }

    public void dismiss(){
        if (mProgressDialog != null) mProgressDialog.dismiss();
    }

    public String getTitle(){
        return IOUtils.getFileNameWithoutExtension(hostsManager.getURI());
    }

    public String getUrl(){
        return hostsManager.getURI();
    }

    public String getHash(){
        return hostsManager.getURIHash();
    }

    public int size(){
        return hostsManager.getHosts().size();
    }

    public ListView getListView(){
        return mListView;
    }
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        currPosition = i;
        return false;
    }

    public int getCurrPosition(){
        return currPosition;
    }

    public void showSearchView(){
        et_search.setIconified(false);
        searchView.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        mAdapter.getFilter(cb_regex.isChecked()).filter((CharSequence) s);
        return true;
    }

    @Override
    public boolean onClose() {
        searchView.setVisibility(View.GONE);
        return false;
    }

    public void refresh(String url){
        hostsManager.setURI(url);
        refresh();
    }

    public void refresh(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
            new HostsLoador().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }else{
            new HostsLoador().execute();
        }
    }

    public void loadHosts(String url){
        hostsManager = new HostsManager(getContext(), url);
        refresh(url);
    }

    public void saveHosts(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
            new HostSaver().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }else{
            new HostSaver().execute();
        }
    }

    public void saveAs(OutputStream output) throws IOException {
        hostsManager.createTempHostsFile(true);
        IOUtils.copy(new FileReader(new File(hostsManager.getTempHostsFile())),output);
    }

    public void uploadHosts(){
        UID = new User().getObjectId();
        if (StringUtils.isEmpty(UID)){
            ToastUtil.showId(R.string.toast_unauthorized);
            return;
        }
        final String tempHostsFile = hostsManager.getTempHostsFile();
        uploadMaterialDialog
                .setText(R.id.et_hosts, tempHostsFile)
                .setPositiveButton(android.R.string.ok, new MaterialDialog.OnClickListener() {
                    @Override
                    public boolean onClick(View v, MaterialDialog materialDialog) {
                        String title = materialDialog.getText(R.id.et_title);
                        String description = materialDialog.getText(R.id.et_description);

                        if (StringUtils.isEmpty(title)) {
                            ToastUtil.showId(R.string.toast_project_title_is_empty);
                            return false;
                        }

                        Upload upload = new Upload(getContext(),title,
                                new File(String.format(Locale.US,"%s/%s",getContext().getFilesDir().getAbsolutePath()
                                        ,tempHostsFile)),
                                ((CheckBox)materialDialog.getChildView(R.id.cb_private)).isChecked(),
                                description,UID, System.currentTimeMillis());
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
                            upload.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }else{
                            upload.execute();
                        }

                        return true;
                    }
                })
                .show();
    }

    public void add(){
        add(0);
    }

    public void add(final int index){
        addMaterialDialog
            .setText(R.id.et_ip, "")
            .setText(R.id.et_hostname, "")
            .setText(R.id.et_commit, "")
            .setPositiveButton(android.R.string.ok, new MaterialDialog.OnClickListener() {
                @Override
                public boolean onClick(View v, MaterialDialog materialDialog) {
                    String ip = materialDialog.getText(R.id.et_ip);
                    String hostname = materialDialog.getText(R.id.et_hostname);
                    String commit = materialDialog.getText(R.id.et_commit);

                    if (StringUtils.isEmpty(ip) || StringUtils.isEmpty(hostname)) {
                        ToastUtil.showId(R.string.toast_ip_or_hostname_is_empty);
                        return false;
                    }

                    hostsManager.getHosts().add(index, new Host(ip, hostname, commit));

                    notifyDataSetChanged(index, 1);
                    return true;
                }
            }).show();
    }

    public void edit(int index){
        final Host host = hostsManager.getHosts().get(index);
        editMaterialDialog
                .setText(R.id.et_ip, host.getIp())
                .setText(R.id.et_hostname, host.getHostName())
                .setText(R.id.et_commit, host.getComment())
                .setPositiveButton(android.R.string.ok, new MaterialDialog.OnClickListener() {
                    @Override
                    public boolean onClick(View v, MaterialDialog materialDialog) {
                        String ip = materialDialog.getText(R.id.et_ip);
                        String hostname = materialDialog.getText(R.id.et_hostname);
                        String commit = materialDialog.getText(R.id.et_commit);

                        if (StringUtils.isEmpty(ip) || StringUtils.isEmpty(hostname)) {
                            ToastUtil.showId(R.string.toast_ip_or_hostname_is_empty);
                            return false;
                        }

                        host.updata(ip, hostname, commit);

                        mAdapter.updateHosts(hostsManager.getHosts());

                        return true;
                    }
                }).show();
    }

    public void getHost(int index){
        hostsManager.getHosts().get(index);
    }

    public void removeHost(int index){
        hostsManager.getHosts().remove(index);
        notifyDataSetChanged(index, -1);
    }

    public void selectAll(){
        for (int i = 0; i < mListView.getCount(); i++) {
            mListView.setItemChecked(i, true);
        }
    }

    public void selectByKeyword(String keyword,boolean reg){
        if (!reg){
            keyword = String.format(".*%s.*", keyword);
        }
        List<Host> hosts = hostsManager.getHosts();
        for (int i = 0; i < mListView.getCount(); i++) {
            if (hosts.get(i).getHostName().matches(keyword)) {
                mListView.setItemChecked(i, true);
            } else {
                mListView.setItemChecked(i, false);
            }
        }
    }

    public void selectBySameIps(){
        SparseBooleanArray checked = mListView.getCheckedItemPositions().clone();
        for (int i = 0; i < mListView.getCount(); i++) {
            mListView.setItemChecked(i, false);
        }
        Set<String> ips = new HashSet();
        List<Host> hosts = hostsManager.getHosts();
        for (int j = 0; j < checked.size(); j++) {
            if (checked.valueAt(j)) {
                ips.add(hosts.get(checked.keyAt(j)).getIp());
            }
        }
        Iterator it = ips.iterator();
        while (it.hasNext()){
            String ip = (String)it.next();
            for (int i = 0; i < mListView.getCount(); i++) {
                String ip2 = hosts.get(i).getIp();

                if (ip.equals(ip2)) {
                    mListView.setItemChecked(i, true);
                }
            }
        }
    }

    public void selectCounter(){
        SparseBooleanArray checked = mListView.getCheckedItemPositions().clone();
        for (int i = 0; i < mListView.getCount(); i++) {
            mListView.setItemChecked(i, true);
        }
        for (int i = 0; i < checked.size(); i++) {
            if (checked.valueAt(i)) {
                mListView.setItemChecked(checked.keyAt(i), false);
            }
        }
    }

    public void unSelect(){
        SparseBooleanArray checked = mListView.getCheckedItemPositions().clone();
        for (int i = 0; i < checked.size(); i++) {
            if (checked.valueAt(i)) {
                mListView.setItemChecked(checked.keyAt(i), false);
            }
        }
    }

    public void selectToggle(){
        SparseBooleanArray checked = mListView.getCheckedItemPositions().clone();
        List<Host> hosts = hostsManager.getHosts();
        for (int i = 0; i < checked.size(); i++) {
            if (checked.valueAt(i)) {
                hosts.get(checked.keyAt(i)).toggleComment();
            }
        }
        mAdapter.updateHosts(hosts);
    }

    public void deleteSelect(){
        SparseBooleanArray checked = mListView.getCheckedItemPositions().clone();
        List<Host> hosts = hostsManager.getHosts();
        for (int i = checked.size()-1; i >= 0 ; i--) {
            if (checked.valueAt(i)) {
                hosts.remove(checked.keyAt(i));
                mListView.setItemChecked(checked.keyAt(i), false);
            }
        }
        mAdapter.updateHosts(hosts);
    }

    // TODO:
    public void batchIpSet(){
        SparseBooleanArray checked = mListView.getCheckedItemPositions();
        //,et_batch_ip,et_batch_commit
        batchIpSetMaterialDialog
            .setText(R.id.et_hostname,
                    String.format(getContext().getString(R.string.toast_hosts_select), checked.size()))
            .setPositiveButton(android.R.string.ok, new MaterialDialog.OnClickListener() {
                @Override
                public boolean onClick(View v, MaterialDialog materialDialog) {

                    String ip = materialDialog.getText(R.id.et_ip);

                    if (StringUtils.isEmpty(ip)) {
                        ToastUtil.showId(R.string.toast_ip_is_empty);
                        return false;
                    }

                    batchIpSet(ip);

                    return true;
                }
            }).show();
    }

    public void batchIpSet(String ip){
        SparseBooleanArray checked = mListView.getCheckedItemPositions();
        List<Host> hosts = hostsManager.getHosts();
        for (int i = 0; i < checked.size(); i++) {
            if (checked.valueAt(i)) {
                hosts.get(checked.keyAt(i)).setIp(ip);
            }
        }
        mAdapter.updateHosts(hosts);
    }

    public List<Host> cut(int index){
        List<Host> copyData = new ArrayList<>();
        List<Host> hosts = hostsManager.getHosts();
        copyData.add(hosts.get(index));
        hosts.remove(index);
        mListView.setItemChecked(index, false);
        mAdapter.updateHosts(hosts);
        return copyData;
    }

    public List<Host> cut(){
        List<Host> copyData = new ArrayList<>();
        SparseBooleanArray checked = mListView.getCheckedItemPositions();
        List<Host> hosts = hostsManager.getHosts();
        for (int i = checked.size(); i >= 0; i--) {
            if (checked.valueAt(i)) {
                copyData.add(hosts.get(checked.keyAt(i)));
                hosts.remove(checked.keyAt(i));
                mListView.setItemChecked(checked.keyAt(i), false);
            }
        }
        mAdapter.updateHosts(hosts);
        return copyData;
    }

    public List<Host> copy(int index){
        List<Host> copyData = new ArrayList<>();
        List<Host> hosts = hostsManager.getHosts();
        copyData.add(hosts.get(index));
        return copyData;
    }

    public List<Host> copy(){
        List<Host> copyData = new ArrayList<>();
        SparseBooleanArray checked = mListView.getCheckedItemPositions();
        List<Host> hosts = hostsManager.getHosts();
        for (int i = 0; i < checked.size(); i++) {
            if (checked.valueAt(i)) {
                copyData.add(hosts.get(checked.keyAt(i)));
            }
        }
        return copyData;
    }

    public void paste(int index, List<Host> mHosts){
        List<Host> hosts = hostsManager.getHosts();
        hosts.addAll(index, mHosts);
        notifyDataSetChanged(index, mHosts.size());
    }

    public void paste(List<Host> mHosts) {
        paste(0, mHosts);
    }

    public void notifyDataSetChanged(int index, int offset){
        SparseBooleanArray checked = mListView.getCheckedItemPositions().clone();
        List<Host> hosts = hostsManager.getHosts();
        mAdapter.updateHosts(hosts);
        for (int i = 0; i < checked.size(); i++) {
            boolean isCheck = checked.valueAt(i);
            if (isCheck && index <= checked.keyAt(i)){
                mListView.setItemChecked(checked.keyAt(i), false);
                mListView.setItemChecked(checked.keyAt(i) + offset, true);
            }
        }
    }

    public class Upload extends UploadTask {

        public Upload(Context ctx, String title, File file, boolean lock, String des, String uid, long time) {
            super(ctx, title, file, lock, des, uid, time);
        }

        @Override
        protected void onPreExecute() {
            if (mProgressDialog != null) {
                mProgressDialog.show();
            }
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String s) {
            if (s!=null){
                ToastUtil.show(s);
            }
            if (mProgressDialog != null) mProgressDialog.dismiss();
            super.onPostExecute(s);
        }
    }

    public class HostsLoador extends AsyncTask<Void,Void,Void>{
        @Override
        protected void onPreExecute() {
            if (mProgressDialog != null){
                mProgressDialog.show();
            }
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            hostsManager.getHosts(true);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mProgressDialog != null) mProgressDialog.dismiss();
            mAdapter.updateHosts(hostsManager.getHosts());
            super.onPostExecute(aVoid);
        }
    }

    public class HostSaver extends AsyncTask<Void,Void,Integer>{
        @Override
        protected void onPreExecute() {
            if (mProgressDialog != null){
                mProgressDialog.show();
                mProgressDialog.setMessage(R.string.saving);
            }
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(Void... params) {
            if(hostsManager.getURI().endsWith("whilelist")){
                return hostsManager.saveHosts(Constant.FILE_FOLDER_NAME + "whilelist", false);
            }
            return hostsManager.saveToSysHosts();
        }

        @Override
        protected void onPostExecute(Integer resultCode) {
            if (mProgressDialog != null) mProgressDialog.dismiss();
            String msg = getContext().getString(R.string.msg_save_success);;
            switch (resultCode){
                case HostsManager.SUCCESS:
                    msg = getContext().getString(R.string.msg_save_success);
                    break;
                case HostsManager.ERROR_CAN_NOT_CREATE_TEMPORARY_HOSTS_FILE:
                    msg = getContext().getString(R.string.cant_create_temporary_hosts_file);
                    break;
                case HostsManager.ERROR_CAN_NOT_GET_ROOT_ACCESS:
                    msg = getContext().getString(R.string.cant_get_root_access);
                    break;
                default:
                    msg = String.format("Unknow,resultCode: %s", resultCode);
            }
            ToastUtil.show(msg);
            super.onPostExecute(resultCode);
        }
    }

}
