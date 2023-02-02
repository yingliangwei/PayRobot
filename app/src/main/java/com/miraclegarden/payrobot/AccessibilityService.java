package com.miraclegarden.payrobot;

import android.accessibilityservice.GestureDescription;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

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

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AccessibilityService extends android.accessibilityservice.AccessibilityService {
    private final String TAG = "AccessibilityService";
    private final MySqliteHelper helper = new MySqliteHelper(this);
    private SharedPreferences config;
    private String PackageName;
    private int billSize = 0;
    private AccessibilityNodeInfo bill;
    private JSONObject jsonObject = new JSONObject();
    //记录上一个的返回节点
    private AccessibilityNodeInfo fin;
    private volatile boolean isText;
    private boolean isStart = true;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30 * 1000);
                    MyGesture();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        config = getSharedPreferences("config", MODE_PRIVATE);
        PackageName = event.getPackageName().toString();
        if (!event.getPackageName().equals("cn.gov.pbc.dcep")) {
           /* Intent intent = new Intent();
            ComponentName cn = new ComponentName("packageName", "默认启动的activity");
            intent.setComponent(cn);
            startActivity(intent);*/
            return;
        }

        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_HOVER_ENTER) {
            //钱包来了通知
            //MyGesture();
        } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            //刷新改变事件
            SCROLLED(event.getSource());
        } else if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            AccessibilityNodeInfo nodeInfo = event.getSource();
            //切换界面其他，可以获取Activity名
            CHANGED(nodeInfo);
        }
    }


    /**
     * 获取交易详细界面
     *
     * @param source
     */
    private void CHANGED(AccessibilityNodeInfo source) {
        List<AccessibilityNodeInfo> nodeInfos = new ArrayList<>();
        getAccessibilityNodeInfoS(nodeInfos, source);
        isText = isText(nodeInfos, "交易详情");

        //进入到问题反馈页面获取到钱包，判断是否上一次记录为空
        boolean isf = isText(nodeInfos, "问题反馈");
        if (isf) {
            if (jsonObject.length() != 0) {
                //成功获取到钱包编号
                String str = nodeInfos.get(nodeInfos.size() - 5).getText().toString();
                str = str.substring(str.length() - 4);
                try {
                    //记录
                    jsonObject.put("wall1", str);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                String md5 = StingToMD5(jsonObject.toString());
                if (!hasData(md5)) {
                    upload(jsonObject.toString());
                    isStart = true;
                    System.out.println("不存在" + jsonObject);
                } else {
                    System.out.println("已经存在" + jsonObject);
                }

                //返回节点
                System.out.println("点击返回");
                click(nodeInfos, "返回");
                Thread thread1 = new Thread(() -> {
                    while (true) {
                        if (isText) {
                            break;
                        }
                    }
                    System.out.println("返回界面" + isText);
                    if (fin != null) {
                        System.out.println("点击上一个，返回");
                        performClickNodeInfo(fin);
                        fin = null;
                        jsonObject = new JSONObject();
                    }
                });
                if (!thread1.isAlive()) {
                    thread1.start();
                }
            } else {
                click(nodeInfos, "返回");
            }
        }

        for (AccessibilityNodeInfo nodeInfo : nodeInfos) {
            //获取到通知,直接放弃原来订单
            if (nodeInfo.getText().toString().equals("查看")) {
                AccessibilityNodeInfo accessibilityNodeInfo = nodeInfos.get(1);
                String wall1 = accessibilityNodeInfo.getText().toString();
                int start = wall1.indexOf("向您尾号为") + 5;
                wall1 = wall1.substring(wall1.indexOf("向您尾号为") + 5, start + 4);
                if (jsonObject.length() != 0) {
                    try {
                        jsonObject.put("wall1", wall1);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
                System.out.println(bill);
                //details = null;
                fin = null;
                performClickNodeInfo(nodeInfo);
            }
            //获取到通知以后会返回到首页，所以得点击
            if (nodeInfo.getText().toString().equals("交易记录")) {
                performClickNodeInfo(nodeInfo);
            }
        }

        //进入交易详情，判断是否为空，然后记录
        if (jsonObject.length() == 0) {
            if (isText) {
                getDetails(nodeInfos);
                if (jsonObject.length() == 0) {
                    Toast.makeText(this, "请勿触碰！", Toast.LENGTH_SHORT).show();
                    click(nodeInfos, "返回");
                }
                System.out.println("成功解析出账单" + jsonObject);
                for (AccessibilityNodeInfo nodeInfo : nodeInfos) {
                    String text = nodeInfo.getText().toString();
                    if (text.equals("返回")) {
                        fin = nodeInfo;
                    }
                    if (text.contains("对此订单有疑问")) {
                        performClickNodeInfo(nodeInfo);
                    }
                }

                //对此订单疑问获取钱包id
                //performClickNodeInfo(nodeInfos.get(nodeInfos.size() - 1));
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
     * 上传信息到服务器
     */
    void upload(String text) {
        if (config == null) {
            AimFloat.setMessage("上传失败：配置为空");
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
                if (bill != null) {
                    System.out.println("插入状态" + inst(StingToMD5(bill.getText().toString())));
                }
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
     * MD5加密
     *
     * @param text
     * @return
     */
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
     * 插入数据
     *
     * @param str
     */
    boolean inst(String str) {
        if (!hasData(str)) {
            SQLiteDatabase db = helper.getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put("name", str);
            System.out.println(db.insert("bill", null, contentValues));
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
     * 获取详细
     *
     * @param nodeInfos
     */
    JSONObject getDetails(List<AccessibilityNodeInfo> nodeInfos) {
        if (nodeInfos.size() < 7) {
            return null;
        }
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
            System.out.println(ts > tss);
            if (ts > tss) {
                return jsonObject;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.fillInStackTrace();
        }
        return null;
    }

    Thread thread;

    /**
     * 开始获取账单信息
     *
     * @param nodeInfo
     */
    public void SCROLLED(AccessibilityNodeInfo nodeInfo) {
        //获取全部账单信息
        List<AccessibilityNodeInfo> infos = getBill(nodeInfo);
        if (thread != null) {
            return;
        }
        thread = new Thread(() -> {
            //开始执行遍历点击每一个查看
            for (AccessibilityNodeInfo info : infos) {
                String text = StingToMD5(info.getText().toString());
                if (!hasData(text)) {
                    bill = info;
                    try {
                        long ts = getTime(info.getText().toString());
                        long tss = config.getLong("time", System.currentTimeMillis());
                        System.out.println(ts > tss);
                        if (ts > tss) {
                            System.out.println("不存在" + info.getText().toString());
                            performClickNodeInfo(info);
                            Thread.sleep(4000);
                            inst(text);
                        }
                    } catch (Exception e) {
                        e.fillInStackTrace();
                    }
                } else {
                    System.out.println("存在");
                }
            }
            thread = null;
        });
        thread.start();
    }


    /**
     * 解析出账单
     *
     * @param text
     * @throws JSONException
     * @throws ParseException
     */
    long getTime(String text) throws JSONException, ParseException {
        String k[] = text.split(" ");
        String name = k[0].substring(5);
        String type = text.substring(0, 2);
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

    @Override
    public void onInterrupt() {

    }
}
