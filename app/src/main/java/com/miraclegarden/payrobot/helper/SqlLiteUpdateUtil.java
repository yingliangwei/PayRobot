package com.miraclegarden.payrobot.helper;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.annotations.SerializedName;
import com.miraclegarden.payrobot.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @ProjectName: TianDao
 * @Package: com.tiandao.Sqlite.SqlLiteUtli
 * @ClassName: SqlLiteUpdateUtil
 * @Description: 快捷更新数据库
 * @Author: 笑脸
 * @CreateDate: 2021/11/7 10:13
 * @UpdateUser: 更新者
 * @UpdateDate: 2021/11/7 10:13
 * @UpdateRemark: 更新说明
 * @Version: 1.0
 */

public class SqlLiteUpdateUtil {
    private static final String TAG = "SqlLiteUpdateUtil";
    //数据库连接
    private SQLiteDatabase db;
    //表名
    private String table;


    /**
     * 从实体类获取数据
     *
     * @param c 存在数据的Gson实体类
     * @return ContentValues
     * @throws IllegalAccessException
     */
    public static ContentValues initContentValues(Object c) throws IllegalAccessException {
        ContentValues values = new ContentValues();
        Field[] fields = c.getClass().getDeclaredFields();
        //获取class的属性遍历
        for (Field declaredField : fields) {
            //判断属性上是否有Autowired注解
            SerializedName annotation = declaredField.getAnnotation(SerializedName.class);
            if (annotation != null) {
                //私有属性时也可访问
                declaredField.setAccessible(true);
                values.put(annotation.value(), (String) declaredField.get(c));
                /*if (object instanceof UserBean) {
                    UserBean userBean = (UserBean) object;
                    values.put(annotation.value(), userBean.jsonObject());
                } else {
                    values.put(annotation.value(), (String) declaredField.get(c));
                }*/
            }
        }
        return values;
    }

    @SuppressLint("SimpleDateFormat")
    public static ContentValues objectToContentValues(Object o) throws IllegalAccessException {
        ContentValues cv = new ContentValues();
        for (Field field : o.getClass().getFields()) {
            Object value = field.get(o);
            //check if compatible with contentvalues
            if (value instanceof Double || value instanceof Integer || value instanceof R.string || value instanceof Boolean
                    || value instanceof Long || value instanceof Float || value instanceof Short) {
                cv.put(field.getName(), value.toString());
            } else if (value instanceof Date) {
                cv.put(field.getName(), new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date) value));
            }
        }
        return cv;
    }

    /**
     * 该方法只返回一条数据
     *
     * @param db  数据库
     * @param sql SQL
     * @param t   实体类
     * @throws JSONException
     * @throws IllegalAccessException
     */
    public static boolean executeQuery(SQLiteDatabase db, String sql, Object t) throws JSONException, IllegalAccessException {
        Cursor cursor = db.rawQuery(sql, null);
        JSONArray jsonArray = SqlLiteUpdateUtil.getResults(cursor);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            Field[] fields = t.getClass().getDeclaredFields();
            //获取class的属性遍历
            for (Field declaredField : fields) {
                //判断属性上是否有Autowired注解
                SerializedName annotation = declaredField.getAnnotation(SerializedName.class);
                if (annotation != null) {
                    //私有属性时也可访问
                    declaredField.setAccessible(true);
                    declaredField.set(t, jsonObject.get(annotation.value()));
                }
            }
        }
        return jsonArray.length() != 0;
    }

    /**
     * 数据库
     *
     * @param cursor
     * @return
     */
    public static JSONArray getResults(Cursor cursor) {
        JSONArray resultSet = new JSONArray();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            int totalColumn = cursor.getColumnCount();
            JSONObject rowObject = new JSONObject();
            for (int i = 0; i < totalColumn; i++) {
                if (cursor.getColumnName(i) != null) {
                    try {
                        if (cursor.getString(i) != null) {
                            rowObject.put(cursor.getColumnName(i), cursor.getString(i));
                        } else {
                            rowObject.put(cursor.getColumnName(i), "");
                        }
                    } catch (Exception e) {
                        e.fillInStackTrace();
                    }
                }
            }
            resultSet.put(rowObject);
            cursor.moveToNext();
        }
        cursor.close();
        return resultSet;
    }


    /**
     * 插入和判断是否存在该表
     *
     * @param sql    查询语句
     * @param values 要插入的数据
     * @return true=插入成功，false=存在需要更新
     */
    public boolean insert(String sql, ContentValues values) {
        //没有存在该表直接创建
        //select * from user
        @SuppressLint("Recycle")
        Cursor cursor = db.rawQuery(sql, null);
        if (!cursor.moveToFirst()) {
            db.insert(table, null, values);
            return true;
        }
        return false;
    }

}
