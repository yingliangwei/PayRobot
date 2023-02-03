package com.example.accessibilitylib.util;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.RequiresApi;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

public class CollectionDataUtils {
    private static final String TAG = "CollectionDataUtils";

    /**
     * @param text 打印
     */
    public static void print(String text) {
        Log.d(TAG, text);
    }

    /**
     * 模拟滑动
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void Gesture(AccessibilityService accessibilityService) {
        Path path = new Path();
        path.moveTo(540, 900);//滑动起点
        path.lineTo(540, 1500);//滑动终点
        GestureDescription.Builder builder = new GestureDescription.Builder();
        GestureDescription description = builder.addStroke(new GestureDescription.StrokeDescription(path, 100L, 1500L)).build();
        //100L 第一个是开始的时间，第二个是持续时间
        accessibilityService.dispatchGesture(description, new AccessibilityService.GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.d(TAG, "滑动成功");
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.e(TAG, "滑动失败");
            }
        }, null);
    }

    /**
     * 模拟滑动
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void Gesture(AccessibilityService accessibilityService, AccessibilityService.GestureResultCallback callback) {
        Path path = new Path();
        path.moveTo(540, 900);//滑动起点
        path.lineTo(540, 1500);//滑动终点
        GestureDescription.Builder builder = new GestureDescription.Builder();
        GestureDescription description = builder.addStroke(new GestureDescription.StrokeDescription(path, 100L, 1500L)).build();
        //100L 第一个是开始的时间，第二个是持续时间
        accessibilityService.dispatchGesture(description, callback, null);
    }

    /**
     * @param nodeInfos 集合点
     * @param text      判断集合中是否存在该字符串
     * @return null=不存在|节点
     */
    public static AccessibilityNodeInfo getAccessibilityNodeInfo(List<AccessibilityNodeInfo> nodeInfos, String text) {
        for (AccessibilityNodeInfo info : nodeInfos) {
            if (info.getText().toString().equals(text)) {
                return info;
            }
        }
        return null;
    }

    /**
     * @param nodeInfos 集合
     * @param idName    要找寻的id名
     * @return null=不存在|节点
     */
    public static AccessibilityNodeInfo getIDS(List<AccessibilityNodeInfo> nodeInfos, String idName) {
        for (AccessibilityNodeInfo nodeInfo : nodeInfos) {
            if (nodeInfo.getViewIdResourceName().equals(idName)) {
                return nodeInfo;
            }
        }
        return null;
    }

    /**
     * @param nodeInfos 集合
     * @param text      找寻节点集合，判断字符串内是否存在该字符串
     */
    public static boolean click2(List<AccessibilityNodeInfo> nodeInfos, String text) {
        for (AccessibilityNodeInfo accessibilityNodeInfo : nodeInfos) {
            if (accessibilityNodeInfo.getText().toString().contains(text)) {
                return performClickNodeInfo(accessibilityNodeInfo);
            }
        }
        return false;
    }


    /**
     * @param nodeInfos 集合
     * @param text      找寻该集合是否存在该字符串，然后模拟点击
     */
    public static boolean click(List<AccessibilityNodeInfo> nodeInfos, String text) {
        for (AccessibilityNodeInfo accessibilityNodeInfo : nodeInfos) {
            if (accessibilityNodeInfo.getText().toString().equals(text)) {
                return performClickNodeInfo(accessibilityNodeInfo);
            }
        }
        return false;
    }

    /**
     * @param infos 集合节点
     * @param text  判断集合中是否存在该字符串
     * @return true=存在该字符串
     */
    public static boolean isText(List<AccessibilityNodeInfo> infos, String text) {
        for (AccessibilityNodeInfo nodeInfo : infos) {
            if (nodeInfo.getText().toString().equals(text)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param infos 集合节点
     * @param text  判断集合中是否存在该字符串
     * @return true=存在该字符串
     */
    public static AccessibilityNodeInfo isTextNodeInfo(List<AccessibilityNodeInfo> infos, String text) {
        for (AccessibilityNodeInfo nodeInfo : infos) {
            if (nodeInfo.getText().toString().equals(text)) {
                return nodeInfo;
            }
        }
        return null;
    }

    /**
     * @param nodeInfos 记录集合节点
     * @param node      节点
     */
    public static void getAccessibilityNodeInfoDS(List<AccessibilityNodeInfo> nodeInfos, AccessibilityNodeInfo node) {
        if (node == null) {
            return;
        }
        for (int i = 0; i < node.getChildCount(); i++) { //遍历子节点
            AccessibilityNodeInfo accessibilityNodeInfo = node.getChild(i);
            if (accessibilityNodeInfo != null) {
                nodeInfos.add(accessibilityNodeInfo);
                getAccessibilityNodeInfoDS(nodeInfos, accessibilityNodeInfo);
            }
        }
    }

    /**
     * @param nodeInfos 储存字符串的节点
     * @param node      父节点
     */
    public static void getAccessibilityNodeInfoS(List<AccessibilityNodeInfo> nodeInfos, AccessibilityNodeInfo node) {
        if (node == null) {
            return;
        }
        for (int i = 0; i < node.getChildCount(); i++) { //遍历子节点
            AccessibilityNodeInfo accessibilityNodeInfo = node.getChild(i);
            if (accessibilityNodeInfo != null) {
                CharSequence text = accessibilityNodeInfo.getText();
                if (text != null) {
                    String str = text.toString();
                    str = str.replace(" ", "");
                    if (str.length() != 0) {
                        nodeInfos.add(accessibilityNodeInfo);
                    }
                }
                getAccessibilityNodeInfoS(nodeInfos, accessibilityNodeInfo);
            }
        }
    }


    /**
     * @return 获取分钟的时间戳（13位）
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static long getTimeMills() {
        LocalDate localDate = LocalDate.now();
        LocalTime localTime = LocalTime.now();
        return LocalDateTime.of(localDate.getYear(), localDate.getMonth(), localDate.getDayOfMonth(), localTime.getHour(), localTime.getMinute(), 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * @param nodeInfo 要点击的节点
     * @return true=点击成功
     */
    public static boolean performClickNodeInfo(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo != null) {
            if (nodeInfo.isClickable()) {
                return nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            } else {
                AccessibilityNodeInfo parent = nodeInfo.getParent();
                if (parent != null) {
                    boolean isParentClickSuccess = performClickNodeInfo(parent);
                    parent.recycle();
                    return isParentClickSuccess;
                }
            }
        }
        return false;
    }


    /**
     * @param context 开始运行无障碍
     */
    public static boolean start(Context context, String className) {
        if (!isServiceON(context, className)) {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return false;
        }
        return true;
    }

    /**
     * @param context   Activity
     * @param className 运行中的Services
     * @return ture=正在运行
     */
    public static boolean isServiceON(Context context, String className) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> runningServices = activityManager.getRunningServices(100);
        runningServices.size();
        for (int i = 0; i < runningServices.size(); i++) {
            ComponentName service = runningServices.get(i).service;
            if (service.getClassName().contains(className)) {
                return true;
            }
        }
        return false;
    }


    /**
     * @param text MD5加密
     * @return MD5字符串
     */
    public static String StingToMD5(String text) {
        try {
            byte[] s = MessageDigest.getInstance("md5").digest(text.getBytes());
            String md5code = new BigInteger(1, s).toString(16);
            // 如果生成数字未满32位，需要前面补0
            for (int i = 0; i < 32 - md5code.length(); i++) {
                md5code = "0" + md5code;
            }

            //16位
            return md5code;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
