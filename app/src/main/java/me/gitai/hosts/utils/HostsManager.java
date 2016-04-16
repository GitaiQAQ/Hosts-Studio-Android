package me.gitai.hosts.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.stericson.RootShell.RootShell;
import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

import me.gitai.hosts.Constant;
import me.gitai.hosts.R;
import me.gitai.hosts.entities.Host;
import me.gitai.library.util.L;
import me.gitai.library.util.SharedPreferencesUtil;
import me.gitai.library.util.ToastUtil;

public class HostsManager {

	private static final String UTF_8 = "UTF-8";
	private static final String HOSTS_FILE_NAME = "hosts";
	private static final String HOSTS_FILE_PATH = "/system/etc/"
			+ HOSTS_FILE_NAME;

	private static final String LINE_SEPARATOR = System.getProperty(
			"line.separator", "\n");
	private static final String MOUNT_TYPE_RO = "ro";
	private static final String MOUNT_TYPE_RW = "rw";
	private static final String COMMAND_RM = "rm -f";
	private static final String COMMAND_CHOWN = "chown 0:0";
	private static final String COMMAND_CHMOD_644 = "chmod 644";
	private static final String COMMAND_CHATTR = "chattr -i";

	private boolean preBuild = false;

	private Boolean Addto = false;

	private Context mContext;

	// Do not access this field directly even in the same class, use
	// getAllHosts() instead.
	private List<Host> mHosts;

	public HostsManager() {
	}

	public HostsManager(Context context){
		this.mContext = context;
	}

	/**
	 * Gets all host entries from hosts file.
	 * <p>
	 * <b>Must be in an async call.</b>
	 * </p>
	 * 
	 * @param forceRefresh
	 *            if we want to force using the hosts file (not the cache)
	 * @return a list of host entries
	 */
	public synchronized List<Host> getHosts(boolean forceRefresh) {
		return getHosts(new File(HOSTS_FILE_PATH), forceRefresh);
	}

	public synchronized List<Host> getHosts(String path, boolean forceRefresh) {
		if (mHosts == null || forceRefresh || Addto) {
			if (!Addto || mHosts == null)
				mHosts = Collections.synchronizedList(new ArrayList<Host>());
			InputStream in = null;
			try {
				if (path.equals("default")){
					in = mContext.getResources().openRawResource(R.raw.hosts);
				}else{
					in = new URL(path).openStream();
				}

				getHosts(in);
			} catch (IOException e) {
				L.e(e, "I/O error while opening hosts file");
			} finally {
				if (in != null) {
					IOUtils.closeQuietly(in);
				}
			}
			Addto = false;
		}
		return mHosts;
	}

	public synchronized List<Host> getHosts(File hostsPath, boolean forceRefresh) {
		if (mHosts == null || forceRefresh || Addto) {
			if (!Addto || mHosts == null)
				mHosts = Collections.synchronizedList(new ArrayList<Host>());

			try{
				getHosts(new FileReader(hostsPath));
			}catch (Exception ex){
				L.e(ex, "I/O error while opening hosts file");
			}

			Addto = false;
		}
		return mHosts;
	}

	private synchronized List<Host> getHosts(InputStream inputStream) throws IOException {
		return getHosts(new BufferedReader(new InputStreamReader(inputStream)));
	}

	private synchronized List<Host> getHosts(FileReader fileReader) throws IOException {
		return getHosts(new BufferedReader((Reader) fileReader));
	}

	private synchronized List<Host> getHosts(BufferedReader bufferedReader) throws IOException {
		preBuild = !SharedPreferencesUtil.getInstence(null).getBoolean(Constant.PREFERENCE_KEY_MODEL_QUICKLOAD,false);
		long time = System.currentTimeMillis();
		String line = bufferedReader.readLine();
		while (line != null){
			Host host = Host.fromString(line,preBuild);
			if (host != null && host.isValid()) {
				mHosts.add(host);
			}
			line = bufferedReader.readLine();
		}
		bufferedReader.close();
		L.e("parseHosts: " + (System.currentTimeMillis() - time) + " ms");
		return mHosts;
	}

	public void setisAddTo(Boolean isaddto) {
		Addto = isaddto;
	}

	public synchronized String saveHosts() {
		return saveHosts(mContext);
	}
	/**
	 * Saves new hosts file and creates a backup of previous file.
	 * <p>
	 * <b>Must be in an async call.</b>
	 * </p>
	 * 
	 * @param appContext
	 *            application context
	 * @return {@code true} if everything was working as expected, or
	 *         {@code false} otherwise
	 */
	public synchronized String saveHosts(Context appContext) {
		if (!RootTools.isAccessGiven()) {
			if (mContext!=null){
				return mContext.getString(R.string.cant_get_root_access);
			}else{
				return "Can't get root access";
			}
		}

		// Step 1: Create temporary hosts file in
		// /data/data/project_package/files/hosts
		String fn = createTempHostsFile(appContext);
		if (fn==null) {
			if (mContext!=null){
				return mContext.getString(R.string.cant_create_temporary_hosts_file);
			}else{
				return "Can't create temporary hosts file";
			}
		}

		String tmpFile = String.format(Locale.US, "%s/%s", appContext
				.getFilesDir().getAbsolutePath(), fn);
		String backupFile = String.format(Locale.US, "%s.bak", tmpFile);

		// Step 2: Get canonical path for /etc/hosts (it could be a symbolic
		// link)
		String hostsFilePath = HOSTS_FILE_PATH;
		File hostsFile = new File(HOSTS_FILE_PATH);
		if (hostsFile.exists()) {
			try {
				if (FileUtils.isSymlink(hostsFile)) {
					hostsFilePath = hostsFile.getCanonicalPath();
				}
			} catch (IOException e1) {
				L.e(e1, "Can't find hosts file");
			}
		} else {
			L.w("Hosts file was not found in filesystem");
		}

		try {
			// Step 3: Create backup of current hosts file (if any)
			RootTools.remount(hostsFilePath, MOUNT_TYPE_RW);
			runRootCommand(COMMAND_RM, backupFile);
			RootTools.copyFile(hostsFilePath, backupFile, false, true);

			// Step 4: Replace hosts file with generated file
			runRootCommand(COMMAND_RM, hostsFilePath);
			RootTools.copyFile(tmpFile, hostsFilePath, false, true);

			// Step 5: Give proper rights
			runRootCommand(COMMAND_CHATTR, hostsFilePath);
			runRootCommand(COMMAND_CHOWN, hostsFilePath);
			runRootCommand(COMMAND_CHMOD_644, hostsFilePath);

			// Step 6: Delete local file
			//appContext.deleteFile(HOSTS_FILE_NAME);
		} catch (Exception e) {
			L.e(e, "Failed running root command");
			if (mContext!=null){
				return mContext.getString(R.string.failed_running_root_command);
			}else{
				return "Failed running root command";
			}
		} finally {
			RootTools.remount(hostsFilePath, MOUNT_TYPE_RO);
		}
		return null;
	}

	/**
	 * Returns a list of hosts matching the constraint parameter.
	 */
	public List<Host> filterHosts(CharSequence constraint) {
		List<Host> all = getHosts(false);
		List<Host> hosts = new ArrayList<>();

		for (Host host : all) {
			if (host.isValid()) {
				if (host.getIp().contains(constraint)
						|| host.getHostName().contains(constraint)
						|| (host.getComment() != null && host.getComment()
								.contains(constraint))) {
					hosts.add(host);
				}
			}
		}
		return hosts;
	}

	/**
	 * Creates a temporary hosts file in
	 * {@code /data/data/project_package/files/hosts}.
	 * <p>
	 * <b>Must be in an async call.</b>
	 * </p>
	 * 
	 * @param appContext
	 *            application context
	 * @return {@code true} if the temp file was created, or {@code false}
	 *         otherwise
	 */
	public String createTempHostsFile(Context appContext) {
		OutputStreamWriter writer = null;
		long time = new Date().getTime();
		try {
			FileOutputStream out = appContext.openFileOutput(HOSTS_FILE_NAME + time,
					Context.MODE_PRIVATE);
			writer = new OutputStreamWriter(out);

			for (Host host : getHosts(false)) {
				writer.append(host.toString()).append(LINE_SEPARATOR);
			}
			writer.flush();
		} catch (IOException e) {
			L.e(e, "Error creating temporary hosts file");
			return null;
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					L.e(e, "Error while closing writer");
				}
			}
		}
		return HOSTS_FILE_NAME + time;
	}

	/**
	 * Executes a single argument root command.
	 * <p>
	 * <b>Must be in an async call.</b>
	 * </p>
	 * 
	 * @param command
	 *            a command, ie {@code "rm -f"}, {@code "chmod 644"}...
	 * @param uniqueArg
	 *            the unique argument for the command, usually the file name
	 */
	private void runRootCommand(String command, String uniqueArg)
			throws IOException, TimeoutException, RootDeniedException {
		Command cmd = new Command(0, false, String.format(Locale.US, "%s %s",
				command, uniqueArg));
		RootShell.getShell(true).add(cmd);
	}
}
