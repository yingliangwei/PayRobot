package com.miraclegarden.payrobot.activity;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import com.miraclegarden.payrobot.AimFloat;
import com.miraclegarden.payrobot.helper.MySqliteHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AimFloatCallback implements Callback {
    MySqliteHelper mySqliteHelper;
    String json;
    String text;

    public AimFloatCallback(MySqliteHelper mySqliteHelper, String text, String json) {
        this.mySqliteHelper = mySqliteHelper;
        this.json = json;
        this.text = text;
    }

    @Override
    public void onFailure(@NonNull Call call, @NonNull IOException e) {
        AimFloat.setMessage("上传失败" + e.getMessage());
    }

    @Override
    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
        //判断是否是200
        //获取服务器数据
        String text = response.body().string();
        AimFloat.setMessage(json + "上传成功:" + text);
        //储存到本地
        try {
            JSONObject jsonObject = new JSONObject(this.text);
            String uuid = jsonObject.getString("uuid");
            inst(this.text, uuid);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        inst(this.json, null);
        AimFloat.setMessage("已经记录");
    }


    /**
     * 插入数据
     *
     * @param str
     */
    void inst(String str, String uuid) {
        SQLiteDatabase db = mySqliteHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("name", str);
        if (uuid == null) {
            contentValues.put("uuid", uuid);
            db.insert("bill", null, contentValues);
        } else {
            db.insert("bi", null, contentValues);
        }
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
}
