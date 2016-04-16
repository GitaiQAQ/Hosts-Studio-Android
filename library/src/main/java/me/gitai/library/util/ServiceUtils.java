package me.gitai.library.util;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager;
import android.content.Context;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import java.util.Iterator;
import java.util.List;

/**
 * Created by dphdjy on 15-10-30.
 */
public class ServiceUtils {
    public static boolean isServiceRuning(Context packageContext,Class<?> cls){
        Iterator localIterator = ((ActivityManager)packageContext.getSystemService(Context.ACTIVITY_SERVICE)).getRunningServices(Integer.MAX_VALUE).iterator();
        while (localIterator.hasNext())
        {
            ActivityManager.RunningServiceInfo localRunningServiceInfo = (ActivityManager.RunningServiceInfo)localIterator.next();

            if (cls.getName().equals(localRunningServiceInfo.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAccessibilityEnabled(Context packageContext, String paramString)
    {
        List<AccessibilityServiceInfo> runingServices = ((AccessibilityManager)packageContext.getSystemService(Context.ACCESSIBILITY_SERVICE))
                .getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK);
        for (AccessibilityServiceInfo service : runingServices){
            String id =service.getId();
            if (id.contains(paramString))
                return true;
        }
        return false;
    }
}
