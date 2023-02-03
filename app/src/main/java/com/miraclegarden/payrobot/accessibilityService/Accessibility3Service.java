package com.miraclegarden.payrobot.accessibilityService;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.example.accessibilitylib.util.CollectionDataUtils;
import com.miraclegarden.payrobot.helper.MySqliteHelper;

import java.util.ArrayList;
import java.util.List;

public class Accessibility3Service extends AccessibilityService {
    private SharedPreferences config;
    private MySqliteHelper helper;

    @Override
    protected void onServiceConnected() {
        helper = new MySqliteHelper(this);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo nodeInfo = event.getSource();
        List<AccessibilityNodeInfo> nodeInfos = new ArrayList<>();
        CollectionDataUtils.getAccessibilityNodeInfoS(nodeInfos, nodeInfo);
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            //点击事件改变也会触发
            WINDOW_CONTENT_CHANGED(nodeInfos);
        }
        for (AccessibilityNodeInfo nodeInfo1 : nodeInfos) {
            System.out.println("事件：" + event.getEventType() + "|" + nodeInfo1);
        }
        System.out.println("分割");
    }

    void WINDOW_CONTENT_CHANGED(List<AccessibilityNodeInfo> nodeInfos) {
        //点击，翻转,代表已经进入数字人民货币
        AccessibilityNodeInfo main_front_tv_org_nickname = CollectionDataUtils.getIDS(nodeInfos, "cn.gov.pbc.dcep:id/main_front_tv_org_nickname");
        if (main_front_tv_org_nickname != null) {
            CollectionDataUtils.performClickNodeInfo(main_front_tv_org_nickname);
        } else {
            print("main_front_tv_org_nickname null");
        }

    }

    void print(String text) {
        Log.e("Accessibility3Service", text);
    }

    @Override
    public void onInterrupt() {

    }
}
