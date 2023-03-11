package com.miraclegarden.payrobot.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.miraclegarden.library.app.MiracleGardenActivity;
import com.miraclegarden.payrobot.databinding.ActivitySetBinding;

import java.util.Objects;

public class SetActivity extends MiracleGardenActivity<ActivitySetBinding> {
    private SharedPreferences config;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        config = getSharedPreferences("config", MODE_PRIVATE);
        setTitle("设置");
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        String url = config.getString("url", "1");
        String ID = config.getString("ID", "1");
        if (!ID.equals("1")) {
            binding.ID.setText(ID);
        }
        if (!url.equals("1")) {
            binding.url.setText(url);
        }
        initView();
    }

    private void initView() {
        binding.button.setOnClickListener(v -> {
            SharedPreferences.Editor edit = config.edit();
            edit.putString("url", binding.url.getText().toString());
            edit.putString("ID", binding.ID.getText().toString());
            edit.putLong("time", System.currentTimeMillis());
            edit.apply();
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
        });
    }

    //activity类中的方法
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
