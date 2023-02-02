package com.miraclegarden.payrobot;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ScrollView;

import androidx.annotation.NonNull;

import com.miraclegarden.payrobot.activity.MainActivity;
import com.miraclegarden.payrobot.helper.MySqliteHelper;
import com.miraclegarden.payrobot.databinding.LayoutMainBinding;

import java.text.SimpleDateFormat;
import java.util.Date;

public class AimFloat extends Service {
    private static SharedPreferences config;

    public static boolean IsVisibility;
    //判断是否储存在本地
    private static boolean isStorage;
    public static final String LOG_TAG = new String(Base64.decode("emVjbGF5eA==", 0));
    //数据库储存
    private static MySqliteHelper mySqliteHelper;
    public static boolean floataim;
    private float downRawX;
    private float downRawY;
    public boolean isBtnChecked = false;
    private PowerManager.WakeLock mWakeLock;
    private static LayoutMainBinding binding;
    private WindowManager.LayoutParams paramsMainView;
    private WindowManager windowManagerMainView;

    private static final Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                setGONE();
                return;
            }
            String str = (String) msg.obj;
            @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
            String t = format.format(new Date());
            binding.text.append(t + "：" + str + "\n\n\r");
            binding.scrollable.post(() -> binding.scrollable.fullScroll(ScrollView.FOCUS_DOWN));
        }
    };
    private NotificationManager notificationManager;


    public static void setMessage(String mess) {
        try {
            if (binding == null) {
                return;
            }
            Message message = new Message();
            message.what = 0;
            message.obj = mess;
            AimFloat.handler.sendMessage(message);
        } catch (Exception e) {
            e.fillInStackTrace();
        }
    }

    public static void setMessage(int what) {
        Message message = new Message();
        message.what = what;
        AimFloat.handler.sendMessage(message);
    }


    public static void setGONE() {
        binding.image.setVisibility(View.VISIBLE);
        binding.line1.setVisibility(View.GONE);
    }

    @SuppressLint({"ClickableViewAccessibility", "ResourceType"})
    private void ShowMainView() {
        binding = LayoutMainBinding.inflate(LayoutInflater.from(this));
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-2, -2, getLayoutType(), getFlagsType(), -3);
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.x = 0;
        layoutParams.y = 0;
        this.paramsMainView = layoutParams;
        this.windowManagerMainView = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        this.windowManagerMainView.addView(binding.getRoot(), this.paramsMainView);
        this.isBtnChecked = false;
        binding.delete.setOnClickListener(v -> {
            binding.image.setVisibility(View.VISIBLE);
            binding.line1.setVisibility(View.GONE);
        });
        binding.Storage.setOnClickListener(v -> sendSimpleNotify("转账通知", "小花花向您尾号为9127的数字钱包转钱￥0.01"));
        binding.layoutIconControlView.setOnTouchListener(new View.OnTouchListener() {
            private float initialTouchX;
            private float initialTouchY;
            private int initialX;
            private int initialY;

            @Override
            public boolean onTouch(View param1View, MotionEvent param1MotionEvent) {
                //ImageView imageView;
                int i;
                int j;
                switch (param1MotionEvent.getAction()) {
                    default:
                        return false;
                    case 0:
                        this.initialX = paramsMainView.x;
                        this.initialY = paramsMainView.y;
                        this.initialTouchX = param1MotionEvent.getRawX();
                        this.initialTouchY = param1MotionEvent.getRawY();
                        downRawX = param1MotionEvent.getRawX();
                        downRawY = param1MotionEvent.getRawY();
                        return true;
                    case 1:
                        i = (int) (param1MotionEvent.getRawX() - this.initialTouchX);
                        j = (int) (param1MotionEvent.getRawY() - this.initialTouchY);
                        if (i < 10 && j < 10 && isViewFlesed()) {
                            //imageView = (ImageView) mainView.findViewById(R.id.imageview_aim);
                            if (!isBtnChecked) {
                                // AimMemory(45, true);
                                binding.image.setVisibility(View.GONE);
                                binding.line1.setVisibility(View.VISIBLE);
                                //imageView.setImageDrawable(getResources().getDrawable(R.drawable.a1));
                                isBtnChecked = true;
                                return true;
                            }
                        } else {
                            return true;
                        }
                        //AimMemory(45, false);
                        //imageView.setImageDrawable(getResources().getDrawable(R.drawable.a2));
                        binding.image.setVisibility(View.VISIBLE);
                        binding.line1.setVisibility(View.GONE);
                        isBtnChecked = false;
                        return true;
                    case 2:
                        break;
                }
                paramsMainView.x = this.initialX + (int) (param1MotionEvent.getRawX() - this.initialTouchX);
                paramsMainView.y = this.initialY + (int) (param1MotionEvent.getRawY() - this.initialTouchY);
                windowManagerMainView.updateViewLayout(binding.getRoot(), paramsMainView);
                return true;
            }
        });
    }


    private void createNotificationChannel() {
        //Android8.0(API26)以上需要调用下列方法，但低版本由于支持库旧，不支持调用
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("2958474980", "通知", importance);
            channel.setDescription("模拟推送");
            notificationManager = (NotificationManager)
                    getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        } else {
            notificationManager = (NotificationManager)
                    getSystemService(Context.NOTIFICATION_SERVICE);
        }

    }


    // 发送简单的通知消息（包括消息标题和消息内容）
    private void sendSimpleNotify(String title, String message) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.zfb);
        Log.d("MainActivity", String.valueOf((bitmap == null)));
        Notification.Builder builder = new Notification.Builder(this);
        Intent intent = new Intent(this, MainActivity.class);//将要跳转的界面
        //builder.setLargeIcon(bitmap);
        builder.setSmallIcon(R.drawable.zfb);
        builder.setContentText(message);//通知内容
        builder.setLargeIcon(bitmap);
        builder.setContentTitle(title);
        //利用PendingIntent来包装我们的intent对象,使其延迟跳转
        PendingIntent intentPend;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            intentPend = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            intentPend = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        }
        builder.setContentIntent(intentPend);
        Log.d("MainActivity", String.valueOf((notificationManager == null)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("2958474980");
        }
        notificationManager.notify(10086, builder.build());
    }


    private int getFlagsType() {
        return 8;
    }

    private static int getLayoutType() {
        return 2038;
    }

    private boolean isViewFlesed() {
        return !(binding != null && binding.image.getVisibility() != View.VISIBLE);
    }


    @Override
    public IBinder onBind(Intent paramIntent) {
        return (IBinder) null;
    }

    @SuppressLint("CutPasteId")
    @Override
    public void onCreate() {
        super.onCreate();
        config = getSharedPreferences("config", MODE_PRIVATE);
        mySqliteHelper = new MySqliteHelper(this);
        createNotificationChannel();
        ShowMainView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.isBtnChecked) this.isBtnChecked = false;
        if (this.mWakeLock != null) {
            this.mWakeLock.release();
            this.mWakeLock = null;
        }
        if (binding != null) this.windowManagerMainView.removeView(binding.getRoot());
    }

    @SuppressLint({"InvalidWakeLockTag", "WakelockTimeout"})
    @Override
    public int onStartCommand(Intent paramIntent, int paramInt1, int paramInt2) {
        if (this.mWakeLock == null) {
            this.mWakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE)).newWakeLock(1, LOG_TAG);
            this.mWakeLock.acquire();
        }
        return Service.START_NOT_STICKY;
    }
}
