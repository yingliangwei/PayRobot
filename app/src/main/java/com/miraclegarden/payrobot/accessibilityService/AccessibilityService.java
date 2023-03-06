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
        List<AccessibilityNodeInfo> nodeInfos = new ArrayList<>();
        CollectionDataUtils.getAccessibilityNodeInfoS(nodeInfos, source);
        goTransactionDetails(nodeInfos);
        goProblemFeedback(nodeInfos);
    }


    private void goProblemFeedback(List<AccessibilityNodeInfo> nodeInfos) {
        String tag = "问题反馈";
        boolean isText = CollectionDataUtils.isText(nodeInfos, tag);
        if (isText) {
            print("进入" + tag);
            //获取第5节点得到编码号
            String str = nodeInfos.get(nodeInfos.size() - 5).getText().toString();
            str = str.substring(str.length() - 4);
            if (!isNumeric(str)) {
                print(tag + "钱包编码" + str);
                //这里不能返回，因为加载数据中可能不是数字
                return;
            }
            this.wall = str;
            CollectionDataUtils.click(nodeInfos, "返回");
        }
    }

    public boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        return isNum.matches();
    }


    private void goTransactionDetails(List<AccessibilityNodeInfo> nodeInfos) {
        String tag = "交易详情";
        //进入交易详情
        boolean isText = CollectionDataUtils.isText(nodeInfos, tag);
        if (!isText) {
            return;
        }
        print("进入" + tag);

        //是通知点击进入
        if (wall == null) {
            print(tag + "点击对此订单有疑问");
            //如果wall一直为空一直死循环
            boolean is = CollectionDataUtils.click2(nodeInfos, "对此订单有疑问");
            if (!is) {
                print(tag + "点击失败返回");
                print(tag + wall);
            }
            return;
        }

        print(tag + "解析账单");
        JSONObject jsonObject = getDetails(nodeInfos);
        if (jsonObject == null) {
            print(tag + "解析详细订单错误！");
            return;
        }

        try {
            jsonObject.put("wall", wall);
            //判断是否上传过
            String md5 = CollectionDataUtils.StingToMD5(jsonObject.toString());
            if (hasData(md5)) {
                print(tag + "已经记录:" + jsonObject);
                //因为已经上传过了，所以wall可以为空，通知前面已经完毕
                wall = null;
                isHttpOk = true;
                //进入交易详情
                print(tag + "点击" + CollectionDataUtils.click(nodeInfos, "返回"));
                if (body != null) {
                    print(tag + "忘记插入" + body);
                    String md = CollectionDataUtils.StingToMD5(body);
                    print(tag + "插入" + inst(md));
                }
                return;
            }
            print(tag + "开始上传");
            upload(nodeInfos, jsonObject.toString());
            //直接插入以免重复上传
            inst(md5);
        } catch (Exception e) {
            print("插入错误" + e);
            e.fillInStackTrace();
        }
    }

    private void upload(List<AccessibilityNodeInfo> nodeInfos, String toString) {
        if (config == null) {
            return;
        }
        String url = config.getString("url", "1");
        if (url.equals("1")) {
            AimFloat.setMessage("上传失败: 未设置url");
            return;
        }
        if (body == null) {
            isHttpOk = true;
            wall = null;
            print("账单信息为空解决上传！");
            return;
        }
        uploadPost(nodeInfos, url, toString);

    }

    private int billSize;
    private boolean isHttpOk;

    /**
     * 上传服务器
     *
     * @param nodeInfos
     * @param url
     * @param json
     */
    void uploadPost(List<AccessibilityNodeInfo> nodeInfos, String url, String json) {
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
                isHttpOk = true;
                wall = null;
                body = null;
                String tag = "交易详情";
                //进入交易详情
                boolean isText = CollectionDataUtils.isText(nodeInfos, tag);
                if (isText) {
                    print(tag + "点击" + CollectionDataUtils.click(nodeInfos, "返回"));
                }
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
                isHttpOk = true;
                wall = null;
                if (body != null) {
                    String md4 = CollectionDataUtils.StingToMD5(body);
                    //记录通知栏来的信息，进行记录，以免二次打开
                    inst(md4);
                }
                body = null;
                String tag = "交易详情";
                //进入交易详情
                boolean isText = CollectionDataUtils.isText(nodeInfos, tag);
                if (isText) {
                    print(tag + "点击" + CollectionDataUtils.click(nodeInfos, "返回"));
                }
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
            String wall = jsonObject.getString("wall");
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


    private boolean put(JSONObject jsonObject, String key, String value) {
        try {
            jsonObject.put(key, value);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    /**
     * 获取详细
     *
     * @param nodeInfos
     */
    @SuppressLint("SimpleDateFormat")
    JSONObject getDetails(List<AccessibilityNodeInfo> nodeInfos) {
        String tag = "交易详情";
        if (nodeInfos.size() != 10) {
            print(tag + "长度不等于10");
            return null;
        }
        //转账人节点
        AccessibilityNodeInfo name = nodeInfos.get(3);
        String nameStr = name.getText().toString();
        if (!nameStr.contains("-来自")) {
            boolean dui = CollectionDataUtils.click(nodeInfos, "返回");
            print(tag + "数据错误,点击返回:" + dui);
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
            long tss = config.getLong("time", System.currentTimeMillis());
            String res = String.valueOf(ts);
            jsonObject.put("time", res);
            jsonObject.put("uuid", uuidStr);
            boolean isTime = ts > tss;
            print(tag + "是否过期:" + isTime);
            if (ts > tss) {
                return jsonObject;
            } else {
                boolean dui = CollectionDataUtils.click(nodeInfos, "返回");
                print(tag + "过期,点击返回:" + dui);
            }
        } catch (Exception e) {
            print(tag + e);
            e.fillInStackTrace();
            return null;
        }
        print(tag + "莫名其妙");
        return null;
    }

    private volatile boolean isRun;

    private void TYPE_WINDOW_STATE_CHANGED(AccessibilityNodeInfo source) {
        List<AccessibilityNodeInfo> nodeInfos = new ArrayList<>();
        CollectionDataUtils.getAccessibilityNodeInfoS(nodeInfos, source);
        //goNotice(nodeInfos);
        boolean isjy = CollectionDataUtils.isText(nodeInfos, "交易记录");
        if (isjy && isHttpOk) {
            //前面步骤以完成
            isRun = true;
            // thread = null;
            print("前面步骤完成" + isRun);
        }

        //进入交易详情
        boolean isText = CollectionDataUtils.isText(nodeInfos, "交易详情");
        if (isText && this.body == null) {
            isRun = true;
            print("交易详情页面不对");
            CollectionDataUtils.click(nodeInfos, "返回");
        } else if (isText && isHttpOk) {
            //前面步骤以完成
            isRun = true;
            print("上传步骤成功");
            CollectionDataUtils.click(nodeInfos, "返回");
        } else if (isText && this.wall != null) {
            String tag = "返回获取到的详细账单";
            //返回进入的交易详情
            JSONObject jsonObject = getDetails(nodeInfos);
            if (jsonObject == null) {
                print(tag + "解析详细订单错误！");
                return;
            }
            try {
                jsonObject.put("wall", wall);
                //判断是否上传过
                String md5 = CollectionDataUtils.StingToMD5(jsonObject.toString());
                if (hasData(md5)) {
                    print(tag + "已经记录:" + jsonObject);
                    //因为已经上传过了，所以wall可以为空，通知前面已经完毕
                    wall = null;
                    isHttpOk = true;
                    //进入交易详情
                    print(tag + "点击" + CollectionDataUtils.click(nodeInfos, "返回"));
                    if (body != null) {
                        print(tag + "忘记插入" + body);
                        String md = CollectionDataUtils.StingToMD5(body);
                        print(tag + "插入" + inst(md));
                    }
                    return;
                }
                print(tag + "开始上传");
                upload(nodeInfos, jsonObject.toString());
                inst(md5);
            } catch (Exception e) {
                e.fillInStackTrace();
            }
        }

        boolean is = CollectionDataUtils.isText(nodeInfos, "问题反馈");
        if (is) {
            print("问题反馈页面不对");
            CollectionDataUtils.click(nodeInfos, "返回");
        }

        for (AccessibilityNodeInfo nodeInfo1 : nodeInfos) {
            if (nodeInfo1.getClassName().equals("android.widget.TextView")) {
                //返回的交易界面
                //点击交易界面，来通知以后他会返回交易界面
                CollectionDataUtils.click2(nodeInfos, "交易记录");
                isNotice = false;
            }
        }
    }

    private boolean isNotice;

    //来通知以后所有刷新数据会到刷新里面
    private boolean ik;
    private String wall;
    private String body;

    private void goNotice(List<AccessibilityNodeInfo> nodeInfos) {
        String tag = "通知";
        boolean is = CollectionDataUtils.isText(nodeInfos, "查看");
        if (is) {
            print("进入" + tag);
            //来过通知了
            thread = null;
            ik = true;
            //告诉他们，来通知了
            isNotice = true;
            isRun = true;
            //禁止刷新
            isGesture = false;
            AccessibilityNodeInfo tv_detail = CollectionDataUtils.getIDS(nodeInfos, "cn.gov.pbc.dcep:id/tv_detail");
            if (tv_detail == null) {
                isNotice = false;
                isRun = false;
                isGesture = true;
                print(tag + "获取内容失败");
                return;
            }
            String wall1 = tv_detail.getText().toString();
            int start = wall1.indexOf("向您尾号为") + 5;
            wall = wall1.substring(wall1.indexOf("向您尾号为") + 5, start + 4);
            JSONObject json = getNString(wall1);
            if (json == null) {
                isNotice = false;
                isRun = false;
                isGesture = true;
                print(tag + "数据解析失败");
                return;
            }
            this.body = json.toString();
            if (CollectionDataUtils.click(nodeInfos, "查看")) {
                print(tag + "点击查看成功");
            }
        }
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

    private Thread thread;

    private void TYPE_VIEW_SCROLLED(AccessibilityNodeInfo source) {
        List<AccessibilityNodeInfo> accessibilityNodeInfos = new ArrayList<>();
        CollectionDataUtils.getAccessibilityNodeInfoDS(accessibilityNodeInfos, source);
        List<AccessibilityNodeInfo> nodeInfos = getBill(accessibilityNodeInfos);
        if (thread != null && !isNotice) {
            print("不允许重新开始" + isNotice);
        } else {
            thread = new Thread(TYPE_VIEW_SCROLLED_Runnable(nodeInfos));
            thread.start();
        }
    }

    Runnable TYPE_VIEW_SCROLLED_Runnable(List<AccessibilityNodeInfo> nodeInfos) {
        return () -> {
            String tag = "交易记录";
            isGesture = false;
            for (AccessibilityNodeInfo nodeInfo : nodeInfos) {
                String text = nodeInfo.getText().toString();
                JSONObject jsonObject = getString(text);
                print(tag + jsonObject);
                if (jsonObject != null) {
                    String str = CollectionDataUtils.StingToMD5(jsonObject.toString());
                    if (str != null) {
                        if (!hasData(str)) {
                            String time = getString(jsonObject, "time");
                            if (time != null) {
                                long ts = Long.parseLong(time);
                                long tss = config.getLong("time", System.currentTimeMillis()) / 1000;
                                //判断账单时间是否大于本地时间
                                if (ts > tss) {
                                    boolean cli = CollectionDataUtils.performClickNodeInfo(nodeInfo);
                                    do {
                                        if (isNotice) {
                                            break;
                                        }
                                        cli = CollectionDataUtils.performClickNodeInfo(nodeInfo);
                                    } while (!cli);
                                    print("点击成功" + nodeInfo);
                                    this.body = jsonObject.toString();
                                    //进入了交易详情，开始等待
                                    while (!isRun) {
                                        //退出循环
                                        //print(tag + "等待前面操作完毕");
                                    }
                                    print(tag + "等待前面操作完毕");
                                    isRun = false;
                                    isHttpOk = false;
                                }
                            }
                        }
                    }
                }
            }
            isGesture = true;
            thread = null;
            print("账单遍历完毕");
        };
    }

    String getString(JSONObject jsonObject, String key) {
        try {
            return jsonObject.getString(key);
        } catch (JSONException e) {
            return null;
        }
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

    private boolean debug;

    void print(String text) {
        if (debug) {
            AimFloat.setMessage(text);
        }
        Log.d("Accessibility2Service", text);
    }

    private boolean isGesture = true;

    @Override
    public void run() {
        while (true) {
            if (isGesture) {
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
            }
            try {
                java.lang.Thread.sleep(30 * 1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
