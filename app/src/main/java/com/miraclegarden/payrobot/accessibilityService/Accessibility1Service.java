package com.miraclegarden.payrobot.accessibilityService;

import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;

import com.miraclegarden.payrobot.activity.MainActivity;
import com.miraclegarden.payrobot.AimFloat;
import com.miraclegarden.payrobot.helper.MySqliteHelper;
import com.miraclegarden.payrobot.ShortcutEncryption;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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

public class Accessibility1Service extends android.accessibilityservice.AccessibilityService {
    private SharedPreferences config;
    //提供给账单列表点击事件
    private Thread thread;
    private MySqliteHelper helper;
    //判断是否在执行遍历订单
    private boolean isRun;
    //进入其他事件
    private boolean isStart = false;
    private JSONObject jsonObject;
    //记录上传次数
    private int billSize;
    private String Activity;
    //交易详情判断是否可以返回
    private boolean isj = false;
    //交易详情判断是否是通知的内容
    private boolean iscs = false;
    //弹窗记录钱包编码
    private String wall1;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        config = getSharedPreferences("config", MODE_PRIVATE);
        //界面名
        String activity = event.getClassName().toString();
        if (activity.contains("Activity")) {
            Activity = activity;
            print(Activity);
        }
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            //刷新改变事件
            SCROLLED(event.getSource());
        } else if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            AccessibilityNodeInfo nodeInfo = event.getSource();
            if (nodeInfo == null) {
                return;
            }
            //切换界面其他，可以获取Activity名
            CHANGED(nodeInfo);
        }
    }

    /**
     * 监听账单详情刷新事件
     *
     * @param nodeInfo
     */
    private void SCROLLED(AccessibilityNodeInfo nodeInfo) {
        //获取全部账单信息
        List<AccessibilityNodeInfo> infos = getBill(nodeInfo);
        //以免重复调用
        if (thread != null) {
            return;
        }
        isStart = false;
        thread = new Thread(() -> Accessibility1Service.this.run(infos));
        //运行继续操作
        thread.start();
    }

    /**
     * 解析刷新获取到账单集合
     *
     * @param infos
     */
    void run(List<AccessibilityNodeInfo> infos) {
        for (AccessibilityNodeInfo node : infos) {
            JSONObject bbblll = getString(node.getText().toString());
            if (bbblll == null) {
                return;
            }
            String bill = StingToMD5(bbblll.toString());
            boolean isHas = !hasData(bill);
            print("存在" + isHas + "|" + bill + "|" + bbblll);
            //判断数据库不存在
            if (isHas) {
                try {
                    long ts = Long.parseLong(bbblll.getString("time"));
                    long tss = config.getLong("time", System.currentTimeMillis()) / 1000;
                    print("时间" + tss);
                    //判断url创建时间，抛弃前面订单
                    if (ts > tss) {
                        print("点击" + bill + "|" + bbblll);
                        performClickNodeInfo(node);
                        //在这里等一下，等前面操作完毕
                        while (true) {
                            if (isRun) {
                                isRun = false;
                                break;
                            }
                        }
                        //是否有最新订单，有则立马结束该遍历行为
                        if (isStart) {
                            break;
                        }
                        //前面操作完毕记录该数据
                    }
                    //这里直接记录
                    inst(bill);
                } catch (Exception e) {
                    e.fillInStackTrace();
                }
            }
        }
        thread = null;
        print("账单遍历完毕");
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
            //Log.d("分割账单信息", "类型:" + type + "|转账人:" + name + "|时间:" + time1 + "|金额:" + money);
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
    String dateToStamp(String s) throws ParseException {
        String res;
        //设置时间模版
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日HH:mm");
        Date date = simpleDateFormat.parse(s);
        long ts = date.getTime() / 1000;
        res = String.valueOf(ts);
        return res;
    }


    JSONObject getNString(String text) {
        try {
            String name = text.substring(0, text.indexOf("向您尾号为"));
            String money = text.substring(text.indexOf("¥") + 1);
            String time = String.valueOf(getTimeMills() / 1000);
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
     * 切换页面事件
     *
     * @param nodeInfo
     */
    private void CHANGED(AccessibilityNodeInfo nodeInfo) {
        List<AccessibilityNodeInfo> nodeInfos = new ArrayList<>();
        getAccessibilityNodeInfoS(nodeInfos, nodeInfo);

        //返回了前面界面,点击交易记录，进入界面
        boolean isJY = isText(nodeInfos, "交易记录");
        if (isJY) {
            click2(nodeInfos, "交易记录");
        }

        boolean isc = isText(nodeInfos, "查看");
        if (isc) {
            //通知来了，让前面停止运行，都别管了，处理该条
            isRun = true;
            isStart = true;
            iscs = true;
            //获取内容节点
            AccessibilityNodeInfo accessibilityNodeInfo = getIDS(nodeInfos, "cn.gov.pbc.dcep:id/tv_detail");
            if (accessibilityNodeInfo != null) {
                String wall1 = accessibilityNodeInfo.getText().toString();
                int start = wall1.indexOf("向您尾号为") + 5;
                this.wall1 = wall1.substring(wall1.indexOf("向您尾号为") + 5, start + 4);
                JSONObject jsonObject = getNString(wall1);
                if (jsonObject != null) {
                    String md5 = StingToMD5(jsonObject.toString());
                    print("通知" + jsonObject + "|" + md5);
                    print("插入" + inst(md5));
                    click(nodeInfos, "查看");
                }
            }
        }

        boolean isText = isText(nodeInfos, "交易详情");
        if (isText) {
            //进入了交易详情界面了
            try {
                //成功解析出数据,提供全局变量
                jsonObject = getDetails(nodeInfos);
                if (jsonObject == null) {
                    print("数组为空");
                    click(nodeInfos, "返回");
                    return;
                }
                //判断后面是否执行完毕
                if (isj) {
                    //告诉子线程遍历等待操作，该过程已完成，可以退出循环了
                    isRun = true;
                    jsonObject = null;
                    isj = false;
                    //执行完毕可以返回
                    click(nodeInfos, "返回");
                } else if (iscs) {
                    //退出等待
                    isRun = true;
                    //来的是通知不需要进入其他页面获取编码
                    if (wall1 != null) {
                        jsonObject.put("wall1", wall1);
                        //判断是否上传过
                        String md5 = StingToMD5(jsonObject.toString());
                        if (!hasData(md5)) {
                            //本地数据库没有上传记录则开始上传
                            upload(jsonObject.toString());
                            print("记录" + jsonObject);
                        } else {
                            print("已经记录" + jsonObject);
                        }
                        //可以返回了
                        click(nodeInfos, "返回");
                    } else {
                        //可以返回了
                        click(nodeInfos, "返回");
                        print("编码号为空");
                    }
                    wall1 = null;
                    //已经结束
                    iscs = false;
                } else {
                    //点击获取编码号
                    click2(nodeInfos, "对此订单有疑问");
                }
            } catch (JSONException | ParseException e) {
                click(nodeInfos, "返回");
                e.fillInStackTrace();
            }
        }

        boolean isw = isText(nodeInfos, "问题反馈");
        //判断集合是否为空,判断前面的数据是否为空
        if (isw && jsonObject != null) {
            //进入问题反馈界面
            //获取第5节点得到编码号
            String str = nodeInfos.get(nodeInfos.size() - 5).getText().toString();
            str = str.substring(str.length() - 4);
            try {
                jsonObject.put("wall1", str);
                //判断是否上传过
                String md5 = StingToMD5(jsonObject.toString());
                if (!hasData(md5)) {
                    //本地数据库没有上传记录则开始上传
                    upload(jsonObject.toString());
                    print("插入" + jsonObject);
                } else {
                    print("已经记录" + jsonObject);
                }
                //告诉前面可以返回了
                isj = true;
                //开始返回
                click(nodeInfos, "返回");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (isw) {
            //不存在直接返回
            click(nodeInfos, "返回");
        }
    }

    /**
     * 获取分钟的时间戳（13位）
     *
     * @return
     */

    private long getTimeMills() {
        LocalDate localDate = LocalDate.now();
        LocalTime localTime = LocalTime.now();
        return LocalDateTime.of(localDate.getYear(), localDate.getMonth(), localDate.getDayOfMonth(), localTime.getHour(), localTime.getMinute(), 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }


    /**
     * 获取id
     *
     * @param nodeInfos
     * @param text
     * @return
     */
    AccessibilityNodeInfo getIDS(List<AccessibilityNodeInfo> nodeInfos, String text) {
        for (AccessibilityNodeInfo nodeInfo : nodeInfos) {
            if (nodeInfo.getViewIdResourceName().equals(text)) {
                return nodeInfo;
            }
        }
        return null;
    }

    /**
     * 打印日志
     *
     * @param text
     */
    void print(String text) {
        boolean debug = false;
        if (debug) {
            AimFloat.setMessage(text);
        }
        String TAG = "Accessibility1Service";
        Log.d(TAG, text);
    }

    /**
     * 上传信息到服务器
     */
    void upload(String text) {
        if (config == null) {
            return;
        }

        String url = config.getString("url", "1");
        if (url.equals("1")) {
            AimFloat.setMessage("上传失败: 未设置url");
            return;
        }

        uploadPost(url, text);
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
                String md5 = StingToMD5(json);
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
            String wall = jsonObject.getString("wall");
            String wall1 = jsonObject.getString("wall1");
            String uuid = jsonObject.getString("uuid");
            String sign = sign(url, type, name, money, time, state, wall, uuid);
            String text = "转账类型:" + type + "|转账人:" + name + "|时间:" + time + "|金额:" + money + "|交易状态:" + state + "|收款钱包:" + wall + "|凭证号:" + uuid + "|收款钱包1:" + wall1;
            AimFloat.setMessage(text);
            String post = "sign=" + sign + "&timestamp=" + time + "&type=" + type + "&name=" + name + "&money=" + money + "&uuid=" + uuid + "&wall=" + wall + "&state=" + state + "&wall1=" + wall1;
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


    /**
     * 找寻节点集合，判断字符串内是否存在该字符串
     *
     * @param nodeInfos
     * @param text
     */
    void click2(List<AccessibilityNodeInfo> nodeInfos, String text) {
        for (AccessibilityNodeInfo accessibilityNodeInfo : nodeInfos) {
            if (accessibilityNodeInfo.getText().toString().contains(text)) {
                performClickNodeInfo(accessibilityNodeInfo);
            }
        }
    }


    /**
     * 找寻该集合是否存在该节点
     *
     * @param nodeInfos
     * @param text
     */
    void click(List<AccessibilityNodeInfo> nodeInfos, String text) {
        for (AccessibilityNodeInfo nodeInfo : nodeInfos) {
            if (nodeInfo.getText().toString().equals(text)) {
                performClickNodeInfo(nodeInfo);
            }
        }
    }

    /**
     * 获取详细
     *
     * @param nodeInfos
     */
    @SuppressLint("SimpleDateFormat")
    JSONObject getDetails(List<AccessibilityNodeInfo> nodeInfos) throws JSONException, ParseException {
        if (nodeInfos.size() != 10) {
            return null;
        }
        //转账人节点
        AccessibilityNodeInfo name = nodeInfos.get(3);
        String nameStr = name.getText().toString();
        if (!nameStr.contains("-来自")) {
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
        System.out.println("是否过期:" + isTime);
        if (ts > tss) {
            return jsonObject;
        }
        return null;
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
     * MD5加密
     *
     * @param text
     * @return
     */
    String StingToMD5(String text) {
        try {
            byte[] s = MessageDigest.getInstance("md5").digest(text.getBytes());
            //16位
            return new BigInteger(1, s).toString(16);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解析出账单
     *
     * @param text
     * @throws ParseException
     */
    @SuppressLint("SimpleDateFormat")
    long getTime(String text) throws ParseException {
        String[] k = text.split(" ");
        //时间
        String data = k[1];
        String data1 = k[2];
        String time = data + data1;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日HH:mm");
        Date date = simpleDateFormat.parse(time);
        //Log.d("分割账单信息", "类型:" + type + "|转账人:" + name + "|时间:" + time1 + "|金额:" + money);
        return date.getTime();
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
     * 获取集合节点
     *
     * @param node
     * @return
     */
    void getAccessibilityNodeInfoS(List<AccessibilityNodeInfo> nodeInfos, AccessibilityNodeInfo node) {
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
     * 获取详细集合节点
     *
     * @param node
     * @return
     */
    void getAccessibilityNodeInfoDS(List<AccessibilityNodeInfo> nodeInfos, AccessibilityNodeInfo node) {
        if (node == null) {
            return;
        }
        for (int i = 0; i < node.getChildCount(); i++) { //遍历子节点
            AccessibilityNodeInfo accessibilityNodeInfo = node.getChild(i);
            if (accessibilityNodeInfo != null) {
                nodeInfos.add(accessibilityNodeInfo);
                getAccessibilityNodeInfoS(nodeInfos, accessibilityNodeInfo);
            }
        }
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

    @Override
    public void onInterrupt() {

    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        helper = new MySqliteHelper(this);
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30 * 1000);
                    if (Activity == null) {
                        return;
                    }
                    if (Activity.equals("com.alipay.mobile.nebulacore.ui.H5Activity")) {
                        MyGesture();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 模拟滑动
     */
    void MyGesture() {
        AimFloat.setMessage(1);
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
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.e("触发事件", "失败");
                MainActivity.sendMessage("滑动事件:失败");
            }
        }, null);
    }

}
