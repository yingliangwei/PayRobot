package com.miraclegarden.payrobot;


import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;

import com.miraclegarden.payrobot.activity.MainActivity;
import com.miraclegarden.payrobot.helper.MySqliteHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MyAccessibilityService extends AccessibilityService {
    public static Timer timer;
    private boolean isJ = true;
    private String Activity;
    private int start;
    private SharedPreferences config;
    private MySqliteHelper mySqliteHelper;
    private AccessibilityNodeInfo bill;
    private boolean t = false;//防止重复触发，让其暂停一会


    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        mySqliteHelper = new MySqliteHelper(this);
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (t) {
                    try {
                        Thread.sleep(30 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                AimFloat.setMessage(1);
                MyGesture();
            }
        }).start();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        config = getSharedPreferences("config", MODE_PRIVATE);
        start = config.getInt("start", 0);
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
                //开始获取全部账单模拟个个点击
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
                t = true;
                //获取到收款信息开刷新
                AimFloat.setMessage(1);
                MyGesture();
                //if (event != null && event.getClassName() != null)
                //dfsnode(time, accessibilityEvent.getSource(), 0, accessibilityEvent.getEventType());
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
               start(event);
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
                //AimFloat.setMessage("25");
                //Log.d("事件", "25");
                //窗体改变,打印太多
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                //AimFloat.setMessage("26");
                Log.d("事件", "26");
                /**
                 * 界面事件
                 * 进入界面才能获取到Activity界面名字
                 */
                Activity = accessibilityEvent.getClassName().toString();
                TransactionDetails(event);
                break;
        }
    }

    //获取交易详细界面
    void TransactionDetails(AccessibilityNodeInfo nodeInfo) {
        List<AccessibilityNodeInfo> nodeInfos = new ArrayList<>();
        getAccessibilityNodeInfoS(nodeInfos, nodeInfo);
        boolean isText = isText(nodeInfos, "交易详情");
        if (isText) {
            //返回节点
            AccessibilityNodeInfo fin = nodeInfos.get(2);
            //转账人节点
            AccessibilityNodeInfo name = nodeInfos.get(3);
            String nameStr = name.getText().toString();
            String type = nameStr.substring(0, 2);
            nameStr = nameStr.substring(5);
            //金额节点
            AccessibilityNodeInfo money = nodeInfos.get(4);
            String moneyStr = money.getText().toString();
            moneyStr = moneyStr.substring(moneyStr.indexOf(" 收入 ") + 4).replace("点", ".").replace(" ", "").replace("元", "");

            //交易状态节点
            AccessibilityNodeInfo state = nodeInfos.get(5);
            String stateStr = state.getText().toString();
            stateStr = stateStr.substring(5);
            //收款钱包
            AccessibilityNodeInfo wall = nodeInfos.get(6);
            String wallStr = wall.getText().toString();
            wallStr = wallStr.substring(5);
            //时间
            AccessibilityNodeInfo time = nodeInfos.get(7);
            String timeStr = time.getText().toString();
            timeStr = timeStr.substring(4);
            //凭证号
            AccessibilityNodeInfo uuid = nodeInfos.get(8);
            String uuidStr = uuid.getText().toString();
            uuidStr = uuidStr.substring(4);

            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type", type);
                jsonObject.put("name", nameStr);
                jsonObject.put("money", moneyStr);
                jsonObject.put("state", stateStr);
                jsonObject.put("wall", wallStr);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
                Date date = simpleDateFormat.parse(timeStr);
                long ts = date.getTime() / 1000;
                String res = String.valueOf(ts);
                jsonObject.put("time", res);
                jsonObject.put("uuid", uuidStr);
                String text = jsonObject.toString();
                if (someMethod(text)) {
                    if (bill == null) {
                        return;
                    }
                    String json = getString(bill.getText().toString());
                    //text为信息订单
                    upload(text, json);
                } else {
                    Log.d("数据库已经存在", text);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            performClickNodeInfo(fin);
        }
    }


    /**
     * 上传信息到服务器
     *
     * @param json
     */
    void upload(String text, String json) {
        if (config == null) {
            AimFloat.setMessage("上传失败：配置为空");
            return;
        }

        String url = config.getString("url", "1");
        if (url.equals("1")) {
            AimFloat.setMessage("上传失败: 未设置url");
            return;
        }

        uploadPost(url, text, json);
    }

    void uploadPost(String url, String text, String json) {
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = getFormBody(url, text);
        if (requestBody == null) {
            return;
        }
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AimFloat.setMessage("上传失败" + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                //判断是否是200
                //获取服务器数据
                String textx = response.body().string();
                AimFloat.setMessage(json + "上传成功:" + textx);
                //储存到本地
                inst(text, json);
                bill = null;
            }
        });
    }

    RequestBody getFormBody(String url, String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            String type = jsonObject.getString("type");
            String name = jsonObject.getString("name");
            String time = jsonObject.getString("time");
            String money = jsonObject.getString("money");
            String state = jsonObject.getString("state");
            String wall = jsonObject.getString("wall");
            String uuid = jsonObject.getString("uuid");
            String sign = sign(url, type, name, money, time, state, wall, uuid);
            String text = "转账类型:" + type + "|转账人:" + name + "|时间:" + time + "|金额:" + money + "|交易状态:" + state + "|收款钱包:" + wall + "|凭证号:" + uuid;
            AimFloat.setMessage(text);
            String post = "sign=" + sign + "&timestamp=" + time + "&type=" + type + "&name=" + name + "&money=" + money + "&uuid=" + uuid + "&wall=" + wall + "&state=" + state;
            String aes = ShortcutEncryption.java_openssl_encrypt(post);
            FormBody.Builder formBody = new FormBody.Builder();
            formBody.add("data", aes);
            return formBody.build();
        } catch (Exception e) {
            AimFloat.setMessage("准备上传服务器失败,JSON解析失败");
            e.printStackTrace();
        }
        return null;
    }

    String sign(String url, String type, String name, String money, String time, String state, String wall, String uuid) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("type", type);
            jsonObject.put("name", name);
            jsonObject.put("money", money);
            jsonObject.put("state", state);
            jsonObject.put("wall", wall);
            jsonObject.put("uuid", uuid);
            String key = StingToMD5(url);
            //从字符串获取key
            jsonObject.put("key", key);
            jsonObject.put("time", time);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //sendMessage("密匙:"+ jsonObject.toString());
        return StingToMD5(jsonObject.toString());
    }

    String StingToMD5(String text) {
        try {
            byte[] s = MessageDigest.getInstance("md5").digest(text.getBytes(StandardCharsets.UTF_8));
            //16位
            return new BigInteger(1, s).toString(16);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 判断集合中是否存在该字符串
     *
     * @param infos
     * @param text
     * @return
     */
    private boolean isText(List<AccessibilityNodeInfo> infos, String text) {
        for (AccessibilityNodeInfo nodeInfo : infos) {
            if (nodeInfo.getText().toString().equals(text)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 点击节点
     *
     * @return true表示点击成功
     */
    boolean performClickNodeInfo(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo != null) {
            if (nodeInfo.isClickable()) {
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return true;
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

    void start(AccessibilityNodeInfo node) {
        if (node == null) {
            return;
        }
        List<AccessibilityNodeInfo> billS = getBill(node);
        new Thread(() -> {
            for (AccessibilityNodeInfo nodeInfo : billS) {
                bill = nodeInfo;
                performClickNodeInfo(nodeInfo);
                while (true) {
                    if (bill == null) {
                        break;
                    }
                }
            }
        }).start();
    }

    /**
     * 注意，如果是通知获取成功以后获取界面的会变成android.widget.FrameLayout
     *
     * @param node
     * @param num
     */
    void getDetailed(AccessibilityNodeInfo node, int num) {
        if (node == null) {
            return;
        }
        List<AccessibilityNodeInfo> nodeInfos = getBill(node);
        Log.d("无障碍集合长度", "" + nodeInfos.size());
        if (nodeInfos.size() != 0) {
            if (start == 0) {
                AimFloat.setMessage("欢迎第一次使用!\n第一次记录全部账单信息以免重复上传！");
                //判断是否是第一次启动，第一次启动进行记录
                for (AccessibilityNodeInfo info : nodeInfos) {
                    try {
                        String json = getString(info.getText().toString());
                        Log.d("第一次记录数据", json);
                        inst(json, null);
                    } catch (JSONException | ParseException e) {
                        e.printStackTrace();
                    }
                }
                SharedPreferences.Editor editor = config.edit();
                editor.putInt("start", 1);
                editor.apply();
                AimFloat.setMessage("记录完毕！可正常使用");
            } else if (start == 1) {
                for (AccessibilityNodeInfo nodeInfo : nodeInfos) {
                    try {
                        String json = getString(nodeInfo.getText().toString());
                        if (someMethod(json)) {
                            bill = nodeInfo;
                            Log.d("不存在", json);
                            performClickNodeInfo(nodeInfo);
                        } else {
                            Log.d("数据库", "已经存在于数据库");
                        }
                    } catch (JSONException | ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 插入数据
     *
     * @param str
     */
    void inst(String str, String detailed) {
        SQLiteDatabase db = mySqliteHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("name", str);
        contentValues.put("detailed", detailed);
        db.insert("bill", null, contentValues);
    }

    /**
     * 判断表是否存在该字符串
     *
     * @param s
     * @return
     */
    boolean someMethod(String s) {
        return !hasData(s);
    }

    /**
     * 检查数据库中是否已经有该条记录
     */
    boolean hasData(String tempName) {
        Cursor cursor = mySqliteHelper.getReadableDatabase().rawQuery(
                "select id as _id,name from bill where name =?", new String[]{tempName});
        //判断是否有下一个
        return cursor.moveToNext();
    }

    /**
     * 获取集合节点
     *
     * @param node
     * @return
     */
    void getAccessibilityNodeInfoS(List<AccessibilityNodeInfo> nodeInfos, AccessibilityNodeInfo node) {
        if (node == null) {
            Log.e("无障碍", "AccessibilityNodeInfo为空");
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
     * 获取账单全部节点
     *
     * @param info
     * @return
     */
    List<AccessibilityNodeInfo> getBill(AccessibilityNodeInfo info) {
        List<AccessibilityNodeInfo> nodeInfos = new ArrayList<>();
        List<AccessibilityNodeInfo> nodeInfoList = new ArrayList<>();
        getAccessibilityNodeInfoS(nodeInfoList, info);
        for (AccessibilityNodeInfo node : nodeInfoList) {
            if (node != null) {
                if (node.getText() != null && node.getText().length() > 30 && !node.getText().toString().contains("支出")) {
                    if (node.getClassName().equals("android.widget.Button")) {
                        nodeInfos.add(node);
                    }
                }
            }
        }
        return nodeInfos;
    }


    /**
     * 解析出账单
     *
     * @param text
     * @throws JSONException
     * @throws ParseException
     */
    String getString(String text) throws JSONException, ParseException {
        String k[] = text.split(" ");
        String name = k[0].substring(5);
        String type = text.substring(0, 2);
        //时间
        String data = k[1];
        String data1 = k[2];
        String time = data + data1;
        String time1 = dateToStamp(time);

        String money = text.substring(text.indexOf(" 收入 ") + 4).replace("点", ".").replace(" ", "").replace("元", "");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", type);
        jsonObject.put("name", name);
        jsonObject.put("time", time1);
        jsonObject.put("money", money);
        //Log.d("分割账单信息", "类型:" + type + "|转账人:" + name + "|时间:" + time1 + "|金额:" + money);
        return jsonObject.toString();
    }

    /**
     * 时间转换成时间戳,参数和返回值都是字符串
     *
     * @param s
     * @return res
     * @throws ParseException
     */
    String dateToStamp(String s) throws ParseException {
        String res;
        //设置时间模版
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日HH:mm");
        Date date = simpleDateFormat.parse(s);
        long ts = date.getTime() / 1000;
        res = String.valueOf(ts);
        return res;
    }


    String getActivity(AccessibilityEvent event) {
        String activityName = event.getClassName().toString();
        //activityName = activityName.substring(activityName.indexOf(" "), activityName.indexOf("}"));
        Log.e("当前窗口activity", "=================" + activityName);
        return activityName;
    }

    void MyGesture() {//仿滑动
        Path path = new Path();
        path.moveTo(540, 900);//滑动起点
        path.lineTo(540, 1500);//滑动终点
        GestureDescription.Builder builder = new GestureDescription.Builder();
        GestureDescription description = builder.addStroke(new GestureDescription.StrokeDescription(path, 100L, 1500L)).build();
        //100L 第一个是开始的时间，第二个是持续时间
        dispatchGesture(description, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                MainActivity.sendMessage("滑动事件:成功");
                Log.e("触发事件", "成功");
                t = false;
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.e("触发事件", "失败");
                t = false;
                MainActivity.sendMessage("滑动事件:失败");
            }
        }, null);
    }


    @Override
    public void onInterrupt() {

    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }
}