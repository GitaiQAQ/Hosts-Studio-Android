package me.gitai.hosts.widget;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import me.gitai.hosts.R;
import me.gitai.hosts.adapter.ListHostsAdapter;
import me.gitai.hosts.entities.Host;
import me.gitai.hosts.entities.User;
import me.gitai.hosts.tasks.UploadTask;
import me.gitai.hosts.utils.HostsManager;
import me.gitai.hosts.utils.ProgressDialogUtil;
import me.gitai.library.util.IOUtils;
import me.gitai.library.util.L;
import me.gitai.library.util.StringUtils;
import me.gitai.library.util.ToastUtil;
import me.gitai.library.widget.MaterialDialog;

/**
 * Created by dphdjy on 15-11-5.
 */
public class HostsView extends LinearLayout implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, SearchView.OnQueryTextListener, SearchView.OnCloseListener {
    private ProgressDialogUtil mProgressDialog;

    private ListView mListView;
    private ListHostsAdapter mAdapter;

    private HostsManager hostsManager;
    private List<Host> hosts;

    private int add_index,edit_index;
    private EditText et_add_hostname,et_add_ip,et_add_commit,et_edit_hostname,et_edit_ip,et_edit_commit,et_updata_title,et_updata_hosts,et_updata_description;
    private CheckBox cb_updata_private;

    private MaterialDialog addMaterialDialog,editMaterialDialog,uploadMaterialDialog;

    private String mUrl = "location";

    private View searchView;
    private SearchView et_search;
    private CheckBox cb_regex;
    private int currPosition = -1;
    private String tempHostsFile;
    private String UID;

    public HostsView(Context context) {
        super(context);
        init();
    }

    public HostsView(Context context, String url) {
        super(context);
        this.mUrl = url;
        init();
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
                .setContentView(R.layout.dialog_hosts_add, new MaterialDialog.OnViewInflateListener() {
                    @Override
                    public boolean onInflate(View v) {
                        et_add_ip = (EditText)v.findViewById(R.id.et_ip);
                        et_add_hostname = (EditText)v.findViewById(R.id.et_hostname);
                        et_add_commit = (EditText)v.findViewById(R.id.et_commit);
                        return false;
                    }
                })
                .setTitle(R.string.dialog_host_add_title)
                .setPositiveButton(android.R.string.ok, new MaterialDialog.OnClickListener() {
                    @Override
                    public boolean onClick(View v, View MaterialDialog) {
                        String ip = et_add_ip.getText().toString();
                        String hostname = et_add_hostname.getText().toString();
                        String commit = et_add_commit.getText().toString();

                        if (StringUtils.isEmpty(ip) || StringUtils.isEmpty(hostname)) {
                            ToastUtil.showId(R.string.toast_ip_or_hostname_is_empty);
                            return false;
                        }

                        Host host = new Host(ip, hostname, commit);

                        hosts.add(add_index, host);

                        notifyDataSetChanged(add_index, 1);
                        return true;
                    }
                })
                .setNegativeButton(android.R.string.cancel,null);
        editMaterialDialog = new MaterialDialog(getContext())
                .setContentView(R.layout.dialog_hosts_edit, new MaterialDialog.OnViewInflateListener() {
                    @Override
                    public boolean onInflate(View v) {
                        et_edit_ip = (EditText)v.findViewById(R.id.et_ip);
                        et_edit_hostname = (EditText)v.findViewById(R.id.et_hostname);
                        et_edit_commit = (EditText)v.findViewById(R.id.et_commit);
                        return false;
                    }
                })
                .setTitle(R.string.dialog_host_edit_title)
                .setPositiveButton(android.R.string.ok, new MaterialDialog.OnClickListener() {
                    @Override
                    public boolean onClick(View v, View MaterialDialog) {
                        String ip = et_edit_ip.getText().toString();
                        String hostname = et_edit_hostname.getText().toString();
                        String commit = et_edit_commit.getText().toString();

                        if (StringUtils.isEmpty(ip) || StringUtils.isEmpty(hostname)) {
                            ToastUtil.showId(R.string.toast_ip_or_hostname_is_empty);
                            return false;
                        }

                        Host host = hosts.get(edit_index);
                        host.updata(ip, hostname, commit);

                        mAdapter.updateHosts(hosts);

                        return true;
                    }
                })
                .setNegativeButton(android.R.string.cancel,null);
        uploadMaterialDialog = new MaterialDialog(getContext())
                .setTitle(R.string.dialog_upload_title)
                .setContentView(R.layout.dialog_hosts_updata, new MaterialDialog.OnViewInflateListener() {
                    @Override
                    public boolean onInflate(View v) {
                        et_updata_title = (EditText) v.findViewById(R.id.et_title);
                        et_updata_hosts = (EditText) v.findViewById(R.id.et_hosts);
                        cb_updata_private = (CheckBox) v.findViewById(R.id.cb_private);
                        et_updata_description = (EditText) v.findViewById(R.id.et_description);
                        return false;
                    }
                })
                .setPositiveButton(android.R.string.ok, new MaterialDialog.OnClickListener() {
                    @Override
                    public boolean onClick(View v, View MaterialDialog) {
                        String title = et_updata_title.getText().toString();
                        String description = et_updata_description.getText().toString();

                        if (StringUtils.isEmpty(title)) {
                            ToastUtil.showId(R.string.toast_project_title_is_empty);
                            return false;
                        }

                        File tempFile = new File(String.format(Locale.US,"%s/%s",getContext().getFilesDir().getAbsolutePath(),tempHostsFile));

                        Upload upload = new Upload(getContext(),title,tempFile,cb_updata_private.isChecked(),description,UID, System.currentTimeMillis());
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
                            upload.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }else{
                            upload.execute();
                        }

                        return true;
                    }
                })
                .setNegativeButton(android.R.string.cancel,null);
        refresh();
    }

    public String getTitle(){
        return IOUtils.getFileNameWithoutExtension(mUrl);
    }

    public String getUrl(){
        return mUrl;
    }

    public int size(){
        return hosts.size();
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

    private void refresh(){
        hostsManager = new HostsManager(getContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
            new HostsLoador().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mUrl);
        }else{
            new HostsLoador().execute(mUrl);
        }
    }

    public void loadHosts(String url){
        mUrl = url;
        refresh();
    }

    public void saveHosts(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
            new HostSaver().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }else{
            new HostSaver().execute();
        }
    }

    public void saveAs(OutputStream output) throws IOException {
        IOUtils.copy(getContext().openFileInput(hostsManager.createTempHostsFile(getContext())),output);
    }

    public void uploadHosts(){
        UID = new User().getObjectId();
        if (StringUtils.isEmpty(UID)){
            ToastUtil.showId(R.string.toast_unauthorized);
            return;
        }
        tempHostsFile = hostsManager.createTempHostsFile(getContext());
        et_updata_hosts.setText(tempHostsFile);
        uploadMaterialDialog.show();
    }

    public void addHost(int index, Host host){
        hosts.add(index, host);
        notifyDataSetChanged(index, 1);
    }

    public void add(){
        add(0);
    }

    public void add(int index){
        add_index = index;
        et_add_hostname.setText("");
        et_add_ip.setText("");;
        et_add_commit.setText("");
        addMaterialDialog.show();
    }

    public void edit(int index){
        edit_index = index;
        Host host = hosts.get(index);
        if (!host.isBuild()){
            host.build();
        }
        et_edit_hostname.setText(host.getHostName());
        et_edit_ip.setText(host.getIp());;
        et_edit_commit.setText(host.getComment());
        editMaterialDialog.show();
    }

    public void getHost(int index){
        hosts.get(index);
    }

    public void removeHost(int index){
        hosts.remove(index);
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
        for (int j = 0; j < checked.size(); j++) {
            if (checked.valueAt(j)) {
                String ip1 = hosts.get(checked.keyAt(j)).getIp();
                for (int i = 0; i < mListView.getCount(); i++) {
                    String ip2 = hosts.get(i).getIp();

                    if (ip1.equals(ip2)) {
                        mListView.setItemChecked(i, true);
                    }
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
        for (int i = 0; i < checked.size(); i++) {
            if (checked.valueAt(i)) {
                hosts.get(checked.keyAt(i)).toggleComment();
            }
        }
        mAdapter.updateHosts(hosts);
    }

    public void deleteSelect(){
        SparseBooleanArray checked = mListView.getCheckedItemPositions().clone();
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

    }

    public void batchIpSet(String ip){
        SparseBooleanArray checked = mListView.getCheckedItemPositions();
        for (int i = 0; i < checked.size(); i++) {
            if (checked.valueAt(i)) {
                hosts.get(checked.keyAt(i))
                        .setIp(ip);
            }
        }
        mAdapter.updateHosts(hosts);
    }

    public List<Host> cut(int index){
        List<Host> copyData = new ArrayList<>();
        SparseBooleanArray checked = mListView.getCheckedItemPositions();
        copyData.add(hosts.get(index));
        hosts.remove(index);
        mListView.setItemChecked(index, false);
        mAdapter.updateHosts(hosts);
        return copyData;
    }

    public List<Host> cut(){
        List<Host> copyData = new ArrayList<>();
        SparseBooleanArray checked = mListView.getCheckedItemPositions();
        for (int i = 0; i < checked.size(); i++) {
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
        copyData.add(hosts.get(index));
        return copyData;
    }

    public List<Host> copy(){
        List<Host> copyData = new ArrayList<>();
        SparseBooleanArray checked = mListView.getCheckedItemPositions();
        for (int i = 0; i < checked.size(); i++) {
            if (checked.valueAt(i)) {
                copyData.add(hosts.get(checked.keyAt(i)));
            }
        }
        return copyData;
    }

    public void paste(int index, List<Host> mHosts){
        hosts.addAll(index, mHosts);
        notifyDataSetChanged(index, mHosts.size());
    }

    public void paste(List<Host> mHosts) {
        paste(0, mHosts);
    }

    public void notifyDataSetChanged(int index, int offset){
        SparseBooleanArray checked = mListView.getCheckedItemPositions().clone();
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
            mProgressDialog.show();
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String s) {
            if (s!=null){
                ToastUtil.show(s);
            }

            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
            super.onPostExecute(s);
        }
    }

    public class HostsLoador extends AsyncTask<String,Void,Void>{
        @Override
        protected void onPreExecute() {
            mProgressDialog.show();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... params) {
            if (StringUtils.hasText(params[0]) && !params[0].equals("location")){
                hosts = hostsManager.getHosts(params[0],true);
            }else{
                hosts = hostsManager.getHosts(true);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
            mAdapter.updateHosts(hosts);
            super.onPostExecute(aVoid);
        }
    }

    public class HostSaver extends AsyncTask<Void,Void,String>{
        @Override
        protected void onPreExecute() {
            mProgressDialog.show();
            mProgressDialog.setMessage(R.string.saving);
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... params) {
            return hostsManager.saveHosts();
        }

        @Override
        protected void onPostExecute(String string) {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
            mAdapter.updateHosts(hosts);
            if (StringUtils.hasText(string)){
                ToastUtil.show(string);
            }else{
                ToastUtil.show(getContext().getString(R.string.msg_save_success));
            }
            super.onPostExecute(string);
        }
    }

}
