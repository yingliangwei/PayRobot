package com.miraclegarden.payrobot;


import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Button;

import com.miraclegarden.payrobot.Activity.MainActivity;
import com.miraclegarden.payrobot.Helper.MySqliteHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MyAccessibilityService extends AccessibilityService {
    public static Timer timer;
    private boolean isJ = true;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        String time = String.valueOf((System.currentTimeMillis() / 1000)-2);
        AccessibilityNodeInfo event = accessibilityEvent.getSource();
        if (isJ && accessibilityEvent.getPackageName().equals("cn.gov.pbc.dcep")) {
            AimFloat.setMessage("进入数字人民货币");
            isJ = false;
        }
        switch (accessibilityEvent.getEventType()) {
            case AccessibilityEvent.TYPE_ANNOUNCEMENT:
                //AimFloat.sendMessage("1");
                Log.d("事件", "1");
                break;
            case AccessibilityEvent.TYPE_ASSIST_READING_CONTEXT:
                //AimFloat.sendMessage("2");
                Log.d("事件", "2");
                break;
            case AccessibilityEvent.TYPE_GESTURE_DETECTION_END:
                //AimFloat.sendMessage("3");
                Log.d("事件", "3");
                break;
            case AccessibilityEvent.TYPE_GESTURE_DETECTION_START:
                //AimFloat.sendMessage("4");
                Log.d("事件", "4");
                break;
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                //AimFloat.sendMessage("5");
                Log.d("事件", "5");
                break;
            case AccessibilityEvent.TYPE_SPEECH_STATE_CHANGE:
                //AimFloat.sendMessage("6");
                Log.d("事件", "6");
                break;
            case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END:
                //AimFloat.sendMessage("7");
                Log.d("事件", "7");
                break;
            case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START:
                //AimFloat.sendMessage("8");
                Log.d("事件", "8");
                break;
            case AccessibilityEvent.TYPE_TOUCH_INTERACTION_END:
                //AimFloat.sendMessage("9");
                Log.d("事件", "9");
                break;
            case AccessibilityEvent.TYPE_TOUCH_INTERACTION_START:
                //AimFloat.sendMessage("10");
                Log.d("事件", "10");
                break;
            case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED:
                //AimFloat.sendMessage("11");
                Log.d("事件", "11");
                break;
            case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED:
                //AimFloat.sendMessage("12");
                Log.d("事件", "12");
                break;
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                //AimFloat.sendMessage("13");
                Log.d("事件", "13");
                break;
            case AccessibilityEvent.TYPE_VIEW_CONTEXT_CLICKED:
                //AimFloat.sendMessage("14");
                Log.d("事件", "14");
                break;
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                //AimFloat.sendMessage("15");
                Log.d("事件", "15");
                break;
            case AccessibilityEvent.TYPE_VIEW_HOVER_ENTER:
                //AimFloat.sendMessage("16");
                Log.d("事件", "16");
                //获取到收款信息开刷新
                //AimFloat.setMessage(1);
                if (event != null && event.getClassName() != null)
                    dfsnode(time, accessibilityEvent.getSource(), 0, accessibilityEvent.getEventType());
                //模拟刷新两次，以免获取不到
                MyGesture();
                MyGesture();
                break;
            case AccessibilityEvent.TYPE_VIEW_HOVER_EXIT:
                //AimFloat.sendMessage("17");
                Log.d("事件", "17");
                break;
            case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
                //AimFloat.sendMessage("18");
                Log.d("事件", "18");
                break;
            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                //AimFloat.sendMessage("19");
                Log.d("事件", "19");
                if (event != null && event.getClassName() != null)
                    System.out.println("父控件" + event.getClassName());
                getDetailed(event, 0);
                //dfsnode(accessibilityEvent.getSource(), 0, accessibilityEvent.getEventType());
                break;
            case AccessibilityEvent.TYPE_VIEW_SELECTED:
                //AimFloat.sendMessage("20");
                Log.d("事件", "20");
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                //AimFloat.sendMessage("21");
                Log.d("事件", "21");
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED:
                //AimFloat.sendMessage("22");
                Log.d("事件", "22");
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY:
                //AimFloat.sendMessage("23");
                Log.d("事件", "23");
                break;
            case AccessibilityEvent.TYPE_WINDOWS_CHANGED:
                //AimFloat.sendMessage("24");
                Log.d("事件", "24");
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                // AimFloat.setMessage("25");
                //Log.d("事件", "25");
                //这里只能获取到控件类名
                //窗体改变
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                //AimFloat.setMessage("26");
                Log.d("事件", "26");
                /**
                 * 界面事件
                 * 进入界面才能获取到Activity界面名字
                 */
                String Activity = accessibilityEvent.getClassName().toString();
                break;
        }
    }

    public void dfsnode(String time, AccessibilityNodeInfo node, int num, int eve) {
        if (node == null) {
            return;
        }
        //Log.d("数据", "属性:" + num + "|字符串长度" + node.getText().length() + "|类名:" + Activity + "|" + node.toString());
        if (node.getText() != null && node.getClassName().toString().equals("android.widget.TextView")) {
            //列表信息
            AimFloat.sendMessage(time, node.getText().toString());
            MainActivity.sendMessage(node.getText().toString());
        }
        for (int i = 0; i < node.getChildCount(); i++) { //遍历子节点
            dfsnode(time, node.getChild(i), num + 1, eve);
        }
    }

    /**
     * 注意，如果是通知获取成功以后获取界面的会变成android.widget.FrameLayout
     *
     * @param node
     * @param num
     */
    public void getDetailed(AccessibilityNodeInfo node, int num) {
        if (node == null) {
            return;
        }
        if (node.getText() != null && node.getText().length() > 30) {
            if (node.getClassName().equals("android.widget.Button")) {
                try {
                    getString(node.getText().toString());
                } catch (JSONException | ParseException e) {
                    e.printStackTrace();
                }
                //Log.d("数据", "Text:" + node.getText() + "|属性:" + num + "|字符串长度" + node.getText().length() + "|类名:" + Activity + "|" + node.toString());
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) { //遍历子节点
            getDetailed(node.getChild(i), num + 1);
        }
    }

    private String getString(String text) throws JSONException, ParseException {
        String k[] = text.split(" ");
        String name = k[0].substring(5);
        String type = text.substring(0, 2);
        //时间
        String data = k[1];
        String data1 = k[2];
        String time = data + data1;

        String money = text.substring(text.indexOf(" 收入 ") + 4).replace("点", ".").replace(" ", "").replace("元", "");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", type);
        jsonObject.put("name", name);
        jsonObject.put("time", time);
        jsonObject.put("money", money);
        Log.d("分割账单信息", "类型:" + type + "|转账人:" + name + "|时间:" + dateToStamp(time) + "|金额:" + money);
        return data;
    }

    /**
     * 时间转换成时间戳,参数和返回值都是字符串
     *
     * @param s
     * @return res
     * @throws ParseException
     */
    public static String dateToStamp(String s) throws ParseException {
        String res;
        //设置时间模版
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日HH:mm");
        Date date = simpleDateFormat.parse(s);
        long ts = date.getTime();
        res = String.valueOf(ts);
        return res;
    }


    private String getActivity(AccessibilityEvent event) {
        String activityName = event.getClassName().toString();
        //activityName = activityName.substring(activityName.indexOf(" "), activityName.indexOf("}"));
        Log.e("当前窗口activity", "=================" + activityName);
        return activityName;
    }

    private void MyGesture() {//仿滑动
        Path path = new Path();
        path.moveTo(540, 900);//滑动起点
        path.lineTo(540, 1500);//滑动终点
        GestureDescription.Builder builder = new GestureDescription.Builder();
        GestureDescription description = builder.addStroke(new GestureDescription.StrokeDescription(path, 100L, 1500L)).build();
        //100L 第一个是开始的时间，第二个是持续时间
        dispatchGesture(description, new MyCallBack(), null);
    }


    @Override
    public void onInterrupt() {

    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }
}