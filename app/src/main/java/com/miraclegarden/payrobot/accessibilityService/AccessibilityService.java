package com.miraclegarden.payrobot.accessibilityService;

import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import androidx.annotation.NonNull;

import com.example.accessibilitylib.util.CollectionDataUtils;
import com.miraclegarden.payrobot.AimFloat;
import com.miraclegarden.payrobot.ShortcutEncryption;
import com.miraclegarden.payrobot.helper.MySqliteHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AccessibilityService extends android.accessibilityservice.AccessibilityService implements Runnable {
    String PackageName, Activity, TAG = "AccessibilityService", wall1;
    SharedPreferences Config;
    Thread Thread;
    boolean Debug = false, isRun, isStart, isOK, isNotice, isYes;
    MySqliteHelper helper;
    JSONObject jsonObject;
    private int billSize;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Config = getSharedPreferences("config", MODE_PRIVATE);
        //界面名
        String activity = event.getClassName().toString();
        if (activity.equals("Activity")) {
            Activity = activity;
        }
        PackageName = event.getPackageName().toString();
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            //刷新改变事件
            SCROLLED(event.getSource());
        } else if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            AccessibilityNodeInfo nodeInfo = event.getSource();
            if (nodeInfo == null) {
                return;
            }
            STATE_CHANGED(nodeInfo);
        } else if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            AccessibilityNodeInfo nodeInfo = event.getSource();
            if (nodeInfo == null) {
                return;
            }
            //切换页面，信息加载完
            CHANGED(event.getSource());
        }
    }

    private void STATE_CHANGED(AccessibilityNodeInfo nodeInfo) {
        List<AccessibilityNodeInfo> nodeInfos = new ArrayList<>();
        CollectionDataUtils.getAccessibilityNodeInfoS(nodeInfos, nodeInfo);
        //点击交易界面，来通知以后他会返回交易界面
        CollectionDataUtils.click2(nodeInfos, "交易记录");

        //来通知了
        boolean isc = CollectionDataUtils.isText(nodeInfos, "查看");
        if (isc) {
            //告诉后面是通知事件
            isNotice = true;
            //前面账单遍历退出
            setBreak();
            AccessibilityNodeInfo tv_detail = CollectionDataUtils.getIDS(nodeInfos, "cn.gov.pbc.dcep:id/tv_detail");
            if (tv_detail == null) {
                print("通知获取失败");
                return;
            }
            String wall1 = tv_detail.getText().toString();
            int start = wall1.indexOf("向您尾号为") + 5;
            this.wall1 = wall1.substring(wall1.indexOf("向您尾号为") + 5, start + 4);
            JSONObject json = getNString(wall1);
            if (json == null) {
                print("通知栏数据解析失败");
                return;
            }
            String md5 = CollectionDataUtils.StingToMD5(json.toString());
            boolean IsInst = inst(md5);
            print("通知栏数据记录状态：" + IsInst + "|信息：" + json);
            CollectionDataUtils.click(nodeInfos, "查看");
        }

        CHANGED(nodeInfo);
    }

    //SCROLLED
    void SCROLLED(AccessibilityNodeInfo nodeInfo) {
        List<AccessibilityNodeInfo> accessibilityNodeInfos = new ArrayList<>();
        CollectionDataUtils.getAccessibilityNodeInfoDS(accessibilityNodeInfos, nodeInfo);
        List<AccessibilityNodeInfo> nodeInfos = getBill(accessibilityNodeInfos);
        if (Thread != null) {
            return;
        }
        Thread = new Thread(() -> AccessibilityService.this.run(nodeInfos));
        Thread.start();
    }


    void CHANGED(AccessibilityNodeInfo source) {
        List<AccessibilityNodeInfo> nodeInfos = new ArrayList<>();
        CollectionDataUtils.getAccessibilityNodeInfoS(nodeInfos, source);

        //进入交易详情
        boolean isText = CollectionDataUtils.isText(nodeInfos, "交易详情");
        if (isText) {
            print("进入交易详情");
            jsonObject = getDetails(nodeInfos);
            if (jsonObject == null) {
                //boolean f = CollectionDataUtils.click(nodeInfos, "返回");
                //print("交易数据详情解析失败!返回上一页状态:" + f);
                return;
            }
            //是通知进入的
            if (isNotice) {
                isNotice = false;
                if (wall1 == null) {
                    boolean dui = CollectionDataUtils.click(nodeInfos, "返回");
                    print("获取通知编码号失败,点击返回:" + dui);
                    return;
                }
                try {
                    jsonObject.put("wall1", wall1);
                    //判断是否上传过
                    String md5 = CollectionDataUtils.StingToMD5(jsonObject.toString());
                    if (!hasData(md5)) {
                        //本地数据库没有上传记录则开始上传
                        upload(jsonObject.toString());
                    } else {
                        boolean f = CollectionDataUtils.click(nodeInfos, "返回");
                        print("点击返回:" + f + "|已经记录" + jsonObject);
                    }
                    //告诉前面可以返回了
                    isOK = true;
                    //可以进行账单遍历了
                    isRun = false;
                    isStart = false;
                } catch (JSONException e) {
                    print("错误" + e);
                    return;
                }
            }

            if (isOK) {
                isOK = false;
                boolean dui = CollectionDataUtils.click(nodeInfos, "返回");
                print("上传完成,点击返回:" + dui);
            } else {
                //进去获取订单编号
                boolean dui = CollectionDataUtils.click2(nodeInfos, "对此订单有疑问");
                print("点击：对此订单有疑问状态:" + dui);
            }

        }

        //进入问题反馈
        boolean isw = CollectionDataUtils.isText(nodeInfos, "问题反馈");
        if (isw) {
            print("进入问题反馈");
            if (jsonObject == null) {
                return;
            }
            //获取第5节点得到编码号
            String str = nodeInfos.get(nodeInfos.size() - 5).getText().toString();
            str = str.substring(str.length() - 4);
            if (!isNumeric(str)) {
                return;
            }
            try {
                jsonObject.put("wall1", str);
                //判断是否上传过
                String md5 = CollectionDataUtils.StingToMD5(jsonObject.toString());
                if (!hasData(md5)) {
                    //本地数据库没有上传记录则开始上传
                    upload(jsonObject.toString());
                } else {
                    print("已经记录" + jsonObject);
                }
                //告诉前面可以返回了
                isOK = true;
                //可以记录该信息了
                isYes = true;
                //开始返回
                boolean hui = CollectionDataUtils.click(nodeInfos, "返回");
                print("问题反馈点击返回" + hui);
            } catch (JSONException e) {
                print("错误" + e);
            }
        }
    }

    public boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        return isNum.matches();
    }


    private void upload(String toString) {
        if (Config == null) {
            return;
        }
        String url = Config.getString("url", "1");
        if (url.equals("1")) {
            AimFloat.setMessage("上传失败: 未设置url");
            return;
        }
        uploadPost(url, toString);
    }

    /**
     * 上传服务器
     *
     * @param url
     * @param json
     */
    void uploadPost(String url, String json) {
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = getFormBody(url, json);
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
                billSize++;
                //储存到本地
                String md5 = CollectionDataUtils.StingToMD5(json);
                inst(md5);
                AimFloat.setMessage("成功提交数量:" + billSize);
            }
        });
    }

    /**
     * 获取参数
     *
     * @param url
     * @param json
     * @return
     */
    RequestBody getFormBody(String url, String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            String type = jsonObject.getString("type");
            String name = jsonObject.getString("name");
            String time = jsonObject.getString("time");
            String money = jsonObject.getString("money");
            String state = jsonObject.getString("state");
            String wall = jsonObject.getString("wall1");
            String uuid = jsonObject.getString("uuid");
            String sign = sign(url, type, name, money, time, state, wall, uuid);
            String text = "转账类型:" + type + "|转账人:" + name + "|时间:" + time + "|金额:" + money + "|交易状态:" + state + "|收款钱包:" + wall + "|凭证号:" + uuid;
            AimFloat.setMessage(text);
            String post = "sign=" + sign + "&timestamp=" + time + "&type=" + type + "&name=" + name + "&money=" + money + "&uuid=" + uuid + "&wall=" + wall + "&state=" + state;
            //AimFloat.setMessage(post);
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

    /**
     * 获取密匙
     *
     * @param url
     * @param type
     * @param name
     * @param money
     * @param time
     * @param state
     * @param wall
     * @param uuid
     * @return
     */
    String sign(String url, String type, String name, String money, String time, String state, String wall, String uuid) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("type", type);
            jsonObject.put("name", name);
            jsonObject.put("money", money);
            jsonObject.put("state", state);
            jsonObject.put("wall", wall);
            jsonObject.put("uuid", uuid);
            String key = CollectionDataUtils.StingToMD5(url);
            //从字符串获取key
            jsonObject.put("key", key);
            jsonObject.put("time", time);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //AimFloat.setMessage("密匙加密顺序：" + jsonObject);
        //print("密匙:" + jsonObject + "|连接" + url);
        return CollectionDataUtils.StingToMD5(jsonObject.toString());
    }

    /**
     * 获取详细
     *
     * @param nodeInfos
     */
    @SuppressLint("SimpleDateFormat")
    JSONObject getDetails(List<AccessibilityNodeInfo> nodeInfos) {
        if (nodeInfos.size() != 10) {
            return null;
        }
        //转账人节点
        AccessibilityNodeInfo name = nodeInfos.get(3);
        String nameStr = name.getText().toString();
        if (!nameStr.contains("-来自")) {
            boolean dui = CollectionDataUtils.click(nodeInfos, "返回");
            print("数据错误,点击返回:" + dui);
            return null;
        }
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
            long ts = date.getTime();
            long tss = Config.getLong("time", System.currentTimeMillis());
            String res = String.valueOf(ts);
            jsonObject.put("time", res);
            jsonObject.put("uuid", uuidStr);
            boolean isTime = ts > tss;
            System.out.println("是否过期:" + isTime);
            if (ts > tss) {
                return jsonObject;
            } else {
                boolean dui = CollectionDataUtils.click(nodeInfos, "返回");
                print("过期,点击返回:" + dui);
            }
        } catch (Exception e) {
            e.fillInStackTrace();
            return null;
        }
        return null;
    }

    JSONObject getNString(String text) {
        try {
            String name = text.substring(0, text.indexOf("向您尾号为"));
            String money = text.substring(text.indexOf("¥") + 1);
            String time = String.valueOf(CollectionDataUtils.getTimeMills() / 1000);
            JSONObject jsonObject1 = new JSONObject();
            jsonObject1.put("name", name);
            jsonObject1.put("time", time);
            jsonObject1.put("money", money);
            return jsonObject1;
        } catch (Exception e) {
            e.fillInStackTrace();
        }
        return null;
    }


    /**
     * 获取账单全部节点
     *
     * @param nodeInfoList
     * @return
     */
    List<AccessibilityNodeInfo> getBill(List<AccessibilityNodeInfo> nodeInfoList) {
        List<AccessibilityNodeInfo> nodeInfos = new ArrayList<>();
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

    @Override
    protected void onServiceConnected() {
        helper = new MySqliteHelper(this);
        new Thread(this).start();
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void run() {
        while (true) {
            CollectionDataUtils.Gesture(this, new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    super.onCompleted(gestureDescription);
                    print("滑动成功");
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    super.onCancelled(gestureDescription);
                    print("滑动失败");
                }
            });
            try {
                java.lang.Thread.sleep(30 * 1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    void run(List<AccessibilityNodeInfo> infos) {
        for (AccessibilityNodeInfo nodeInfo : infos) {
            String text = nodeInfo.getText().toString();
            JSONObject jsonObject = getString(text);
            if (jsonObject == null) {
                print("解析账单出错！");
                return;
            }
            String str = CollectionDataUtils.StingToMD5(jsonObject.toString());
            if (str == null) {
                print("加密错误！");
                return;
            }
            boolean isMD5 = !hasData(str);
            if (isMD5) {
                String time = getString(jsonObject, "time");
                if (time == null) {
                    print("账单解析出的时间戳错误");
                    return;
                }
                long ts = Long.parseLong(time);
                long tss = Config.getLong("time", System.currentTimeMillis()) / 1000;
                //判断账单时间是否大于本地时间
                if (ts > tss) {
                    boolean isClick = CollectionDataUtils.performClickNodeInfo(nodeInfo);
                    print("账单列表点击:" + isClick + text);
                    while (true) {
                        //退出循环
                        if (isRun) {
                            break;
                        }
                    }
                    //退出全部循环
                    if (isStart) {
                        break;
                    }
                    while (true) {
                        //等待后面完成然后判断是否上传成功然后插入
                        if (isYes) {
                            isYes = false;
                            //不管前面有没有完成，直接记录
                            boolean in = inst(str);
                            print("插入账单信息：" + in);
                            break;
                        }
                    }
                }
            }
        }
        //可以重新运行
        Thread = null;
        print("账单遍历完毕");
    }

    //退出账单遍历循环
    void setBreak() {
        isRun = true;
        isStart = true;
    }

    String getString(JSONObject jsonObject, String key) {
        try {
            return jsonObject.getString(key);
        } catch (JSONException e) {
            return null;
        }
    }

    void print(String text) {
        if (Debug) {
            AimFloat.setMessage(text);
        }
        Log.d(TAG, text);
    }

    /**
     * 插入数据
     *
     * @param str
     */
    boolean inst(String str) {
        if (!hasData(str)) {
            SQLiteDatabase db = helper.getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put("name", str);
            print("插入状态:" + db.insert("bill", null, contentValues));
            return true;
        }
        return false;
    }

    /**
     * 检查数据库中是否已经有该条记录
     */
    boolean hasData(String tempName) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "select id as _id,name from bill where name =?", new String[]{tempName});
        //判断是否有下一个
        return cursor.moveToNext();
    }

    /**
     * 解析出账单
     *
     * @param text
     * @throws JSONException
     * @throws ParseException
     */
    JSONObject getString(String text) {
        try {
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
            //jsonObject.put("type", type);
            jsonObject.put("name", name);
            jsonObject.put("time", time1);
            jsonObject.put("money", money);
            return jsonObject;
        } catch (Exception e) {
            e.fillInStackTrace();
        }
        return null;
    }

    /**
     * 时间转换成时间戳,参数和返回值都是字符串
     *
     * @param s
     * @return res
     * @throws ParseException
     */
    @SuppressLint("SimpleDateFormat")
    String dateToStamp(String s) throws ParseException {
        String res;
        //设置时间模版
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日HH:mm");
        Date date = simpleDateFormat.parse(s);
        long ts = (date != null ? date.getTime() : 0) / 1000;
        res = String.valueOf(ts);
        return res;
    }
}
