package com.miraclegarden.payrobot;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.util.Log;

import com.miraclegarden.payrobot.activity.MainActivity;

public class MyCallBack extends AccessibilityService.GestureResultCallback {
    public MyCallBack() {
        super();
    }

    @Override
    public void onCompleted(GestureDescription gestureDescription) {
        super.onCompleted(gestureDescription);
        MainActivity.sendMessage("滑动事件:成功");
        Log.e("触发事件", "成功");
    }

    @Override
    public void onCancelled(GestureDescription gestureDescription) {
        super.onCancelled(gestureDescription);
        Log.e("触发事件", "失败");
        MainActivity.sendMessage("滑动事件:失败");
    }
}