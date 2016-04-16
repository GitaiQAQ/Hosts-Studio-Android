package me.gitai.library.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.TrafficStats;

import java.util.HashMap;
import java.util.Map;

/**
 * User: mcxiaoke
 * Date: 14-6-3
 * Time: 16:17
 */
public abstract class TrafficUtils {
    public static final String TAG_SESSION = "session_traffic_info";

    private static Map<String, Long> sReceivedBytes = new HashMap<String, Long>();
    private static Map<String, Long> sSendBytes = new HashMap<String, Long>();

    private TrafficUtils() {
    }

    /**
     * 开始流量统计
     *
     * @param context Context
     * @param tag     traffic tag
     * @return received bytes
     */
    public static long start(Context context, String tag) {
        final int uid = getUid(context);
        if (uid > 0) {
            long appRxValue = TrafficStats.getUidRxBytes(uid);
            long appTxValue = TrafficStats.getUidTxBytes(uid);
            sReceivedBytes.put(tag, appRxValue);
            sSendBytes.put(tag, appTxValue);
            L.d("rxValue=" + appRxValue / 1000 + " txValue=" + appTxValue / 1000 + " uid=" + uid);
            return appRxValue;
        }
        return 0;
    }

    /**
     * 计算当前流量
     *
     * @param context Context
     * @param tag     traffic tag
     * @return received bytes
     */
    public static long current(Context context, String tag) {
        Long appRxValue = sReceivedBytes.get(tag);
        Long appTxValue = sSendBytes.get(tag);
        if (appRxValue == null || appRxValue == null) {
            L.d("appRxValue or appTxValue is null.");
            return 0;
        }
        final int uid = getUid(context);
        long appRxValue2 = TrafficStats.getUidRxBytes(uid);
        long appTxValue2 = TrafficStats.getUidTxBytes(uid);
        long rxValue = appRxValue2 - appRxValue;
        long txValue = appTxValue2 - appTxValue;
        L.d("rxValue=" + rxValue / 1000 + " txValue=" + txValue / 1000 + " uid=" + uid);
        return rxValue;

    }

    /**
     * 统计TAG流量
     *
     * @param context Context
     * @param tag     traffic tag
     * @return received bytes
     */
    public static long stop(Context context, String tag) {
        Long appRxValue = sReceivedBytes.remove(tag);
        Long appTxValue = sSendBytes.remove(tag);
        if (appRxValue == null || appRxValue == null) {
            L.d("appRxValue or appTxValue is null.");
            return 0;
        }
        final int uid = getUid(context);
        long appRxValue2 = TrafficStats.getUidRxBytes(uid);
        long appTxValue2 = TrafficStats.getUidTxBytes(uid);
        long rxValue = appRxValue2 - appRxValue;
        long txValue = appTxValue2 - appTxValue;
        L.d("rxValue=" + rxValue / 1000 + " txValue=" + txValue / 1000 + " uid=" + uid);
        return rxValue;

    }

    public static int getUid(Context context) {
        try {
            final PackageManager pm = context.getPackageManager();
            final String pn = context.getPackageName();
            ApplicationInfo ai = pm.getApplicationInfo(pn, 0);
            return ai.uid;
        } catch (NameNotFoundException e) {
            L.d("ex=" + e);
            return -1;
        }
    }
}
