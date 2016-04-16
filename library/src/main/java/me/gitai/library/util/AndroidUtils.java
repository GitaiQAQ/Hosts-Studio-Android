package me.gitai.library.util;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Configuration;
import android.database.Cursor;
import android.hardware.Sensor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.os.StatFs;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import me.gitai.library.util.io.StringBuilderWriter;

/**
 * User: mcxiaoke
 * Date: 13-5-3
 * Time: 上午10:06
 */
public final class AndroidUtils {
    public static final String FILENAME_NOMEDIA = ".nomedia";

    public static final int HEAP_SIZE_LARGE = 48 * 1024 * 1024;
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("[\\w%+,./=_-]+");

    private AndroidUtils() {
    }

    /**
     * Check if a filename is "safe" (no metacharacters or spaces).
     *
     * @param file The file to check
     */
    public static boolean isFilenameSafe(File file) {
        // Note, we check whether it matches what's known to be safe,
        // rather than what's known to be unsafe.  Non-ASCII, control
        // characters, etc. are all unsafe by default.
        return SAFE_FILENAME_PATTERN.matcher(file.getPath()).matches();
    }


    public static File getCacheDir(Context context) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File cacheDir = context.getExternalCacheDir();
            File noMedia = new File(cacheDir, FILENAME_NOMEDIA);
            if (!noMedia.exists()) {
                try {
                    noMedia.createNewFile();
                } catch (IOException e) {
                    L.e(e);
                }
            }
            return cacheDir;
        } else {
            return context.getCacheDir();
        }
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author paulburke
     */
    @SuppressLint("NewApi")
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= VERSION_CODES.KITKAT;

        L.d("getRealPath() uri=" + uri + " isKitKat=" + isKitKat);

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if (ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return uri.getPath();
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private static String getDataColumn(Context context, Uri uri, String selection,
                                        String[] selectionArgs) {

        Cursor cursor = null;
        final String column = MediaStore.MediaColumns.DATA;
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    public static boolean isLargeHeap() {
        return Runtime.getRuntime().maxMemory() > HEAP_SIZE_LARGE;
    }


    public static boolean noSdcard() {
        return !Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /**
     * check if free size of SDCARD and CACHE dir is OK
     *
     * @param needSize how much space should release at least
     * @return true if has enough space
     */
    public static boolean noFreeSpace(long needSize) {
        long freeSpace = getFreeSpace();
        return freeSpace < needSize * 3;
    }

    @SuppressWarnings("deprecation")
    public static long getFreeSpace() {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory()
                .getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return availableBlocks * blockSize;
    }

    public static void hideSoftKeyboard(Context context, EditText editText) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    public static void showSoftKeyboard(Context context, EditText editText) {
        if (editText.requestFocus()) {
            InputMethodManager imm = (InputMethodManager)
                    context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    public static void toggleSoftInput(Context context, View view) {
        if (view.requestFocus()) {
            InputMethodManager imm = (InputMethodManager)
                    context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(0, 0);
        }
    }
    /**
     * Execute an {@link AsyncTask} on a thread pool.
     *
     * @param task Task to add.
     * @param args Optional arguments to pass to {@link AsyncTask#execute(Object[])}.
     * @param <T>  Task argument type.
     */
    @SuppressWarnings("unchecked")
    @TargetApi(VERSION_CODES.HONEYCOMB)
    public static <T> void execute(AsyncTask<T, ?, ?> task, T... args) {
        if (Build.VERSION.SDK_INT < VERSION_CODES.HONEYCOMB) {
            task.execute(args);
        } else {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, args);
        }
    }

    public static boolean hasCamera(Context context) {
        PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)
                || pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
    }

    public static void mediaScan(Context context, Uri uri) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(uri);
        context.sendBroadcast(intent);
    }

    // another media scan way
    public static void addToMediaStore(Context context, File file) {
        String[] path = new String[]{file.getPath()};
        MediaScannerConnection.scanFile(context, path, null, null);
    }

    public static boolean isMediaMounted() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    public static void setFullScreen(final Activity activity,
                                     final boolean fullscreen) {
        if (fullscreen) {
            activity.getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            activity.getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        } else {
            activity.getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            activity.getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    public static void setPortraitOrientation(final Activity activity,
                                              final boolean portrait) {
        if (portrait) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    public static void lockScreenOrientation(final Activity context, final boolean portrait) {
        if (portrait) {
            context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }
    }

    public static void unlockScreenOrientation(final Activity context) {
        context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    public static boolean hasIceCreamSandwich() {
        return Build.VERSION.SDK_INT >= VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    public static boolean isPreIceCreamSandwich() {
        return Build.VERSION.SDK_INT < VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    public static boolean hasJellyBean() {
        return Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN;
    }

    public static boolean hasKitkat() {
        return Build.VERSION.SDK_INT >= VERSION_CODES.KITKAT;
    }

    public static boolean isPreLollipop() {
        return Build.VERSION.SDK_INT < VERSION_CODES.LOLLIPOP;
    }

    public static boolean hasLollipop() {
        return Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP;
    }

    @SuppressWarnings({"ResourceType", "unchecked"})
    public <T> T getSystemService(final Context context, final String name) {
        return (T) context.getSystemService(name);
    }

    @TargetApi(11)
    public static void setStrictMode(boolean enable) {
        if (!enable) {
            return;
        }
        StrictMode.ThreadPolicy.Builder threadPolicyBuilder =
                new StrictMode.ThreadPolicy.Builder()
                        .detectAll()
                        .penaltyLog();
        StrictMode.VmPolicy.Builder vmPolicyBuilder =
                new StrictMode.VmPolicy.Builder()
                        .detectAll()
                        .penaltyLog();
        StrictMode.setThreadPolicy(threadPolicyBuilder.build());
        StrictMode.setVmPolicy(vmPolicyBuilder.build());
    }


    /**
     * 重启一个Activity
     *
     * @param activity Activity
     */
    public static void restartActivity(final Activity activity) {
        Intent intent = activity.getIntent();
        activity.overridePendingTransition(0, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.finish();
        activity.overridePendingTransition(0, 0);
        activity.startActivity(intent);
    }

    public static Intent getBatteryStatus(Context context) {
        Context appContext = context.getApplicationContext();
        return appContext.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    public static float getBatteryLevel(Intent batteryIntent) {
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        return level / (float) scale;
    }

    public static String getBatteryInfo(Intent batteryIntent) {
        int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
        int chargePlug = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
        boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level / (float) scale;
        return "Battery Info: isCharging=" + isCharging
                + " usbCharge=" + usbCharge + " acCharge=" + acCharge
                + " batteryPct=" + batteryPct;
    }


    @SuppressLint("PackageManagerGetSignatures")
    private static Signature getPackageSignature(Context context) {
        final PackageManager pm = context.getPackageManager();
        PackageInfo info = null;
        try {
            info = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
        } catch (Exception ignored) {
            L.e(ignored);
        }

        Signature signature = null;
        if (info != null) {
            Signature[] signatures = info.signatures;
            if (signatures != null && signatures.length > 0) {
                signature = signatures[0];
            }
        }

        L.d("getSignature() " + signature);
        return signature;
    }

    public static String getSignature(Context context) {
        final Signature signature = getPackageSignature(context);
        if (signature != null) {
            try {
                return CryptoUtils.HASH.sha1(signature.toByteArray());
            } catch (Exception e) {
                L.e(e);
            }
        }
        return "";
    }

    private static final Uri GSF_URI = Uri.parse("content://com.google.android.gservices");

    private static final String GSF_ID_KEY = "android_id";


    public static String getGSFId(Context ctx){
        final String[] params = { GSF_ID_KEY };
        final Cursor c = ctx.getContentResolver().query(GSF_URI,null,null,params,null);

        try{
            if (!c.moveToFirst() || c.getColumnCount() < 2){
                return null;
            }
            return Long.toHexString(Long.parseLong(c.getString(1)));
        }catch (NumberFormatException e){
            L.e(e);
        }finally {
            if (c!=null){
                c.close();
            }
        }
        return null;
    }

    public static String getBuildSerialId(){
        String deviceId = null;
        if (Build.VERSION.SDK_INT > VERSION_CODES.GINGERBREAD){
            deviceId = Build.SERIAL;
        }
        return deviceId;
    }

    public static String getAndroidId(Context ctx){
        String androidId = Settings.Secure.getString(ctx.getContentResolver(),Settings.Secure.ANDROID_ID);
        if (androidId == null || androidId.equalsIgnoreCase("android_id")||androidId.equalsIgnoreCase("9774d56d682e549c")){
            return null;
        }
        return androidId;
    }

    public static String getIMEI(Context ctx){
        String IMEI = null;
        try{
            final TelephonyManager tm = (TelephonyManager)ctx.getSystemService(Context.TELEPHONY_SERVICE);
            IMEI = tm.getDeviceId();
        }catch (Exception e){
            L.e(e);
        }
        return IMEI;
    }
    
    public static String getDeviceId(Context ctx){
        String deviceId = null;
        try{
            final TelephonyManager tm = (TelephonyManager)ctx.getSystemService(Context.TELEPHONY_SERVICE);
            String tmDevice = "" + tm.getDeviceId();
            String tmSerial = "" + tm.getSimSerialNumber();
            String androidId = "" + Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);

            UUID deviceUUID = new UUID(androidId.hashCode(),((long)tmDevice.hashCode() << 32)|tmSerial.hashCode());
            deviceId = deviceUUID.toString();
        }catch (Exception e){
            L.e(e);
            try{
                deviceId = getSerialDeviceId(ctx);
            }catch (Exception e1){
                L.e(e1);
                throw new RuntimeException("FATAL!!! - This device doesn`t hava any Unique Serial Number",e1);
            }
        }
        return deviceId;
    }

    public static String getPid(){
        StringBuilder pid = new StringBuilder(Build.MANUFACTURER)
                .append(" ")
                .append(Build.MODEL)
                .append(" - ")
                .append(Build.VERSION.RELEASE);

        return pid.toString();
    }

    public static String getSerialDeviceId(Context ctx){
        String deviceId = null;
        if (Build.VERSION.SDK_INT > VERSION_CODES.GINGERBREAD){
            deviceId = Build.SERIAL;
        }
        if (deviceId == null || deviceId.equalsIgnoreCase("unknown")){
            deviceId = getAndroidId(ctx);
            if (deviceId == null){
                throw new RuntimeException("FATAL!!! - This device doesn`t hava any Unique Serial Number");
            }
        }
        return deviceId;
    }

    public static String getTagusSerialDeviceId(Context ctx){
        String deviceId = null;
        try{
            deviceId = getIMEI(ctx);
            if (deviceId == null){
                deviceId = getSerialDeviceId(ctx);
            }
        }catch (Exception ex){
            deviceId = getDeviceId(ctx);
            L.e(ex);
        }
        return deviceId;
    }

    public static String getSignatureInfo(Context context) {
        final Signature signature = getPackageSignature(context);
        if (signature == null) {
            return "";
        }
        final StringBuilder builder = new StringBuilder();
        try {
            final byte[] signatureBytes = signature.toByteArray();
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            final InputStream is = new ByteArrayInputStream(signatureBytes);
            X509Certificate cert = (X509Certificate) certFactory.generateCertificate(is);
            final String chars = signature.toCharsString();
            final String hex = CryptoUtils.HEX.encodeHex(signatureBytes, false);
            final String md5 = CryptoUtils.HASH.md5(signatureBytes);
            final String sha1 = CryptoUtils.HASH.sha1(signatureBytes);
            builder.append("SignName:").append(cert.getSigAlgName()).append("\n");
            builder.append("Chars:").append(chars).append("\n");
            builder.append("Hex:").append(hex).append("\n");
            builder.append("MD5:").append(md5).append("\n");
            builder.append("SHA1:").append(sha1).append("\n");
            builder.append("SignNumber:").append(cert.getSerialNumber()).append("\n");
            builder.append("SubjectDN:").append(cert.getSubjectDN().getName()).append("\n");
            builder.append("IssuerDN:").append(cert.getIssuerDN().getName()).append("\n");
        } catch (Exception e) {
            L.e(e);
        }

        final String text = builder.toString();

        L.d(text);

        return text;
    }

    private static String UNKNOWN = "";


    /**
     * The user-visible SDK version of the framework;
     * its possible values are defined in Build.VERSION_CODES.
     * @param sdk_int "SDK_INT"
     */
    public static String getSDKIntStr(int sdk_int)
    {
        switch (sdk_int)
        {
            case Build.VERSION_CODES.BASE://1
                return "BASE";//Android 1.0
            case Build.VERSION_CODES.BASE_1_1://2
                return "BASE_1_1";//Android 1.1
            case Build.VERSION_CODES.CUPCAKE://3
                return "CUPCAKE";//Android 1.5
            case Build.VERSION_CODES.DONUT://4
                return "DONUT";//Android 1.6
            case Build.VERSION_CODES.ECLAIR://5
                return "ECLAIR";//Android 2.0
            case Build.VERSION_CODES.ECLAIR_0_1://6
                return "ECLAIR_0_1";//Android 2.0.1
            case Build.VERSION_CODES.ECLAIR_MR1://7
                return "ECLAIR_MR1";//Android 2.1.x
            case Build.VERSION_CODES.FROYO://8
                return "FROYO";//Android 2.2.x
            case Build.VERSION_CODES.GINGERBREAD://9
                return "GINGERBREAD";//Android 2.3 Android 2.3.1 Android 2.3.2
            case Build.VERSION_CODES.GINGERBREAD_MR1://10
                return "GINGERBREAD_MR1";//Android 2.3.3 Android 2.3.4
            case Build.VERSION_CODES.HONEYCOMB://11
                return "HONEYCOMB";//Android 3.0.x
            case Build.VERSION_CODES.HONEYCOMB_MR1://12
                return "HONEYCOMB_MR1";//Android 3.1.x
            case Build.VERSION_CODES.HONEYCOMB_MR2://13
                return "HONEYCOMB_MR2";//Android 3.2
            case Build.VERSION_CODES.ICE_CREAM_SANDWICH://14
                return "ICE_CREAM_SANDWICH";//Android 4.0 Android 4.0.1 Android 4.0.2
            case Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1://15
                return "ICE_CREAM_SANDWICH_MR1";//Android 4.0.3 Android 4.0.4
            case Build.VERSION_CODES.JELLY_BEAN://16
                return "JELLY_BEAN";//Android 4.1 Android 4.1.1
            case Build.VERSION_CODES.JELLY_BEAN_MR1://17
                return "JELLY_BEAN_MR1";//Android 4.2 Android 4.2.2
            case Build.VERSION_CODES.JELLY_BEAN_MR2://18
                return "JELLY_BEAN_MR2";//Android 4.3
            case Build.VERSION_CODES.KITKAT://19
                return "KITKAT";//Android 4.4
            case Build.VERSION_CODES.KITKAT_WATCH://20
                return "KITKAT_WATCH";//Android 4.4W
            case Build.VERSION_CODES.LOLLIPOP://21
                return "LOLLIPOP";//Android 5.0
            case Build.VERSION_CODES.LOLLIPOP_MR1://22
                return "LOLLIPOP_MR1";
            case Build.VERSION_CODES.M://23
                return "M";
            //Magic version number for a current development build,
            //which has not yet turned into an official release.
            case Build.VERSION_CODES.CUR_DEVELOPMENT://1000
                return "CUR_DEVELOPMENT";
            default:
                return UNKNOWN;
        }
    }

    /**
     * @param sdk_int "SDK_INT"
     * @return The date Google release the given Android version.
     */
    public static String getSDKIntDateStr(int sdk_int)
    {
        switch (sdk_int)
        {
            case Build.VERSION_CODES.BASE://0
                return "2008-10";//Android 1.0
            case Build.VERSION_CODES.BASE_1_1://1
                return "2009-02";//Android 1.1
            case Build.VERSION_CODES.CUPCAKE://3
                return "2009-05";//Android 1.5
            case Build.VERSION_CODES.DONUT://4
                return "2009-09";//Android 1.6
            case Build.VERSION_CODES.ECLAIR://5
                return "2009-11";//Android 2.0
            case Build.VERSION_CODES.ECLAIR_0_1://6
                return "2009-12";//Android 2.0.1
            case Build.VERSION_CODES.ECLAIR_MR1://7
                return "2010-01";//Android 2.1.x
            case Build.VERSION_CODES.FROYO://8
                return "2010-06";//Android 2.2.x
            case Build.VERSION_CODES.GINGERBREAD://9
                return "2010-11";//Android 2.3 Android 2.3.1 Android 2.3.2
            case Build.VERSION_CODES.GINGERBREAD_MR1://10
                return "2011-02";//Android 2.3.3 Android 2.3.4
            case Build.VERSION_CODES.HONEYCOMB://11
                return "2011-02";//Android 3.0.x
            case Build.VERSION_CODES.HONEYCOMB_MR1://12
                return "2011-05";//Android 3.1.x
            case Build.VERSION_CODES.HONEYCOMB_MR2://13
                return "2011-06";//Android 3.2
            case Build.VERSION_CODES.ICE_CREAM_SANDWICH://14
                return "2011-10";//Android 4.0 Android 4.0.1 Android 4.0.2
            case Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1://15
                return "2011-12";//Android 4.0.3 Android 4.0.4
            case Build.VERSION_CODES.JELLY_BEAN://16
                return "2012-06";//Android 4.1 Android 4.1.1
            case Build.VERSION_CODES.JELLY_BEAN_MR1://17
                return "2012-11";//Android 4.2 Android 4.2.2
            case Build.VERSION_CODES.JELLY_BEAN_MR2://18
                return "2013-07";//Android 4.3
            case Build.VERSION_CODES.KITKAT://19
                return "2013-10";//Android 4.4
            case Build.VERSION_CODES.KITKAT_WATCH://20
                return UNKNOWN;//Android 4.4W
            case Build.VERSION_CODES.LOLLIPOP://21
                return UNKNOWN;//Android 5.0
            case Build.VERSION_CODES.LOLLIPOP_MR1://22
                return UNKNOWN;
            case Build.VERSION_CODES.M://23
                return UNKNOWN;
            case Build.VERSION_CODES.CUR_DEVELOPMENT://1000
                return UNKNOWN;
            default:
                return UNKNOWN;
        }
    }

    /**
     * The screen density expressed as dots-per-inch.
     * @param density_dpi "densityDpi"
     */
    public static String getDensityDPIStr(int density_dpi)
    {
        switch (density_dpi)
        {
            case DisplayMetrics.DENSITY_LOW://120
                return "DENSITY_LOW";
            case DisplayMetrics.DENSITY_MEDIUM://160
                return "DENSITY_MEDIUM/DENSITY_DEFAULT";
            //case DisplayMetrics.DENSITY_DEFAULT://160
            //    return "DENSITY_DEFAULT";
            case DisplayMetrics.DENSITY_TV://213
                return "DENSITY_TV";
            case DisplayMetrics.DENSITY_HIGH://240
                return "DENSITY_HIGH";
            case DisplayMetrics.DENSITY_280://280
                return "DENSITY_280";
            case DisplayMetrics.DENSITY_XHIGH://320
                return "DENSITY_XHIGH";
            case DisplayMetrics.DENSITY_360://360
                return "DENSITY_360";
            case DisplayMetrics.DENSITY_400://400
                return "DENSITY_400";
            case DisplayMetrics.DENSITY_420://420
                return "DENSITY_420";
            case DisplayMetrics.DENSITY_XXHIGH://480
                return "DENSITY_XXHDPI";
            case DisplayMetrics.DENSITY_560://560
                return "DENSITY_560";
            case DisplayMetrics.DENSITY_XXXHIGH://640
                return "DENSITY_XXXHDPI";
            default:
                return UNKNOWN;
        }
    }

    /**
     * Overall orientation of the screen.
     * @param orientation "orientation"
     */
    @SuppressWarnings("deprecation")
    public static String getOrientationStr(int orientation)
    {
        switch (orientation)
        {
            case Configuration.ORIENTATION_UNDEFINED://0
                return "ORIENTATION_UNDEFINED";
            case Configuration.ORIENTATION_PORTRAIT://1
                return "ORIENTATION_PORTRAIT";
            case Configuration.ORIENTATION_LANDSCAPE://2
                return "ORIENTATION_LANDSCAPE";
            case Configuration.ORIENTATION_SQUARE://3
                return "ORIENTATION_SQUARE";
            default:
                return UNKNOWN;
        }
    }

    @SuppressWarnings("deprecation")
    public static String getSensorTypeStr(int type)
    {
        switch (type)
        {
            case Sensor.TYPE_ALL://-1
                return "TYPE_ALL";
            //Accelerometer sensor type
            case Sensor.TYPE_ACCELEROMETER://1
                return "TYPE_ACCELEROMETER";
            //Magnetic field sensor type
            case Sensor.TYPE_MAGNETIC_FIELD://2
                return "TYPE_MAGNETIC_FIELD";
            //Orientation sensor type
            case Sensor.TYPE_ORIENTATION://3
                return "TYPE_ORIENTATION";
            //Gyroscope sensor type
            case Sensor.TYPE_GYROSCOPE://4
                return "TYPE_GYROSCOPE";
            //Light sensor type
            case Sensor.TYPE_LIGHT://5
                return "TYPE_LIGHT";
            //Pressure sensor type
            case Sensor.TYPE_PRESSURE://6
                return "TYPE_PRESSURE";
            //Temperature sensor type
            case Sensor.TYPE_TEMPERATURE://7
                return "TYPE_TEMPERATURE";
            //Proximity sensor type
            case Sensor.TYPE_PROXIMITY://8
                return "TYPE_PROXIMITY";
            //Gravity sensor type
            case Sensor.TYPE_GRAVITY://9
                return "TYPE_GRAVITY";
            //Linear acceleration sensor type
            case Sensor.TYPE_LINEAR_ACCELERATION://10
                return "TYPE_LINEAR_ACCELERATION";
            //Rotation vector sensor type
            case Sensor.TYPE_ROTATION_VECTOR://11
                return "TYPE_ROTATION_VECTOR";
            //Relative humidity sensor type
            case Sensor.TYPE_RELATIVE_HUMIDITY://12
                return "TYPE_RELATIVE_HUMIDITY";
            //Ambient temperature sensor type
            case Sensor.TYPE_AMBIENT_TEMPERATURE://13
                return "TYPE_AMBIENT_TEMPERATURE";
            //Uncalibrated magnetic field sensor type
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED://14
                return "TYPE_MAGNETIC_FIELD_UNCALIBRATED";
            //Uncalibrated rotation vector sensor type
            case Sensor.TYPE_GAME_ROTATION_VECTOR://15
                return "TYPE_GAME_ROTATION_VECTOR";
            //Uncalibreted gyroscope sensor type
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED://16
                return "TYPE_GYROSCOPE_UNCALIBRATED";
            //Significant motion trigger sensor
            case Sensor.TYPE_SIGNIFICANT_MOTION://17
                return "TYPE_SIGNIFICANT_MOTION";
            //Step detector sensor
            case Sensor.TYPE_STEP_DETECTOR://18
                return "TYPE_STEP_DETECTOR";
            //Step counter sensor
            case Sensor.TYPE_STEP_COUNTER://19
                return "TYPE_STEP_COUNTER";
            //Geo-magnetic rotation vector
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR://20
                return "TYPE_GEOMAGNETIC_ROTATION_VECTOR";
            //Heart rate monitor
            case Sensor.TYPE_HEART_RATE://21
                return "TYPE_HEART_RATE";
            default:
                return UNKNOWN;
        }
    }

    /**
     * Returns the rotation of the screen from its "natural" orientation.
     * Notice: ANTICLOCKWISE
     * @param rotation "getRotation()"
     */
    public static String getRotationStr(int rotation)
    {
        switch (rotation)
        {
            //Natural orientation
            case Surface.ROTATION_0://0
                return "ROTATION_0";
            case Surface.ROTATION_90://1
                return "ROTATION_90";
            case Surface.ROTATION_180://2
                return "ROTATION_180";
            case Surface.ROTATION_270://3
                return "ROTATION_270";
            default:
                return UNKNOWN;
        }
    }

    /**
     * Returns the MCC+MNC (mobile country code + mobile network code) of the provider of the SIM.
     * 5 or 6 decimal digits.
     * Notice: Only SIM operators in China are supported currently.
     * @param sim_operator "getSimOperator()"
     */
    public static String getSimOperator(String sim_operator)
    {
        if (sim_operator == null || sim_operator.length() < 5)
        {
            return UNKNOWN;
        }

        int mcc = Integer.parseInt(sim_operator.substring(0, 3));
        int mnc = Integer.parseInt(sim_operator.substring(3));

        switch (mcc)
        {
            case 454:
                switch (mcc)
                {
                    default:
                        return "Hong Kong";
                }
            case 455:
                switch (mcc)
                {
                    default:
                        return "Macao";
                }
            case 460:
                switch (mnc)
                {
                    case 0:
                    case 2:
                    case 7:
                        //00 for TD of China Mobile; 02 for GSM of China Mobile.
                        return "China Mobile";
                    case 1:
                    case 6:
                        //01 for GSM of China Unicom.
                        return "China Unicom";
                    case 3:
                    case 5:
                        //03 for CDMA of China Telecom.
                        return "China Telecom";
                    case 20:
                        return "China Tietong";
                    default:
                        return UNKNOWN;
                }
            case 466:
                switch (mcc)
                {
                    default:
                        return "Taiwan";
                }
            default:
                return UNKNOWN;
        }
    }

    /**
     * Returns the current state of the storage device that provides the given path.
     * @param state "getExternalStorageState()"
     */
    public static String getExternalStorageState(String state)
    {
        if (TextUtils.isEmpty(state))
        {
            return UNKNOWN;
        }

        switch (state)
        {
            case Environment.MEDIA_BAD_REMOVAL://bad_removal
                return "MEDIA_BAD_REMOVAL";
            case Environment.MEDIA_CHECKING://checking
                return "MEDIA_CHECKING";
            case Environment.MEDIA_EJECTING://ejecting
                return "MEDIA_EJECTING";
            case Environment.MEDIA_MOUNTED://mounted
                return "MEDIA_MOUNTED";
            case Environment.MEDIA_MOUNTED_READ_ONLY://mounted_read_only
                return "MEDIA_MOUNTED_READ_ONLY";
            case Environment.MEDIA_NOFS://nofs
                return "MEDIA_NOFS";
            case Environment.MEDIA_REMOVED://removed
                return "MEDIA_REMOVED";
            case Environment.MEDIA_SHARED://shared
                return "MEDIA_SHARED";
            case Environment.MEDIA_UNKNOWN://unknown
                return "MEDIA_UNKNOWN";
            case Environment.MEDIA_UNMOUNTABLE://unmountable
                return "MEDIA_UNMOUNTABLE";
            case Environment.MEDIA_UNMOUNTED://unmounted
                return "MEDIA_UNMOUNTED";
            default:
                return UNKNOWN;
        }
    }

    /**
     * Returns a constant indicating the device phone type.
     * This indicates the type of radio used to transmit voice calls.
     * @param phone_type "getPhoneType()"
     */
    public static String getPhoneTypeStr(int phone_type)
    {
        switch (phone_type)
        {
            case TelephonyManager.PHONE_TYPE_NONE://0
                return "PHONE_TYPE_NONE";
            case TelephonyManager.PHONE_TYPE_GSM://1
                return "PHONE_TYPE_GSM";
            case TelephonyManager.PHONE_TYPE_CDMA://2
                return "PHONE_TYPE_CDMA";
            case TelephonyManager.PHONE_TYPE_SIP://3
                return "PHONE_TYPE_SIP";//API 11
            default:
                return UNKNOWN;
        }
    }

    /**
     * @param phone_type "getPhoneType()"
     * @param id_length Length of device id string.
     * @return The date Google release the given Android version.
     */
    public static String getDeviceIdType(int phone_type, int id_length)
    {
        switch (phone_type)
        {
            case TelephonyManager.PHONE_TYPE_GSM://1
                return "IMEI";
            case TelephonyManager.PHONE_TYPE_CDMA://2
                if (id_length == 8)
                {
                    return "ESN";
                }
                return "MEID";
            default:
                return UNKNOWN;
        }
    }

    /**
     * Returns a constant indicating the state of the default SIM card.
     * @param sim_state "getSimState()"
     */
    public static String getSimStateStr(int sim_state)
    {
        switch (sim_state)
        {
            case TelephonyManager.SIM_STATE_UNKNOWN://0
                return "SIM_STATE_UNKNOWN";
            case TelephonyManager.SIM_STATE_ABSENT://1
                return "SIM_STATE_ABSENT";
            case TelephonyManager.SIM_STATE_PIN_REQUIRED://2
                return "SIM_STATE_PIN_REQUIRED";
            case TelephonyManager.SIM_STATE_PUK_REQUIRED://3
                return "SIM_STATE_PUK_REQUIRED";
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED://4
                return "SIM_STATE_NETWORK_LOCKED";
            case TelephonyManager.SIM_STATE_READY://5
                return "SIM_STATE_READY";
            default:
                return UNKNOWN;
        }
    }

    /**
     * Current user preference for the locale, corresponding to locale resource qualifier.
     * @param locale "locale"
     */
    @TargetApi(9)
    public static String getLocale(Locale locale)
    {
        if (Build.VERSION.SDK_INT >= 9 && locale.equals(Locale.ROOT))//null
        {
            return "ROOT";
        }
        else if (locale.equals(Locale.GERMAN))//de
        {
            return "GERMAN";
        }
        else if (locale.equals(Locale.GERMANY))//de_DE
        {
            return "GERMANY";
        }
        else if (locale.equals(Locale.ENGLISH))//en
        {
            return "ENGLISH";
        }
        else if (locale.equals(Locale.CANADA))//en_CA
        {
            return "CANADA";
        }
        else if (locale.equals(Locale.UK))//en_GB
        {
            return "UK";
        }
        else if (locale.equals(Locale.US))//en_US
        {
            return "US";
        }
        else if (locale.equals(Locale.FRENCH))//fr
        {
            return "FRENCH";
        }
        else if (locale.equals(Locale.CANADA_FRENCH))//fr_CA
        {
            return "CANADA_FRENCH";
        }
        else if (locale.equals(Locale.FRANCE))//fr_FR
        {
            return "FRANCE";
        }
        else if (locale.equals(Locale.ITALIAN))//it
        {
            return "ITALIAN";
        }
        else if (locale.equals(Locale.ITALY))//it_IT
        {
            return "ITALY";
        }
        else if (locale.equals(Locale.JAPANESE))//ja
        {
            return "JAPANESE";
        }
        else if (locale.equals(Locale.JAPAN))//ja_JP
        {
            return "JAPAN";
        }
        else if (locale.equals(Locale.KOREAN))//ko
        {
            return "KOREAN";
        }
        else if (locale.equals(Locale.KOREA))//ko_KR
        {
            return "KOREA";
        }
        else if (locale.equals(Locale.CHINESE))//zh
        {
            return "CHINESE";
        }
        else if (locale.equals(Locale.SIMPLIFIED_CHINESE))//zh_CN
        {
            return "SIMPLIFIED_CHINESE/CHINA/PRC";
        }
        /*else if (locale.equals(Locale.CHINA))//zh_CN
        {
            return "CHINA";
        }*/
        /*else if (locale.equals(Locale.PRC))//zh_CN
        {
            return "PRC";
        }*/
        else if (locale.equals(Locale.TRADITIONAL_CHINESE))//zh_TW
        {
            return "TRADITIONAL_CHINESE/TAIWAN";
        }
        /*else if (locale.equals(Locale.TAIWAN))//zh_TW
        {
            return "TRADITIONAL_TAIWAN";
        }*/
        return UNKNOWN;
    }

    /**
     * A single feature that can be requested by an application.
     * This corresponds to information collected from the AndroidManifest.xml's tag.
     * @param feature "FeatureInfo"
     */
    @SuppressWarnings("deprecation")
    public static String getFeature(String feature)
    {
        if (TextUtils.isEmpty(feature))
        {
            return UNKNOWN;
        }

        switch (feature)
        {
            case PackageManager.FEATURE_APP_WIDGETS://android.software.app_widgets
                return "FEATURE_APP_WIDGETS";
            case PackageManager.FEATURE_AUDIO_LOW_LATENCY://android.hardware.audio.low_latency
                return "FEATURE_AUDIO_LOW_LATENCY";
            case PackageManager.FEATURE_AUDIO_OUTPUT://android.hardware.audio.output
                return "FEATURE_AUDIO_OUTPUT";
            case PackageManager.FEATURE_AUDIO_PRO://android.hardware.audio.pro
                return "FEATURE_AUDIO_PRO";//API 23
            case PackageManager.FEATURE_AUTOMOTIVE://android.hardware.type.automotive
                return "FEATURE_AUTOMOTIVE";//API 23
            case PackageManager.FEATURE_BACKUP://android.software.backup
                return "FEATURE_BACKUP";
            case PackageManager.FEATURE_BLUETOOTH://android.hardware.bluetooth
                return "FEATURE_BLUETOOTH";
            case PackageManager.FEATURE_BLUETOOTH_LE://android.hardware.bluetooth_le
                return "FEATURE_BLUETOOTH_LE";
            case PackageManager.FEATURE_CAMERA://android.hardware.camera
                return "FEATURE_CAMERA";
            case PackageManager.FEATURE_CAMERA_ANY://android.hardware.camera.any
                return "FEATURE_CAMERA_ANY";
            case PackageManager.FEATURE_CAMERA_AUTOFOCUS://android.hardware.camera.autofocus
                return "FEATURE_CAMERA_AUTOFOCUS";
            case PackageManager.FEATURE_CAMERA_CAPABILITY_MANUAL_POST_PROCESSING://android.hardware.camera.capability.manual_post_processing
                return "FEATURE_CAMERA_CAPABILITY_MANUAL_POST_PROCESSING";
            case PackageManager.FEATURE_CAMERA_CAPABILITY_MANUAL_SENSOR://android.hardware.camera.capability.manual_sensor
                return "FEATURE_CAMERA_CAPABILITY_MANUAL_SENSOR";
            case PackageManager.FEATURE_CAMERA_CAPABILITY_RAW://android.hardware.camera.capability.raw
                return "FEATURE_CAMERA_CAPABILITY_RAW";
            case PackageManager.FEATURE_CAMERA_EXTERNAL://android.hardware.camera.external
                return "FEATURE_CAMERA_EXTERNAL";
            case PackageManager.FEATURE_CAMERA_FLASH://android.hardware.camera.flash
                return "FEATURE_CAMERA_FLASH";
            case PackageManager.FEATURE_CAMERA_FRONT://android.hardware.camera.front
                return "FEATURE_CAMERA_FRONT";
            case PackageManager.FEATURE_CAMERA_LEVEL_FULL://android.hardware.camera.level.full
                return "FEATURE_CAMERA_LEVEL_FULL";
            case PackageManager.FEATURE_CONNECTION_SERVICE://android.software.connectionservice
                return "FEATURE_CONNECTION_SERVICE";
            case PackageManager.FEATURE_CONSUMER_IR://android.hardware.consumerir
                return "FEATURE_CONSUMER_IR";
            case PackageManager.FEATURE_DEVICE_ADMIN://android.software.device_admin
                return "FEATURE_DEVICE_ADMIN";
            case PackageManager.FEATURE_FAKETOUCH://android.hardware.faketouch
                return "FEATURE_FAKETOUCH";
            case PackageManager.FEATURE_FAKETOUCH_MULTITOUCH_DISTINCT://android.hardware.faketouch.multitouch.distinct
                return "FEATURE_FAKETOUCH_MULTITOUCH_DISTINCT";
            case PackageManager.FEATURE_FAKETOUCH_MULTITOUCH_JAZZHAND://android.hardware.faketouch.multitouch.jazzhand
                return "FEATURE_FAKETOUCH_MULTITOUCH_JAZZHAND";
            case PackageManager.FEATURE_FINGERPRINT://android.hardware.fingerprint
                return "FEATURE_FINGERPRINT";//API 23
            case PackageManager.FEATURE_GAMEPAD://android.hardware.gamepad
                return "FEATURE_GAMEPAD";
            case PackageManager.FEATURE_HIFI_SENSORS://android.hardware.sensor.hifi_sensors
                return "FEATURE_HIFI_SENSORS";//API 23
            case PackageManager.FEATURE_HOME_SCREEN://android.software.home_screen
                return "FEATURE_HOME_SCREEN";
            case PackageManager.FEATURE_INPUT_METHODS://android.software.input_methods
                return "FEATURE_INPUT_METHODS";
            case PackageManager.FEATURE_LEANBACK://android.software.leanback
                return "FEATURE_LEANBACK";
            case PackageManager.FEATURE_LIVE_TV://android.software.live_tv
                return "FEATURE_LIVE_TV";
            case PackageManager.FEATURE_LIVE_WALLPAPER://android.software.live_wallpaper
                return "FEATURE_LIVE_WALLPAPER";
            case PackageManager.FEATURE_LOCATION://android.hardware.location
                return "FEATURE_LOCATION";
            case PackageManager.FEATURE_LOCATION_GPS://android.hardware.location.gps
                return "FEATURE_LOCATION_GPS";
            case PackageManager.FEATURE_LOCATION_NETWORK://android.hardware.location.network
                return "FEATURE_LOCATION_NETWORK";
            case PackageManager.FEATURE_MANAGED_USERS://android.software.managed_users
                return "FEATURE_MANAGED_USERS";
            case PackageManager.FEATURE_MICROPHONE://android.hardware.microphone
                return "FEATURE_MICROPHONE";
            case PackageManager.FEATURE_MIDI://android.software.midi
                return "FEATURE_MIDI";//API 23
            case PackageManager.FEATURE_NFC://android.hardware.nfc
                return "FEATURE_NFC";
            case PackageManager.FEATURE_NFC_HOST_CARD_EMULATION://android.hardware.nfc.hce
                return "FEATURE_NFC_HOST_CARD_EMULATION";
            case PackageManager.FEATURE_OPENGLES_EXTENSION_PACK://android.hardware.opengles.aep
                return "FEATURE_OPENGLES_EXTENSION_PACK";
            case PackageManager.FEATURE_PRINTING://android.software.print
                return "FEATURE_PRINTING";
            case PackageManager.FEATURE_SCREEN_LANDSCAPE://android.hardware.screen.landscape
                return "FEATURE_SCREEN_LANDSCAPE";
            case PackageManager.FEATURE_SCREEN_PORTRAIT://android.hardware.screen.portrait
                return "FEATURE_SCREEN_PORTRAIT";
            case PackageManager.FEATURE_SECURELY_REMOVES_USERS://android.software.securely_removes_users
                return "FEATURE_SECURELY_REMOVES_USERS";
            case PackageManager.FEATURE_SENSOR_ACCELEROMETER://android.hardware.sensor.accelerometer
                return "FEATURE_SENSOR_ACCELEROMETER";
            case PackageManager.FEATURE_SENSOR_AMBIENT_TEMPERATURE://android.hardware.sensor.ambient_temperature
                return "FEATURE_SENSOR_AMBIENT_TEMPERATURE";
            case PackageManager.FEATURE_SENSOR_BAROMETER://android.hardware.sensor.barometer
                return "FEATURE_SENSOR_BAROMETER";
            case PackageManager.FEATURE_SENSOR_COMPASS://android.hardware.sensor.compass
                return "FEATURE_SENSOR_COMPASS";
            case PackageManager.FEATURE_SENSOR_GYROSCOPE://android.hardware.sensor.gyroscope
                return "FEATURE_SENSOR_GYROSCOPE";
            case PackageManager.FEATURE_SENSOR_HEART_RATE://android.hardware.sensor.heartrate
                return "FEATURE_SENSOR_HEART_RATE";
            case PackageManager.FEATURE_SENSOR_HEART_RATE_ECG://android.hardware.sensor.heartrate.ecg
                return "FEATURE_SENSOR_HEART_RATE_ECG";
            case PackageManager.FEATURE_SENSOR_LIGHT://android.hardware.sensor.light
                return "FEATURE_SENSOR_LIGHT";
            case PackageManager.FEATURE_SENSOR_PROXIMITY://android.hardware.sensor.proximity
                return "FEATURE_SENSOR_PROXIMITY";
            case PackageManager.FEATURE_SENSOR_RELATIVE_HUMIDITY://android.hardware.sensor.relative_humidity
                return "FEATURE_SENSOR_RELATIVE_HUMIDITY";
            case PackageManager.FEATURE_SENSOR_STEP_COUNTER://android.hardware.sensor.stepcounter
                return "FEATURE_SENSOR_STEP_COUNTER";
            case PackageManager.FEATURE_SENSOR_STEP_DETECTOR://android.hardware.sensor.stepdetector
                return "FEATURE_SENSOR_STEP_DETECTOR";
            case PackageManager.FEATURE_SIP://android.software.sip
                return "FEATURE_SIP";
            case PackageManager.FEATURE_SIP_VOIP://android.software.sip.voip
                return "FEATURE_SIP_VOIP";
            case PackageManager.FEATURE_TELEPHONY://android.hardware.telephony
                return "FEATURE_TELEPHONY";
            case PackageManager.FEATURE_TELEPHONY_CDMA://android.hardware.telephony.cdma
                return "FEATURE_TELEPHONY_CDMA";
            case PackageManager.FEATURE_TELEPHONY_GSM://android.hardware.telephony.gsm
                return "FEATURE_TELEPHONY_GSM";
            case PackageManager.FEATURE_TELEVISION://android.hardware.type.television
                return "FEATURE_TELEVISION";
            case PackageManager.FEATURE_TOUCHSCREEN://android.hardware.touchscreen
                return "FEATURE_TOUCHSCREEN";
            case PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH://android.hardware.touchscreen.multitouch
                return "FEATURE_TOUCHSCREEN_MULTITOUCH";
            case PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT://android.hardware.touchscreen.multitouch.distinct
                return "FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT";
            case PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_JAZZHAND://android.hardware.touchscreen.multitouch.jazzhand
                return "FEATURE_TOUCHSCREEN_MULTITOUCH_JAZZHAND";
            case PackageManager.FEATURE_USB_ACCESSORY://android.hardware.usb.accessory
                return "FEATURE_USB_ACCESSORY";
            case PackageManager.FEATURE_USB_HOST://android.hardware.usb.host
                return "FEATURE_USB_HOST";
            case PackageManager.FEATURE_VERIFIED_BOOT://android.software.verified_boot
                return "FEATURE_VERIFIED_BOOT";
            case PackageManager.FEATURE_WATCH://android.hardware.type.watch
                return "FEATURE_WATCH";
            case PackageManager.FEATURE_WEBVIEW://android.software.webview
                return "FEATURE_WEBVIEW";
            case PackageManager.FEATURE_WIFI://android.hardware.wifi
                return "FEATURE_WIFI";
            case PackageManager.FEATURE_WIFI_DIRECT://android.hardware.wifi.direct
                return "FEATURE_WIFI_DIRECT";
            default:
                return UNKNOWN;
        }
    }

    /**
     * @param width Physical width of screen
     * @param height Physical height of screen
     * @return The resolution format, like QVGA.
     */
    public static String getResolutionFormat(int width, int height)
    {
        //Total pixels
        int pixels = width * height;
        //Pixels of the short side
        int min = width <= height ? width : height;
        switch (pixels)
        {
            /*
             * Video Graphics Array
             */
            case 240 * 320://76800
                return "QVGA";//Quarter VGA
            case 240 * 400://96000
                return "WQVGA";//Wide QVGA, like Samsung I5800
            case 320 * 480://153600
                return "HVGA";//Half-size VGA
            case 480 * 640://307200
                return "VGA";//Video Graphics Array, like Motorola ME632
            case 640 * 960://614400
                if (min == 600)//case 600 * 1024://614400
                {
                    return "WSVGA 2";
                }
                return "DVGA";//Double-size VGA, like Meizu MX
            case 480 * 800://384000
                return "WVGA";//Wide VGA
            case 480 * 854://409920
                return "FWVGA";//Full WVGA
            case 600 * 800://480000
                return "SVGA";//Super VGA, like ONDA Vi30W
            //case 576 * 1024://589824
            //    return "WSVGA 1";//Wide SVGA
            //case 600 * 1024://614400
            //    return "WSVGA 2";//Like CUBE U25GT
            /*
             * Extended Graphics Array
             */
            case 768 * 1024://786432
                return "XGA";//Extended Graphics Array
            case 768 * 1280://983040
                return "WXGA 1";//Wide XGA, like Google Nexus 4
            case 800 * 1280://1024000
                return "WXGA 2";//Like Google Nexus 7
            //case 1200 * 1600://1920000
            //    return "UXGA";//Ultra XGA
            case 1200 * 1920://2304000
                return "WUXGA";//Widescreen UXGA, like Google Nexus 7 Ⅱ
            case 1536 * 2048://3145728
                return "QXGA";//Quad XGA, like Google Nexus 9
            case 1600 * 2560://4096000
                return "WQXGA";//Wide QXGA, like Google Nexus 10
            /*
             * High-Definition
             */
            case 540 * 960://518400
                return "QHD";//Quarter HD
            case 720 * 1280://921600
                return "HD";//High-Definition
            //case 900 * 1600://1440000
            //    return "HD+";//HD Plus
            case 1080 * 1920://2073600
                return "FHD";//Full HD
            //case 1080 * 2048://2211840
            //    return "2K";
            case 1440 * 2560://3686400
                return "QHD";//Quad HD, also WQHD, 2K, like Google Nexus 6
            case 2160 * 3840://8294400
                return "UHD";//Ultra HD, also 4K
            //case 2160 * 4096://8847360
            //    return "4K";
            /*
             * Other
             */
            case 480 * 960://460800
                return UNKNOWN;//Like Coolpad 9900
            case 1080 * 1800://1944000
                return UNKNOWN;//Like Meizu MX 3
            case 1152 * 1920://2211840
                return UNKNOWN;//Like Meizu MX 4
            case 1536 * 2560://3932160
                return UNKNOWN;//Like Meizu MX 4 Pro
            default:
                return UNKNOWN;
        }
    }

    /**
     * Constant for screenLayout: bits that encode the size.
     * @param sl_size_mask "screenLayout & SCREENLAYOUT_SIZE_MASK"
     */
    public static String getSLSizeMaskStr(int sl_size_mask)
    {
        switch (sl_size_mask)
        {
            case Configuration.SCREENLAYOUT_SIZE_UNDEFINED://0
                return "SCREENLAYOUT_SIZE_UNDEFINED";
            case Configuration.SCREENLAYOUT_SIZE_SMALL://1
                return "SCREENLAYOUT_SIZE_SMALL";
            case Configuration.SCREENLAYOUT_SIZE_NORMAL://2
                return "SCREENLAYOUT_SIZE_NORMAL";
            case Configuration.SCREENLAYOUT_SIZE_LARGE://3
                return "SCREENLAYOUT_SIZE_LARGE";
            case Configuration.SCREENLAYOUT_SIZE_XLARGE://4
                return "SCREENLAYOUT_SIZE_XLARGE";
            default:
                return UNKNOWN;
        }
    }

    /**
     * Constant for screenLayout: bits that encode the size.
     * @param sl_size_mask "screenLayout & SCREENLAYOUT_SIZE_MASK"
     * @return Device type by screen size, "Handset" or "Tablet".
     */
    public static String getDeviceTypeStr(int sl_size_mask)
    {
        switch (sl_size_mask)
        {
            case Configuration.SCREENLAYOUT_SIZE_SMALL://1
            case Configuration.SCREENLAYOUT_SIZE_NORMAL://2
                return "Handset";
            case Configuration.SCREENLAYOUT_SIZE_LARGE://3
            case Configuration.SCREENLAYOUT_SIZE_XLARGE://4
                return "Tablet";
            default:
                return UNKNOWN;
        }
    }

    /**
     * The GLES version used by an application.
     * The upper order 16 bits represent the major version and the lower order 16 bits the minor version.
     * @param gles_version "reqGlEsVersion"
     * @return Readable version of OpenGL ES, like 0x00030000 - 3.0
     */
    public static String getGlEsVersion(int gles_version)
    {
        switch (gles_version)
        {
            case ConfigurationInfo.GL_ES_VERSION_UNDEFINED://0
                return "GL_ES_VERSION_UNDEFINED";
            default:
                return String.format("%1$d.%2$d", gles_version >> 16, gles_version & 0x00001111);
        }
    }

    /**
     * The NETWORK_TYPE_xxxx for current data connection.
     * @param network_type "getNetworkType()"
     */
    public static String getNetworkTypeStr(int network_type)
    {
        switch (network_type)
        {
            case TelephonyManager.NETWORK_TYPE_UNKNOWN://0
                return "NETWORK_TYPE_UNKNOWN";
            //GPRS (2.5G)
            case TelephonyManager.NETWORK_TYPE_GPRS://1
                return "NETWORK_TYPE_GPRS";
            //EDGE (2.75G)
            case TelephonyManager.NETWORK_TYPE_EDGE://2
                return "NETWORK_TYPE_EDGE";
            //UMTS (3G)
            case TelephonyManager.NETWORK_TYPE_UMTS://3
                return "NETWORK_TYPE_UMTS";
            //CDMA: Either IS95A or IS95B, also cdmaOne (2G)
            case TelephonyManager.NETWORK_TYPE_CDMA://4
                return "NETWORK_TYPE_CDMA";
            //EVDO revision 0 (3G)
            case TelephonyManager.NETWORK_TYPE_EVDO_0://5
                return "NETWORK_TYPE_EVDO_0";
            //EVDO revision A (3G)
            case TelephonyManager.NETWORK_TYPE_EVDO_A://6
                return "NETWORK_TYPE_EVDO_A";
            //1xRTT, also CDMA2000 1x (2.5G/2.75G)
            case TelephonyManager.NETWORK_TYPE_1xRTT://7
                return "NETWORK_TYPE_1xRTT";
            //HSDPA, also W-CDMA R5 (3.5G)
            case TelephonyManager.NETWORK_TYPE_HSDPA://8
                return "NETWORK_TYPE_HSDPA";
            //HSUPA, also W-CDMA R6 (3.5G)
            case TelephonyManager.NETWORK_TYPE_HSUPA://9
                return "NETWORK_TYPE_HSUPA";
            //HSPA also W-CDMA R5/R6 (3G)
            case TelephonyManager.NETWORK_TYPE_HSPA://10
                return "NETWORK_TYPE_HSPA";
            //iDEN (2G)
            case TelephonyManager.NETWORK_TYPE_IDEN://11
                return "NETWORK_TYPE_IDEN";
            //EVDO revision B (3G)
            case TelephonyManager.NETWORK_TYPE_EVDO_B://12
                return "NETWORK_TYPE_EVDO_B";
            //LTE (4G)
            case TelephonyManager.NETWORK_TYPE_LTE://13
                return "NETWORK_TYPE_LTE";
            //eHRPD, also Enhanced 1xEVDO (3G)
            case TelephonyManager.NETWORK_TYPE_EHRPD://14
                return "NETWORK_TYPE_EHRPD";
            //HSPA+, also W-CDMA R7 (3.75G)
            case TelephonyManager.NETWORK_TYPE_HSPAP://15
                return "NETWORK_TYPE_HSPAP";
            //GSM (2G)
            //This is marked with "@hide".
            case 16://TelephonyManager.NETWORK_TYPE_GSM
                return "NETWORK_TYPE_GSM";
            //TD-SCDMA (3G)
            //This is marked with "@hide".
            case 17://TelephonyManager.NETWORK_TYPE_TD_SCDMA
                return "NETWORK_TYPE_TD_SCDMA";
            default:
                return UNKNOWN;
        }
    }

    /**
     * @return An explicit MIME data type.
     */
    public static String getMIMEType(String file_name)
    {
        if (file_name.endsWith(".txt"))
        {
            return "text/plain";
        }
        else if (file_name.endsWith(".apk"))
        {
            return "application/vnd.android.package-archive";
        }
        else
        {
            return "*/*";
        }
    }

    /**
     * The current width of the available screen space, in dp units.
     * @param screen_width_dp "screenWidthDp"
     */
    public static String getScreenWidthDpStr(int screen_width_dp)
    {
        switch (screen_width_dp)
        {
            case Configuration.SCREEN_WIDTH_DP_UNDEFINED://0
                return "SCREEN_WIDTH_DP_UNDEFINED";
            default:
                return UNKNOWN;
        }
    }

    /**
     * The current height of the available screen space, in dp units.
     * @param screen_height_dp "screenHeightDp"
     */
    public static String getScreenHeightDpStr(int screen_height_dp)
    {
        switch (screen_height_dp)
        {
            case Configuration.SCREEN_HEIGHT_DP_UNDEFINED://0
                return "SCREEN_HEIGHT_DP_UNDEFINED";
            default:
                return UNKNOWN;
        }
    }

    /**
     * You can verify which runtime is in use by calling System.getProperty("java.vm.version").
     * If ART is in use, the property's value is "2.0.0" or higher.
     * @param java_vm_version "getProperty("java.vm.version")"
     */
    public static String getVMType(String java_vm_version)
    {
        if (java_vm_version.startsWith("2."))
        {
            return "ART";
        }
        return "Dalvik";
    }

}
