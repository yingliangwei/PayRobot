package com.miraclegarden.payrobot.accessibilityService;

import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;

import com.example.accessibilitylib.util.CollectionDataUtils;
import com.miraclegarden.payrobot.AimFloat;
import com.miraclegarden.payrobot.ShortcutEncryption;
import com.miraclegarden.payrobot.helper.MySqliteHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Accessibility1Service extends android.accessibilityservice.AccessibilityService implements Runnable {
    private SharedPreferences config;
    private MySqliteHelper helper;

    @Override
    protected void onServiceConnected() {
        helper = new MySqliteHelper(this);
        new Thread(this).start();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        config = getSharedPreferences("config", MODE_PRIVATE);
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            //列表刷新事件
            TYPE_VIEW_SCROLLED(event.getSource());
        } else if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            //界面切换开始的数据，通常获取不全
            TYPE_WINDOW_STATE_CHANGED(event.getSource());
        } else if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            //界面切换，数据加载完毕
            TYPE_WINDOW_CONTENT_CHANGED(event.getSource());
        }
    }

    private void TYPE_WINDOW_CONTENT_CHANGED(AccessibilityNodeInfo source) {

    }

    private void TYPE_WINDOW_STATE_CHANGED(AccessibilityNodeInfo source) {

    }

    private synchronized void TYPE_VIEW_SCROLLED(AccessibilityNodeInfo source) {
        List<AccessibilityNodeInfo> accessibilityNodeInfos = new ArrayList<>();
        CollectionDataUtils.getAccessibilityNodeInfoDS(accessibilityNodeInfos, source);
        //获取账单信息
        List<AccessibilityNodeInfo> nodeInfos = getBill(accessibilityNodeInfos);
        JSONArray jsonArray = new JSONArray();
        for (AccessibilityNodeInfo nodeInfo : nodeInfos) {
            String text = nodeInfo.getText().toString();
            JSONObject jsonObject = getString(text);
            if (jsonObject == null) {
                return;
            }
            //判断是否上传过
            String md5 = CollectionDataUtils.StingToMD5(jsonObject.toString());
            if (!hasData(md5)) {
                String time = getString(jsonObject, "time");
                if (time != null) {
                    long ts = Long.parseLong(time);
                    long tss = config.getLong("time", System.currentTimeMillis()) / 1000;
                    //判断账单时间是否大于本地时间,最新订单
                    if (ts > tss) {
                        jsonArray.put(jsonObject);
                        System.out.println("数据" + jsonArray);
                        //uploadPost(url, jsonObject.toString());
                        inst(md5);
                    }
                }
            }
        }

        System.out.println("长度" + jsonArray.length());
        if (jsonArray.length() != 0) {
            String url = config.getString("url", "1");
            String ID = config.getString("ID", "1");
            if (url.equals("1")) {
                AimFloat.setMessage("上传失败: 未设置url");
                return;
            }
            if (ID.equals("1")) {
                AimFloat.setMessage("上传失败：未设置钱包编号");
            }
            uploadPost(url, ID, jsonArray.toString());
        }
    }

    private int billSize;

    /**
     * 上传服务器
     *
     * @param url  地址
     * @param json json
     * @param id   钱包编号
     */
    void uploadPost(String url, String id, String json) {
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = getFormBody(url, id, json);
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
                AimFloat.setMessage("成功提交数量:" + billSize);
            }
        });
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
            db.insert("bill", null, contentValues);
            return true;
        }
        return false;
    }


    /**
     * 获取参数
     *
     * @param url
     * @param json
     * @return
     */
    RequestBody getFormBody(String url, String id, String json) {
        try {
            JSONObject jsonObject = new JSONObject();
            long time = System.currentTimeMillis();
            jsonObject.put("data", json);
            jsonObject.put("time", time);
            String sign = sign(url, time);
            AimFloat.setMessage(json);
            String post = "sign=" + sign + "&timestamp=" + time + "&data=" + jsonObject + "&id=" + id;
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
     * @param time
     * @return
     */
    String sign(String url, long time) {
        JSONObject jsonObject = new JSONObject();
        try {
            String key = CollectionDataUtils.StingToMD5(url);
            //从字符串获取key
            jsonObject.put("key", key);
            jsonObject.put("time", time);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return CollectionDataUtils.StingToMD5(jsonObject.toString());
    }


    String getString(JSONObject jsonObject, String key) {
        try {
            return jsonObject.getString(key);
        } catch (JSONException e) {
            return null;
        }
    }


    /**
     * 检查数据库中是否已经有该条记录
     */
    boolean hasData(String tempName) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select id as _id,name from bill where name =?", new String[]{tempName});
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
            String[] k = text.split(" ");
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
    public void onInterrupt() {

    }

    @Override
    public void run() {
        while (true) {
            CollectionDataUtils.Gesture(this, new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    super.onCompleted(gestureDescription);
                    //print("滑动成功");
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    super.onCancelled(gestureDescription);
                    //print("滑动失败");
                }
            });
            try {
                java.lang.Thread.sleep(30 * 1000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

}
