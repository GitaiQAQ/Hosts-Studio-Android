package me.gitai.hosts.utils;

import android.content.Context;

import com.stericson.RootShell.RootShell;
import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import me.gitai.hosts.Constant;
import me.gitai.hosts.HostsApp;
import me.gitai.hosts.R;
import me.gitai.hosts.entities.Host;
import me.gitai.library.util.CryptoUtils;
import me.gitai.library.util.L;
import me.gitai.library.util.StringUtils;

/**
 * Copyed by gitai on 15-11-5 from hosts-editor-android@Nilhcem.
 */
public class HostsManager {

	private Context mContext;

	//private static final String UTF_8 = "UTF-8";
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

    public static final int SUCCESS = 0;

    public static final int ERROR_CREATE_TEMPORARY_HOSTS_FILE = 1;
    public static final int ERROR_CLOSE_WRITER = 2;
    public static final int ERROR_CAN_NOT_GET_ROOT_ACCESS = 3;
    public static final int ERROR_CAN_NOT_CREATE_TEMPORARY_HOSTS_FILE = 4;
    public static final int FAILED_RUNNING_ROOT_COMMAND = 5;
    public static final int ERROR_UNKNOW = 6;

    private String mTempHostsFile = "";

    private String mURI = null;
    private String mURIHash = null;

	private boolean preBuild = false;

	private boolean addto = false;

	// Do not access this field directly even in the same class, use getHosts() instead.
	private List<Host> mHosts;

    public HostsManager(Context context){
        this.mContext = context;
        setURI("default");
    }

    /**
     * Creates a new HostsManager instance.
     */
	public HostsManager(Context context, String uri){
		this.mContext = context;
        setURI(uri);
	}

    public String getURIHash() {
        return (mURIHash != null)?mURIHash:CryptoUtils.HASH.md5(getURI());
    }

    public void setURIHash(String URIHash) {
        this.mURIHash = URIHash;
    }

    public String getURI() {
        return (mURI != null)?mURI:"default";
    }

    public void setURI(String uri) {
        if(uri.equals("whilelist")) {
            this.mURI = "file://" + Constant.FILE_FOLDER_NAME + "whilelist";
        }else{
            this.mURI = uri;
        }
    }

    public boolean isPreBuild() {
        return preBuild;
    }

    public void setPreBuild(boolean preBuild) {
        this.preBuild = preBuild;
    }

    public boolean isAddto() {
        return addto;
    }

    public void setAddto(boolean addto) {
        this.addto = addto;
    }

    public String getTempHostsFile() {
        return mTempHostsFile;
    }

    public void setTempHostsFile(String tempHostsFile) {
        this.mTempHostsFile = tempHostsFile;
    }

    public synchronized List<Host> getHosts() {
        return (mHosts != null)?mHosts:(mHosts = Collections.synchronizedList(new ArrayList<Host>()));
    }

    /**
     * Gets all host entries from custom hosts file path.
     * <p>
     * <b>Must be in an async call.</b>
     * </p>
     *
     * @param path
     *  the custom hosts file path for parsing.
     *      default: default hosts file.
     *      {@code spec}
     * @param forceRefresh
     *  if we want to force using the hosts file (not the cache)
     * @return a list of host entries
     * @throws IOException
     */
	public synchronized List<Host> getHosts(String path, boolean forceRefresh) {
        setURI(path);
        return getHosts(forceRefresh);
	}

    /**
     * Gets all host entries from default hosts file.
     * <p>
     * <b>Must be in an async call.</b>
     * </p>
     *
     * @param forceRefresh
     *            if we want to force using the hosts file (not the cache)
     * @return a list of host entries
     */
	public synchronized List<Host> getHosts(boolean forceRefresh) {
		if (mHosts == null || forceRefresh || isAddto()) {
			InputStream in = null;
			try {
                if(getURI().equals("location")){
                    getHosts(new File(HOSTS_FILE_PATH));
                }else{
                    if(getURI().equals("default")){
                        in = mContext.getResources().openRawResource(R.raw.hosts);
                    }else{
                        in = new URL(getURI()).openStream();
                    }
                    getHosts(in);
                }
			} catch (IOException e) {
				L.e(e, "I/O error while opening hosts file");
			} finally {
				if (in != null) {
					IOUtils.closeQuietly(in);
				}
			}
		}
		return mHosts;
	}

    /**
     * Gets all host entries from custom hosts file.
     * <p>
     * <b>Must be in an async call.</b>
     * </p>
     *
     * @param hostsPath
     *  the custom hosts file
     * @return a list of host entries
     * @throws IOException
     */
	public synchronized List<Host> getHosts(File hostsPath) {
		try{
            getHosts(new FileReader(hostsPath));
        }catch (Exception ex){
            L.e(ex, "I/O error while opening hosts file");
        }
		return mHosts;
	}

    /**
     * Gets all host entries from input stream.
     * <p>
     * <b>Must be in an async call.</b>
     * </p>
     *
     * @param inputStream
     *  the input stream from which to read characters.
     * @return a list of host entries
     * @throws IOException
     */
	private synchronized List<Host> getHosts(InputStream inputStream) throws IOException {
		return getHosts(new BufferedReader(new InputStreamReader(inputStream)));
	}

    /**
     * Gets all host entries.
     * <p>
     * <b>Must be in an async call.</b>
     * </p>
     *
     * @param fileReader
     *  the {@code Reader} the buffer reads from
     * @return a list of host entries
     * @throws IOException
     */
	private synchronized List<Host> getHosts(FileReader fileReader) throws IOException {
		return getHosts(new BufferedReader(fileReader));
	}

	/**
	 * Gets all host entries.
	 * <p>
	 * <b>Must be in an async call.</b>
	 * </p>
	 *
	 * @param bufferedReader
	 * @return a list of host entries
	 * @throws IOException
	 */
	private synchronized List<Host> getHosts(BufferedReader bufferedReader) throws IOException {
        if (!isAddto() || mHosts == null)
            mHosts = Collections.synchronizedList(new ArrayList<Host>());
		String line = bufferedReader.readLine();
		while (line != null){
			Host host = Host.fromString(line, isPreBuild());
			if (host != null && host.isValid()) {
				mHosts.add(host);
			}
			line = bufferedReader.readLine();
		}
		bufferedReader.close();
        createTempHostsFile(false);
        setAddto(false);
		return mHosts;
	}

    /**
     * Saves new hosts file and creates a backup of previous file.
     * if this class created by {@code HostsManager(Context)},
     * you can save without the argument mContext
     * <p>
     * <b>Must be in an async call.</b>
     * </p>
     *
     * @return {@link null} if everything was working as expected, or
     *          {@link String} otherwise
     */
	public synchronized int saveToSysHosts() {
		if (!RootTools.isAccessGiven()) {
			L.e("Can't get root access");
            return ERROR_CAN_NOT_GET_ROOT_ACCESS;
		}

		// Step 1: Create temporary hosts file in
        // /data/data/project_package/files/hosts
		int resultCode = createTempHostsFile(true);
        if (resultCode != SUCCESS) return resultCode;
		if (mTempHostsFile == null) {
            L.e("Can't create temporary hosts file");
            return ERROR_CAN_NOT_CREATE_TEMPORARY_HOSTS_FILE;
		}

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

        resultCode = ERROR_UNKNOW;
		try {
			// Step 3: Create backup of current hosts file (if any)
            // create backup when loading
            RootTools.remount(hostsFilePath, MOUNT_TYPE_RW);
			/*
			runRootCommand(COMMAND_RM, backupFile);
			RootTools.copyFile(hostsFilePath, backupFile, false, true);
			*/

			// Step 4: Replace hosts file with generated file
			runRootCommand(COMMAND_RM, hostsFilePath);
			RootTools.copyFile(mTempHostsFile, hostsFilePath, false, true);

			// Step 5: Give proper rights
			runRootCommand(COMMAND_CHATTR, hostsFilePath);
			runRootCommand(COMMAND_CHOWN, hostsFilePath);
			runRootCommand(COMMAND_CHMOD_644, hostsFilePath);

			// Step 6: Delete local file
			// mContext.deleteFile(HOSTS_FILE_NAME);
            resultCode = SUCCESS;
		} catch (Exception e) {
			L.e(e, "Failed running root command");
            resultCode = FAILED_RUNNING_ROOT_COMMAND;
		} finally {
			RootTools.remount(hostsFilePath, MOUNT_TYPE_RO);
		}
        return resultCode;
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
     * @return {@link null} if everything was working as expected, or
     *          {@link String} otherwise
	 */
	public synchronized int createTempHostsFile(boolean whilelist) {
        String tmpFn = Constant.FILE_FOLDER_NAME + getURIHash() + File.separator + new Date().getTime();
        int resultCode = saveHosts(tmpFn, whilelist);
        if(resultCode != SUCCESS) return resultCode;
		File[] files = new File(tmpFn).getParentFile().listFiles();
		try {
			if (files.length > 1 && getFileMD5(files[files.length - 1]).equals(getFileMD5(files[files.length - 2]))){
                files[files.length - 2].delete();
            }
		} catch (IOException e) {
			e.printStackTrace();
		}
		mTempHostsFile = tmpFn;
		return SUCCESS;
	}

	public static String getFileMD5(File file) throws IOException {
		FileInputStream fs = new FileInputStream(file);
		FileChannel fc = fs.getChannel();
		MappedByteBuffer byteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
		MessageDigest messageDigest = CryptoUtils.HASH.getDigest("MD5");
		messageDigest.update(byteBuffer);
		return CryptoUtils.getString(messageDigest.digest());
	}

    public synchronized int saveHosts(String tmpFn) {
        return saveHosts(tmpFn, true);
    }

    public synchronized int saveHosts(String tmpFn, boolean whilelist) {
        OutputStreamWriter writer = null;
        try {
            FileOutputStream out = new FileOutputStream(creatFileIfNotExists(new File(tmpFn)));
            writer = new OutputStreamWriter(out);

            if (whilelist){
                Set<String> hostSet = new HashSet<>();
                for (Host host : HostsApp.getWhilelist()){
                    if (!hostSet.contains(host.getHostName())){
                        for (Host host1 : getHosts(false)) {
                            if(host.getHostName().equals(host1.getHostName())){
                                if (StringUtils.isEmpty(host.getIp()) || host.isCommented()){
                                    host1.toggleComment();
                                }else{
                                    host1.setIp(host.getIp());
                                }

                            }
                            writer.append(host1.toString()).append(LINE_SEPARATOR);
                        }
                        hostSet.add(host.getHostName());
                    }
                }
            }else{
                for (Host host : getHosts(false)) {
                    writer.append(host.toString()).append(LINE_SEPARATOR);
                }
            }

            writer.flush();
        } catch (IOException e) {
            L.e(e, "Error creating temporary hosts file");
            return ERROR_CREATE_TEMPORARY_HOSTS_FILE;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    L.e(e, "Error while closing writer");
                    return ERROR_CLOSE_WRITER;
                }
            }
        }
        return SUCCESS;
    }

    /**
     * @return null if IOException
     */
    private static File creatFileIfNotExists(File file) throws IOException {
        if (!file.exists()) {
            new File(file.getParent()).mkdirs();
            file.createNewFile();
        }
        return file;
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
