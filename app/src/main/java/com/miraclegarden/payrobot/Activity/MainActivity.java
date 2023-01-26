package com.miraclegarden.payrobot.Activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

import com.miraclegarden.library.app.MiracleGardenActivity;
import com.miraclegarden.payrobot.AimFloat;
import com.miraclegarden.payrobot.MyAccessibilityService;
import com.miraclegarden.payrobot.R;
import com.miraclegarden.payrobot.databinding.ActivityMainBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends MiracleGardenActivity<ActivityMainBinding> {


    @SuppressLint("StaticFieldLeak")
    private static NestedScrollView scrollView;
    @SuppressLint("StaticFieldLeak")
    private static TextView textView;
    public static int REQUEST_OVERLAY_PERMISSION = 5469;

    private static final Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            String str = (String) msg.obj;
            @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
            String t = format.format(new Date());
            if (textView != null && scrollView != null) {
                textView.append(t + "：" + str + "\n\n\r");
                scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
            }
        }
    };

    public static void sendMessage(String str) {
        if (textView != null && scrollView != null) {
            if (!textView.getText().toString().contains(str)) {
                Message message = new Message();
                message.obj = str;
                MainActivity.handler.sendMessage(message);
            }
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionWindows();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        scrollView = binding.scrollable;
        textView = binding.text;
        initOnclick();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.set) {
            startActivity(new Intent(this, SetActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }


    private void initOnclick() {
        binding.button.setOnClickListener(v -> {
            if (AimFloat.IsVisibility) {
                stopFloating();
                AimFloat.IsVisibility = false;
            } else {
                startFloating();
                AimFloat.IsVisibility = true;
            }
        });
    }


    private void permissionWindows() {
        if (!Settings.canDrawOverlays(this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder((Context) this);
            builder.setMessage("This application requires window overlays access permission, please allow first.");
            builder.setPositiveButton("OK", (param1DialogInterface, param1Int) -> {
                Intent intent = new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION", Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, MainActivity.REQUEST_OVERLAY_PERMISSION);
            });
            builder.setCancelable(false);
            builder.show();
        }
    }

    private void startFloating() {
        startService(new Intent(this, AimFloat.class));
    }

    private void stopFloating() {
        stopService(new Intent(this, AimFloat.class));
        AimFloat.floataim = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean b = isServiceON(this, MyAccessibilityService.class.getName());
        if (!b) {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            MainActivity.sendMessage("开启成功");
        }
    }


    public static boolean isServiceON(Context context, String className) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo>
                runningServices = activityManager.getRunningServices(100);
        runningServices.size();
        for (int i = 0; i < runningServices.size(); i++) {
            ComponentName service = runningServices.get(i).service;
            if (service.getClassName().contains(className)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (MyAccessibilityService.timer != null) {
            MyAccessibilityService.timer.cancel();
            MyAccessibilityService.timer = null;
        }
    }
}
