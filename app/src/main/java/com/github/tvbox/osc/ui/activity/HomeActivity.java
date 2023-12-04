package com.github.tvbox.osc.ui.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Handler;
import android.widget.Toast;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;

import com.github.tvbox.osc.util.AppManager;
import com.github.tvbox.osc.util.LOG;
import org.greenrobot.eventbus.EventBus;

import java.text.SimpleDateFormat;
import java.util.Date;


public class HomeActivity extends BaseActivity {

    private Handler mHandler = new Handler();

    private int retryCount=0;
    private Runnable mRunnable = new Runnable() {
        @SuppressLint({"DefaultLocale", "SetTextI18n"})
        @Override
        public void run() {
            Date date = new Date();
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy/MM/dd EE HH:mm");
//            tvDate.setText(timeFormat.format(date));
            mHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_home;
    }

    boolean useCacheConfig = false;

    @Override
    protected void init() {
        initData();
    }

    private void postInitData(){
        mHandler.postDelayed(()-> {
            jumpActivity(LivePlayActivity.class);
        },10);
    }


    private boolean dataInitOk = false;

    private void retryIt(){
        retryCount++;
        if(retryCount%11==0){
            Toast.makeText(mContext, "Live 正在重试..."+(retryCount/11), Toast.LENGTH_SHORT).show();
            mHandler.postDelayed(()->{
                // wait 2sec
                initData();
            },2000);
        }else {
            initData();
        }
    }

    private void initData() {
//        SourceBean home = ApiConfig.get().getHomeSourceBean();
        if (dataInitOk) {
            showLoading();
//            sourceViewModel.getSort(ApiConfig.get().getHomeSourceBean().getKey());
            if (hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                LOG.e("有");
            } else {
                LOG.e("无");
            }

            postInitData();
            return;
        }
        showLoading();
        ApiConfig.get().loadConfig(useCacheConfig, new ApiConfig.LoadConfigCallback() {

            @Override
            public void retry() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        initData();
                    }
                });
            }

            @Override
            public void success() {
                dataInitOk = true;
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        initData();
                    }
                }, 50);
            }

            @Override
            public void error(String msg) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        retryIt();
                    }
                });
            }
        }, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHandler.post(mRunnable);
    }


    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacksAndMessages(null);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        AppManager.getInstance().appExit(0);
    }

};
