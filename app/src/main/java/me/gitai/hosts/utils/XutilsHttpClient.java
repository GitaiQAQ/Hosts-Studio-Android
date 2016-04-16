package me.gitai.hosts.utils;

import android.content.Context;

import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.util.PreferencesCookieStore;

/**
 * Created by dp on 15-8-22.
 */
public class XutilsHttpClient {
    private static HttpUtils client;
    /**
     * 单例模式获取实例对象
     *
     * @param context
     *            应用程序上下文
     * @return HttpUtils对象实例
     */
    public synchronized static HttpUtils getInstence(Context context) {
        if (client == null) {
            // 设置请求超时时间为10秒
            client = new HttpUtils(1000 * 10);
            client.configSoTimeout(1000 * 10);
            client.configResponseTextCharset("UTF-8");
            // 保存服务器端(Session)的Cookie
            PreferencesCookieStore cookieStore = new PreferencesCookieStore(
                    context);
            //cookieStore.clear(); // 清除原来的cookie
            client.configCookieStore(cookieStore);
        }
        return client;
    }
}
