package com.github.tvbox.osc.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.IntEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.net.Uri;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.Epginfo;
import com.github.tvbox.osc.bean.LiveChannelGroup;
import com.github.tvbox.osc.bean.LiveChannelItem;

import com.github.tvbox.osc.bean.LiveEpgDate;
import com.github.tvbox.osc.bean.LivePlayerManager;

import com.github.tvbox.osc.player.controller.LiveController;

import com.github.tvbox.osc.ui.adapter.LiveChannelGroupAdapter;
import com.github.tvbox.osc.ui.adapter.LiveChannelItemAdapter;
import com.github.tvbox.osc.ui.adapter.LiveEpgDateAdapter;

import com.github.tvbox.osc.ui.dialog.LivePasswordDialog;
import com.github.tvbox.osc.ui.tv.widget.ViewObj;
import com.github.tvbox.osc.util.AppManager;
import com.github.tvbox.osc.util.EpgUtil;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.PlayerHelper;
import com.github.tvbox.osc.util.live.TxtSubscribe;
import com.github.tvbox.osc.util.urlhttp.CallBackUtil;
import com.github.tvbox.osc.util.urlhttp.UrlHttpUtil;
import com.google.gson.JsonArray;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.net.URLEncoder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;

import java.util.TimeZone;

import xyz.doikki.videoplayer.player.VideoView;


public class LivePlayActivity extends BaseActivity {
    public static Context context;
    private VideoView mVideoView;
    private TextView tvTime;
    private TextView tvNetSpeed;
    private LinearLayout tvLeftChannelListLayout;
    private TvRecyclerView mChannelGroupView;
    private TvRecyclerView mLiveChannelView;
    private LiveChannelGroupAdapter liveChannelGroupAdapter;
    private LiveChannelItemAdapter liveChannelItemAdapter;

    public static  int currentChannelGroupIndex = 0;
    private Handler mHandler = new Handler();

    private List<LiveChannelGroup> liveChannelGroupList = new ArrayList<>();
    private int currentLiveChannelIndex = -1;
    private int currentLiveChangeSourceTimes = 0;
    private LiveChannelItem currentLiveChannelItem = null;
    private LivePlayerManager livePlayerManager = new LivePlayerManager();
    private ArrayList<Integer> channelGroupPasswordConfirmed = new ArrayList<>();


    private static LiveChannelItem  channel_Name = null;
    private static Hashtable hsEpg = new Hashtable();

    RelativeLayout ll_epg;
    TextView tv_channelnum;
    TextView tip_chname;
    TextView tip_epg1;
    TextView tip_epg2;
    TextView tv_current_program_name;
    TextView tv_next_program_name;
    TextView tv_srcinfo;
    TextView tv_curepg_left;
    TextView tv_nextepg_left;

    private RelativeLayout ll_right_top_loading;
    private TextView tv_right_top_source_index;
    private TextView tv_right_top_channel_numx;
    private TextView tv_right_top_channel_name;
    private TextView tv_right_top_epg_name;

    public String epgStringAddress ="";

    private LiveEpgDateAdapter liveEpgDateAdapter;



    //laodao 7day replay
    public static SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd");

    public static String day = formatDate.format(new Date());
    public static Date nowday = new Date();


    //kenson
    private ImageView imgLiveIcon;
    private FrameLayout liveIconNullBg;
    private TextView liveIconNullText;

    private int videoWidth = 1920;
    private int videoHeight = 1080;

//    private SeekBar sBar;
    private View iv_playpause;
    private View iv_play;

    private int sourceTimeout = 5000;
    private long mExitTime = 0;

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_live_play;
    }

    @Override
    protected void init() {
        context = this;
        epgStringAddress = Hawk.get(HawkConfig.EPG_URL,"https://epg.112114.xyz/");
        if(epgStringAddress == null || epgStringAddress.length()<5){
            epgStringAddress = "http://epg.51zmt.top:8000/api/diyp/";
        }

        setLoadSir(findViewById(R.id.live_root));
        mVideoView = findViewById(R.id.mVideoView);

        tvLeftChannelListLayout = findViewById(R.id.tvLeftChannnelListLayout);
        mChannelGroupView = findViewById(R.id.mGroupGridView);
        mLiveChannelView = findViewById(R.id.mChannelGridView);

        tvTime = findViewById(R.id.tvTime); //右上角
        tvNetSpeed = findViewById(R.id.tvNetSpeed);

        //EPG  findViewById  by
        tip_chname = (TextView)  findViewById(R.id.tv_channel_bar_name);//底部名称
        tv_channelnum = (TextView) findViewById(R.id.tv_channel_bottom_number); //底部数字
        tip_epg1 = (TextView) findViewById(R.id.tv_current_program_time);//底部EPG当前节目信息
        tip_epg2 = (TextView) findViewById(R.id.tv_next_program_time);//底部EPG当下个节目信息
        tv_current_program_name=(TextView) findViewById(R.id.tv_current_program_name);
        tv_next_program_name=(TextView) findViewById(R.id.tv_next_program_name);

        tv_srcinfo = (TextView) findViewById(R.id.tv_source);//线路状态
        tv_curepg_left = (TextView) findViewById(R.id.tv_current_program);//当前节目
        tv_nextepg_left= (TextView) findViewById(R.id.tv_next_program);//下一节目
        ll_epg = (RelativeLayout) findViewById(R.id.ll_epg);
        tv_right_top_channel_numx = (TextView)findViewById(R.id.tv_right_top_channel_numx);
        tv_right_top_source_index = (TextView)findViewById(R.id.tv_right_top_source_index);
        tv_right_top_channel_name = (TextView)findViewById(R.id.tv_right_top_channel_name);
        tv_right_top_epg_name = (TextView)findViewById(R.id.tv_right_top_epg_name);


        Hawk.put(HawkConfig.NOW_DATE, formatDate.format(new Date()));
        day=formatDate.format(new Date());
        nowday=new Date();

        //EPG频道名称
        imgLiveIcon = findViewById(R.id.img_live_icon);
        liveIconNullBg = findViewById(R.id.live_icon_null_bg);
        liveIconNullText = findViewById(R.id.live_icon_null_text);
        imgLiveIcon.setVisibility(View.INVISIBLE);
        liveIconNullText.setVisibility(View.INVISIBLE);
        liveIconNullBg.setVisibility(View.INVISIBLE);

        iv_playpause = findViewById(R.id.iv_playpause);
        iv_play = findViewById(R.id.iv_play);

        ll_epg.setVisibility(View.VISIBLE);


        iv_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mVideoView.start();
                iv_play.setVisibility(View.INVISIBLE);

                iv_playpause.setBackground(ContextCompat.getDrawable(LivePlayActivity.context, R.drawable.vod_pause));
            }
        });

        iv_playpause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if(mVideoView.isPlaying()){
                    mVideoView.pause();

                    iv_play.setVisibility(View.VISIBLE);
                    iv_playpause.setBackground(ContextCompat.getDrawable(LivePlayActivity.context, R.drawable.icon_play));
                }else{
                    mVideoView.start();
                    iv_play.setVisibility(View.INVISIBLE);

                    iv_playpause.setBackground(ContextCompat.getDrawable(LivePlayActivity.context, R.drawable.vod_pause));
                }
            }

        });





        initEpg();

        initVideoView();
        initChannelGroupView();
        initLiveChannelView();

        initLiveChannelList();

        initTick();



    }

    //获取EPG并存储 // 百川epg  DIYP epg   51zmt epg ------- 自建EPG格式输出格式请参考 51zmt
    private List<Epginfo> epgdata = new ArrayList<>();

    private void showEpg(Date date, ArrayList<Epginfo> arrayList) {
        if (arrayList != null && arrayList.size() > 0) {
            epgdata = arrayList;

            int i = -1;
            int size = epgdata.size() - 1;
            while (size >= 0) {
                if (new Date().compareTo(((Epginfo) epgdata.get(size)).startdateTime) >= 0) {
                    break;
                }
                size--;
            }
            i = size;
        }
    }

    public void getEpg(Date date) {
        String channelName = channel_Name.getChannelName();
        SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd");
        timeFormat.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
        String[] epgInfo = EpgUtil.getEpgInfo(channelName);
        String epgTagName = channelName;
        updateChannelIcon(channelName, epgInfo == null ? null : epgInfo[0]);
        if (epgInfo != null && !epgInfo[1].isEmpty()) {
            epgTagName = epgInfo[1];
        }
        String finalChannelName = channelName;

        String url;
        if(epgStringAddress.contains("{name}") && epgStringAddress.contains("{date}")){
            url= epgStringAddress.replace("{name}",URLEncoder.encode(epgTagName)).replace("{date}",timeFormat.format(date));
        }else {
            url= epgStringAddress + "?ch="+ URLEncoder.encode(epgTagName) + "&date=" + timeFormat.format(date);
        }
        UrlHttpUtil.get(url, new CallBackUtil.CallBackString() {
            public void onFailure(int i, String str) {
                showEpg(date, new ArrayList());
                showBottomEpg(); // 失败了不执行.  会陷入循环...
            }

            public void onResponse(String paramString) {
                ArrayList arrayList = new ArrayList();
                Log.d("返回的EPG信息", paramString);

                try {
                    if (paramString.contains("epg_data")) {
                        final JSONArray jSONArray = new JSONObject(paramString).optJSONArray("epg_data");
                        if (jSONArray != null)
                            for (int b = 0; b < jSONArray.length(); b++) {
                                JSONObject jSONObject = jSONArray.getJSONObject(b);
                                Epginfo epgbcinfo = new Epginfo(date,jSONObject.optString("title"), date, jSONObject.optString("start"), jSONObject.optString("end"),b);
                                arrayList.add(epgbcinfo);
                                Log.d("EPG信息:", day +"  "+ jSONObject.optString("start") +" - "+jSONObject.optString("end") + "  " +jSONObject.optString("title"));
                            }
                    }
                } catch (JSONException jSONException) {
                    jSONException.printStackTrace();
                }
                showEpg(date, arrayList);
                String savedEpgKey = channelName + "_" + liveEpgDateAdapter.getItem(liveEpgDateAdapter.getSelectedIndex()).getDatePresented();
                if (!hsEpg.contains(savedEpgKey))
                    hsEpg.put(savedEpgKey, arrayList);
               showBottomEpg();
            }
        });
    }

    //显示底部EPG
    private void showBottomEpg() {
        if (channel_Name==null || channel_Name.getChannelName() == null){
            // isSHIYI ||
            // 时移SHIYI 
            return;
        }


        tip_chname.setText(channel_Name.getChannelName());
        tv_channelnum.setText("" + channel_Name.getChannelNum());
        tip_epg1.setText("无信息");
        tip_epg2.setText("");
        tv_current_program_name.setText("");
        tv_next_program_name.setText("");

        String savedEpgKey = channel_Name.getChannelName() + "_" 
            + liveEpgDateAdapter.getItem(liveEpgDateAdapter.getSelectedIndex()).getDatePresented();

        if (hsEpg.containsKey(savedEpgKey)) {
            String[] epgInfo = EpgUtil.getEpgInfo(channel_Name.getChannelName());
            // 图标
            updateChannelIcon(channel_Name.getChannelName(), epgInfo == null ? null : epgInfo[0]);
            ArrayList arrayList = (ArrayList) hsEpg.get(savedEpgKey);
            if (arrayList != null && arrayList.size() > 0) {
                int size = arrayList.size() - 1;
                while (size >= 0) {
                    if (new Date().compareTo(((Epginfo) arrayList.get(size)).startdateTime) >= 0) {
                        tip_epg1.setText(((Epginfo) arrayList.get(size)).start + "--" + ((Epginfo) arrayList.get(size)).end);
                        tv_current_program_name.setText(((Epginfo) arrayList.get(size)).title);
                        if (size != arrayList.size() - 1) {
                            tip_epg2.setText(((Epginfo) arrayList.get(size + 1)).start + "--" + ((Epginfo) arrayList.get(size)).end);
                            tv_next_program_name.setText(((Epginfo) arrayList.get(size + 1)).title);
                        }
                        break;
                    } else {
                        size--;
                    }
                }
            }
        }


        String str_source;
        // 频道序号 channel_Name.getSourceIndex()
        // 源数量:channel_Name.getSourceNum()
        if (channel_Name == null || channel_Name.getSourceNum() <= 0) {
            str_source="(1/1)";
        } else {
            str_source="("+(channel_Name.getSourceIndex() + 1) + "/" + channel_Name.getSourceNum()+")";
        }
        tv_srcinfo.setText("[线路" + str_source + "]");
        tv_right_top_source_index.setText(str_source);

        tv_right_top_channel_name.setText(channel_Name.getChannelName());
        tv_right_top_channel_numx.setText("" + channel_Name.getChannelNum());
        
        tv_right_top_epg_name.setText(channel_Name.getChannelName());

        int delay=6000;
        if(tip_epg1.getText().equals("无信息")){
            delay=1500;
        }

        mHandler.removeCallbacks(mHideLL_EPG);
        mHandler.postDelayed(mHideLL_EPG,delay);
        ll_epg.setVisibility(View.VISIBLE);

    }

    private Runnable mHideLL_EPG = new Runnable() {
        @Override
        public void run() {
            ll_epg.setVisibility(View.GONE);
        }
    };

    private void updateChannelIcon(String channelName, String logoUrl) {
        // 不用图标, 用数字.
         liveIconNullBg.setVisibility(View.VISIBLE);
         liveIconNullText.setVisibility(View.VISIBLE);
         imgLiveIcon.setVisibility(View.INVISIBLE);
         liveIconNullText.setText("" + channel_Name.getChannelNum());
    }


    private void doDoubleBackPress(){
        exit();
    }

    private void exit() {
        if (System.currentTimeMillis() - mExitTime < 2000) {
            EventBus.getDefault().unregister(this);
            AppManager.getInstance().appExit(0);
            finish();
            super.onBackPressed();
        } else {
            mExitTime = System.currentTimeMillis();
            Toast.makeText(mContext, "再按一次 返回键🔙 退出应用", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (tvLeftChannelListLayout.getVisibility() == View.VISIBLE) {
            mHandler.removeCallbacks(mHideChannelListRun);
            mHandler.post(mHideChannelListRun);
        }else {
            // 0 : vod
            // 1 : live 返回app主页
            // 2 : live and 直接退(不返回
            // 该模式下, 只有退出app, 不会返回到 点播界面.
            doDoubleBackPress();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        //遥控器键盘按键事件
        //
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
            if (keyCode == KeyEvent.KEYCODE_MENU) {
                // 不显示 配置.
                //  做点别的. TODO
                showBottomEpg();
            } else if (!isChannelListLayoutVisible()) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_UP:
                        // XXTODO 上下键 
                        playPrevious();
                        break;
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        playNext();
                        break;
//                    case KeyEvent.KEYCODE_DPAD_LEFT:
//                    case KeyEvent.KEYCODE_DPAD_RIGHT:
//                        break;
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                    case KeyEvent.KEYCODE_ENTER:
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        showBottomEpg();
                        showChannelList();
                        break;
                }
            }
        }
        
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mVideoView != null) {
            mVideoView.resume();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (mVideoView != null) {
            mVideoView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVideoView != null) {
            mVideoView.release();
            mVideoView = null;
        }
    }

    private void showChannelList() {
        if (tvLeftChannelListLayout.getVisibility() == View.INVISIBLE) {
            //重新载入上一次状态
            liveChannelItemAdapter.setNewData(getLiveChannels(currentChannelGroupIndex));
            if (currentLiveChannelIndex > -1)
                mLiveChannelView.scrollToPosition(currentLiveChannelIndex);
            mLiveChannelView.setSelection(currentLiveChannelIndex);
            mChannelGroupView.scrollToPosition(currentChannelGroupIndex);
            mChannelGroupView.setSelection(currentChannelGroupIndex);

            mHandler.removeCallbacks(mHideChannelListRun);
            mHandler.removeCallbacks(mFocusCurrentChannelAndShowChannelList);
            mHandler.post(mFocusCurrentChannelAndShowChannelList);
            mHandler.postDelayed(mHideChannelListRun,8000);
        }else{
            mHandler.removeCallbacks(mHideChannelListRun);
            mHandler.postDelayed(mHideChannelListRun,8000);
        }
    }

    private Runnable mFocusCurrentChannelAndShowChannelList = new Runnable() {
        @Override
        public void run() {
            if (mChannelGroupView.isScrolling() || mLiveChannelView.isScrolling() || mChannelGroupView.isComputingLayout() || mLiveChannelView.isComputingLayout()) {
                mHandler.postDelayed(this, 100);
            } else {
                // TODO
                liveChannelGroupAdapter.setSelectedGroupIndex(currentChannelGroupIndex);
                liveChannelItemAdapter.setSelectedChannelIndex(currentLiveChannelIndex);
                RecyclerView.ViewHolder holder = mLiveChannelView.findViewHolderForAdapterPosition(currentLiveChannelIndex);
                if (holder != null)
                    holder.itemView.requestFocus();
                tvLeftChannelListLayout.setVisibility(View.VISIBLE);
                ViewObj viewObj = new ViewObj(tvLeftChannelListLayout, (ViewGroup.MarginLayoutParams) tvLeftChannelListLayout.getLayoutParams());
                ObjectAnimator animator = ObjectAnimator.ofObject(viewObj, "marginLeft", new IntEvaluator(), -tvLeftChannelListLayout.getLayoutParams().width, 0);
                animator.setDuration(200);
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mHandler.removeCallbacks(mHideChannelListRun);
                        mHandler.postDelayed(mHideChannelListRun, 8000);
                    }
                });
                animator.start();
            }
        }
    };

    private Runnable mHideChannelListRun = new Runnable() {
        @Override
        public void run() {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) tvLeftChannelListLayout.getLayoutParams();
            if (tvLeftChannelListLayout.getVisibility() == View.VISIBLE) {
                ViewObj viewObj = new ViewObj(tvLeftChannelListLayout, params);
                ObjectAnimator animator = ObjectAnimator.ofObject(viewObj, "marginLeft", new IntEvaluator(), 0, -tvLeftChannelListLayout.getLayoutParams().width);
                animator.setDuration(200);
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        tvLeftChannelListLayout.setVisibility(View.INVISIBLE);
                    }
                });
                animator.start();
            }
        }
    };

    private boolean playChannel(int channelGroupIndex, int liveChannelIndex, boolean changeSource) {
        if ((channelGroupIndex == currentChannelGroupIndex && liveChannelIndex == currentLiveChannelIndex && !changeSource)
                || (changeSource && currentLiveChannelItem.getSourceNum() == 1)) {
            return true;
        }
        mVideoView.release();
        if (!changeSource) {
            currentChannelGroupIndex = channelGroupIndex;
            currentLiveChannelIndex = liveChannelIndex;
            currentLiveChannelItem = getLiveChannels(currentChannelGroupIndex).get(currentLiveChannelIndex);
            Hawk.put(HawkConfig.LIVE_CHANNEL, currentLiveChannelItem.getChannelName());
            livePlayerManager.getLiveChannelPlayer(mVideoView, currentLiveChannelItem.getChannelName());
        }

        channel_Name = currentLiveChannelItem;
//        isSHIYI=false; //时移
//        isBack = false; // 回放?
        //只有url包含pltv/8888才会显示时移功能
		if(currentLiveChannelItem.getUrl().indexOf("PLTV/8888") !=-1){
            currentLiveChannelItem.setinclude_back(true);
        }else {
            currentLiveChannelItem.setinclude_back(false);
        }
        // XXTODO 先不显示
        // showBottomEpg();

        getEpg(new Date());

        mVideoView.setUrl(currentLiveChannelItem.getUrl());

        mVideoView.start();
        return true;
    }


    private void playNext() {
        if (!isCurrentLiveChannelValid()) return;

        mHandler.removeCallbacks(playNext_);
        mHandler.postDelayed(playNext_,300);
    }

    private Runnable playNext_ = new Runnable(){
        @Override
        public void run() {
            if (!isCurrentLiveChannelValid()) return;

            sourceTimeout = 8000;
            Integer[] groupChannelIndex = getNextChannel(1);
            playChannel(groupChannelIndex[0], groupChannelIndex[1], false);
        }
    };



    private void playPrevious() {
        if (!isCurrentLiveChannelValid()) return;

        mHandler.removeCallbacks(playPrevious_);
        mHandler.postDelayed(playPrevious_,300);
    }

    private Runnable playPrevious_ = new Runnable() {
        @Override
        public void run() {
            if (!isCurrentLiveChannelValid()) return;

            sourceTimeout = 8000;
            Integer[] groupChannelIndex = getNextChannel(-1);
            playChannel(groupChannelIndex[0], groupChannelIndex[1], false);
        }
    };

    public void playPreSource() {
        if (!isCurrentLiveChannelValid()) return;

        sourceTimeout=5000;

        currentLiveChannelItem.preSource();
        playChannel(currentChannelGroupIndex, currentLiveChannelIndex, true);
    }

    public void playNextSource() {
        if (!isCurrentLiveChannelValid()) return;

        sourceTimeout=5000;

        currentLiveChannelItem.nextSource();
        playChannel(currentChannelGroupIndex, currentLiveChannelIndex, true);
    }

    private boolean isTopShow=false;
    private void initTick(){
        ll_right_top_loading=findViewById(R.id.ll_right_top_loading);
        mHandler.postDelayed(tick_,1500);
    }

    private Runnable tick_=new Runnable() {
        @Override
        public void run() {
            if(isTopShow){
                ll_right_top_loading.setVisibility(View.GONE);
                mHandler.postDelayed(this,1000);
            }else{
                ll_right_top_loading.setVisibility(View.VISIBLE);
                mHandler.postDelayed(this,11*1000);
            }
            isTopShow=!isTopShow;
        }
    };


    private void initEpg() {
        liveEpgDateAdapter = new LiveEpgDateAdapter();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        SimpleDateFormat datePresentFormat = new SimpleDateFormat("MM-dd");
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        for (int i = 0; i < 8; i++) {
            Date dateIns = calendar.getTime();
            LiveEpgDate epgDate = new LiveEpgDate();
            epgDate.setIndex(i);
            epgDate.setDatePresented(datePresentFormat.format(dateIns));
            epgDate.setDateParamVal(dateIns);
            liveEpgDateAdapter.addData(epgDate);
            calendar.add(Calendar.DAY_OF_MONTH, -1);
        }
        liveEpgDateAdapter.setSelectedIndex(1);
    }

    private void initVideoView() {
        LiveController controller = new LiveController(this);
        controller.setListener(new LiveController.LiveControlListener() {
            @Override
            public boolean singleTap() {
                showChannelList();
                return true;
            }

            @Override
            public void longPress() {
                // 本来 是显示设置.
                //  现在做点别的
                // XXTODO
            }

            @Override
            public void playStateChanged(int playState) {
                switch (playState) {
                    case VideoView.STATE_IDLE:
                    case VideoView.STATE_PAUSED:
                        break;
                    case VideoView.STATE_PREPARED:
                    case VideoView.STATE_BUFFERED:
                    case VideoView.STATE_PLAYING:
                        //播放成功, 增加时间, 防止 卡一下 就切源.
                        sourceTimeout=15000;

                        currentLiveChangeSourceTimes = 0;
                        mHandler.removeCallbacks(mConnectTimeoutChangeSourceRun);
//                        int w=mVideoView.getVideoSize()[0];
//                        int h=mVideoView.getVideoSize()[1];
////                        tvTime.setText(""+w+"x"+h);
//                        if(w>2000 || h>1100){
//                            sourceTimeout=8000;
//                            playNextSource();
//                        }
                        break;
                    case VideoView.STATE_ERROR:
                    case VideoView.STATE_PLAYBACK_COMPLETED:
                        mHandler.removeCallbacks(mConnectTimeoutChangeSourceRun);
                        mHandler.postDelayed(mConnectTimeoutChangeSourceRun, 2000);
                        break;
                    case VideoView.STATE_PREPARING:
                    case VideoView.STATE_BUFFERING:
                        // 超时换源(source)
                        // 初始|换频道: 8秒
                        // 换源: 5秒
                        // 播放成功:15 秒 // 防止卡了就切...
                        //   sourceTimeout=5000;
                        mHandler.removeCallbacks(mConnectTimeoutChangeSourceRun);
                        mHandler.postDelayed(mConnectTimeoutChangeSourceRun, sourceTimeout);
                        break;
                }
            }

            @Override
            public void changeSource(int direction) {
                if (direction > 0){
                    playNextSource();
                }else{
                    playPreSource();
                }
            }
        });

        controller.setCanChangePosition(false);
        controller.setEnableInNormal(true);
        controller.setGestureEnabled(true);
        controller.setDoubleTapTogglePlayEnabled(false);
        mVideoView.setVideoController(controller);
        mVideoView.setProgressManager(null);
    }

    private Runnable mConnectTimeoutChangeSourceRun = new Runnable() {
        @Override
        public void run() {
            // 自动:  只换源, 不换频道!!!!

            // currentLiveChangeSourceTimes++;
            // if (currentLiveChannelItem.getSourceNum() == currentLiveChangeSourceTimes) {
            //     currentLiveChangeSourceTimes = 0;
            //     Integer[] groupChannelIndex = getNextChannel(Hawk.get(HawkConfig.LIVE_CHANNEL_REVERSE, false) ? -1 : 1);
            //     playChannel(groupChannelIndex[0], groupChannelIndex[1], false);
            // } else {
            //     playNextSource();
            // }

            currentLiveChangeSourceTimes++;

            if (currentLiveChangeSourceTimes>=currentLiveChannelItem.getSourceNum()) {
                currentLiveChangeSourceTimes = 0;
                //XXTODO 把 换源 这个动作. 显示出来
            }

            //Toast.makeText(App.getInstance(), "源: "+(currentLiveChangeSourceTimes+1)+"/"+currentLiveChannelItem.getSourceNum(), Toast.LENGTH_SHORT).show();
            if(channel_Name!=null){
                String str_source="   ("+(channel_Name.getSourceIndex() + 1) + "/" + channel_Name.getSourceNum()+")";
                // 频道序号 channel_Name.getSourceNum()
                tv_right_top_channel_numx.setText("" + channel_Name.getChannelNum());
                tv_right_top_source_index.setText(str_source);
            }

            playNextSource();
        }
    };

    private void initChannelGroupView() {
        mChannelGroupView.setHasFixedSize(true);
        mChannelGroupView.setLayoutManager(new V7LinearLayoutManager(this.mContext, 1, false));

        liveChannelGroupAdapter = new LiveChannelGroupAdapter();
        mChannelGroupView.setAdapter(liveChannelGroupAdapter);
        mChannelGroupView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                mHandler.removeCallbacks(mHideChannelListRun);
                mHandler.postDelayed(mHideChannelListRun, 8000);
            }
        });

        //电视
        mChannelGroupView.setOnItemListener(new TvRecyclerView.OnItemListener() {
            @Override
            public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {
            }

            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                selectChannelGroup(position, true, -1);
            }

            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {
                if (isNeedInputPassword(position)) {
                    showPasswordDialog(position, -1);
                }
            }
        });

        //手机/模拟器
        liveChannelGroupAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                FastClickCheckUtil.check(view);
                selectChannelGroup(position, false, -1);
            }
        });
    }

    private void selectChannelGroup(int groupIndex, boolean focus, int liveChannelIndex) {
        if (focus) {
            liveChannelGroupAdapter.setFocusedGroupIndex(groupIndex);
            liveChannelItemAdapter.setFocusedChannelIndex(-1);
        }
        if ((groupIndex > -1 && groupIndex != liveChannelGroupAdapter.getSelectedGroupIndex()) || isNeedInputPassword(groupIndex)) {
            liveChannelGroupAdapter.setSelectedGroupIndex(groupIndex);
            if (isNeedInputPassword(groupIndex)) {
                showPasswordDialog(groupIndex, liveChannelIndex);
                return;
            }
            loadChannelGroupDataAndPlay(groupIndex, liveChannelIndex);
        }
        if (tvLeftChannelListLayout.getVisibility() == View.VISIBLE) {
            mHandler.removeCallbacks(mHideChannelListRun);
            mHandler.postDelayed(mHideChannelListRun, 8000);
        }
    }

    private void initLiveChannelView() {
        mLiveChannelView.setHasFixedSize(true);
        mLiveChannelView.setLayoutManager(new V7LinearLayoutManager(this.mContext, 1, false));

        liveChannelItemAdapter = new LiveChannelItemAdapter();
        mLiveChannelView.setAdapter(liveChannelItemAdapter);
        mLiveChannelView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                mHandler.removeCallbacks(mHideChannelListRun);
                mHandler.postDelayed(mHideChannelListRun, 8000);
            }
        });

        //电视
        mLiveChannelView.setOnItemListener(new TvRecyclerView.OnItemListener() {
            @Override
            public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {
            }

            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                if (position < 0) return;
                liveChannelGroupAdapter.setFocusedGroupIndex(-1);
                liveChannelItemAdapter.setFocusedChannelIndex(position);
                mHandler.removeCallbacks(mHideChannelListRun);
                mHandler.postDelayed(mHideChannelListRun, 8000);
            }

            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {
                clickLiveChannel(position);
            }
        });

        //手机/模拟器
        liveChannelItemAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                FastClickCheckUtil.check(view);
                clickLiveChannel(position);
            }
        });
    }

    private void clickLiveChannel(int position) {
        sourceTimeout=8000;
        liveChannelItemAdapter.setSelectedChannelIndex(position);
        playChannel(liveChannelGroupAdapter.getSelectedGroupIndex(), position, false);
        if (tvLeftChannelListLayout.getVisibility() == View.VISIBLE) {
            mHandler.removeCallbacks(mHideChannelListRun);
            mHandler.postDelayed(mHideChannelListRun, 8000);
        }
    }

    // 被 init 调用
    private void initLiveChannelList() {
        List<LiveChannelGroup> list = ApiConfig.get().getChannelGroupList();
        if (list.isEmpty()) {
            Toast.makeText(App.getInstance(), "频道列表为空", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (list.size() == 1 && list.get(0).getGroupName().startsWith("http://127.0.0.1")) {
            loadProxyLives(list.get(0).getGroupName());
        } else {
            liveChannelGroupList.clear();
            liveChannelGroupList.addAll(list);
            showSuccess();
            initLiveState();
        }
    }

    // 被 initLiveChannelList 调用
    public void loadProxyLives(String url) {
        try {
            Uri parsedUrl = Uri.parse(url);
            url = new String(Base64.decode(parsedUrl.getQueryParameter("ext"), Base64.DEFAULT | Base64.URL_SAFE | Base64.NO_WRAP), "UTF-8");
        } catch (Throwable th) {
            Toast.makeText(App.getInstance(), "频道列表为空", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        showLoading();
        OkGo.<String>get(url).execute(new AbsCallback<String>() {

            @Override
            public String convertResponse(okhttp3.Response response) throws Throwable {
                return response.body().string();
            }

            @Override
            public void onSuccess(Response<String> response) {
                JsonArray livesArray;
                LinkedHashMap<String, LinkedHashMap<String, ArrayList<String>>> linkedHashMap = new LinkedHashMap<>();
                TxtSubscribe.parse(linkedHashMap, response.body());
                livesArray = TxtSubscribe.live2JsonArray(linkedHashMap);

                ApiConfig.get().loadLives(livesArray);
                List<LiveChannelGroup> list = ApiConfig.get().getChannelGroupList();
                if (list.isEmpty()) {
                    Toast.makeText(App.getInstance(), "频道列表为空", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                liveChannelGroupList.clear();
                liveChannelGroupList.addAll(list);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        LivePlayActivity.this.showSuccess();
                        initLiveState();
                    }
                });
            }
        });
    }

    // 初始化, 被 initLiveChannelList || loadProxyLives 调用
    private void initLiveState() {
        String lastChannelName = Hawk.get(HawkConfig.LIVE_CHANNEL, "");

        int lastChannelGroupIndex = -1;
        int lastLiveChannelIndex = -1;
        for (LiveChannelGroup liveChannelGroup : liveChannelGroupList) {
            for (LiveChannelItem liveChannelItem : liveChannelGroup.getLiveChannels()) {
                if (liveChannelItem.getChannelName().equals(lastChannelName)) {
                    lastChannelGroupIndex = liveChannelGroup.getGroupIndex();
                    lastLiveChannelIndex = liveChannelItem.getChannelIndex();
                    break;
                }
            }
            if (lastChannelGroupIndex != -1) break;
        }
        if (lastChannelGroupIndex == -1) {
            lastChannelGroupIndex = getFirstNoPasswordChannelGroup();
            if (lastChannelGroupIndex == -1)
                lastChannelGroupIndex = 0;
            lastLiveChannelIndex = 0;
        }

        //播放器
        livePlayerManager.init(mVideoView);
        //时钟
        showTime();
        //网速
        showNetSpeed();
        //左侧 频道: 组,列表
        tvLeftChannelListLayout.setVisibility(View.INVISIBLE);
        liveChannelGroupAdapter.setNewData(liveChannelGroupList);

        selectChannelGroup(lastChannelGroupIndex, false, lastLiveChannelIndex);
    }

    private boolean isChannelListLayoutVisible() {
        return tvLeftChannelListLayout.getVisibility() == View.VISIBLE;
    }

    private void loadCurrentSourceList() {
        ArrayList<String> currentSourceNames = currentLiveChannelItem.getChannelSourceNames();
    }

    // time
    void showTime() {
        mHandler.post(mUpdateTimeRun);
        tvTime.setVisibility(View.VISIBLE);
    }

    private Runnable mUpdateTimeRun = new Runnable() {
        @Override
        public void run() {
            Date day=new Date();
            SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
            tvTime.setText(df.format(day));
            mHandler.postDelayed(this, 1000);
        }
    };

    //netspeed 网速
    private void showNetSpeed() {
        tv_right_top_source_index.setVisibility(View.VISIBLE);
        mHandler.post(mUpdateNetSpeedRun);
        tvNetSpeed.setVisibility(View.VISIBLE);
    }

    private Runnable mUpdateNetSpeedRun = new Runnable() {
        @Override
        public void run() {
            if (mVideoView == null) return;
            String speed = PlayerHelper.getDisplaySpeed(mVideoView.getTcpSpeed());
            tvNetSpeed.setText(speed);
            mHandler.postDelayed(this, 1000);
        }
    };

    private void showPasswordDialog(int groupIndex, int liveChannelIndex) {
        if (tvLeftChannelListLayout.getVisibility() == View.VISIBLE)
            mHandler.removeCallbacks(mHideChannelListRun);

        LivePasswordDialog dialog = new LivePasswordDialog(this);
        dialog.setOnListener(new LivePasswordDialog.OnListener() {
            @Override
            public void onChange(String password) {
                if (password.equals(liveChannelGroupList.get(groupIndex).getGroupPassword())) {
                    channelGroupPasswordConfirmed.add(groupIndex);
                    loadChannelGroupDataAndPlay(groupIndex, liveChannelIndex);
                } else {
                    Toast.makeText(App.getInstance(), "密码错误", Toast.LENGTH_SHORT).show();
                }

                if (tvLeftChannelListLayout.getVisibility() == View.VISIBLE)
                    mHandler.postDelayed(mHideChannelListRun, 8000);
            }

            @Override
            public void onCancel() {
                if (tvLeftChannelListLayout.getVisibility() == View.VISIBLE) {
                    int groupIndex = liveChannelGroupAdapter.getSelectedGroupIndex();
                    liveChannelItemAdapter.setNewData(getLiveChannels(groupIndex));
                }
            }
        });
        dialog.show();
    }

    private void loadChannelGroupDataAndPlay(int groupIndex, int liveChannelIndex) {
        // TODO here !!!!
        liveChannelItemAdapter.setNewData(getLiveChannels(groupIndex));
        if (groupIndex == currentChannelGroupIndex) {
            // 当前组
            if (currentLiveChannelIndex > -1){
                mLiveChannelView.scrollToPosition(currentLiveChannelIndex);
            }
            liveChannelItemAdapter.setSelectedChannelIndex(currentLiveChannelIndex);
        } else {
            // 非当前组
            mLiveChannelView.scrollToPosition(0);
            //-1 == 不通知.
            liveChannelItemAdapter.setSelectedChannelIndex(-1);
        }

        if (liveChannelIndex > -1) {
            clickLiveChannel(liveChannelIndex);
            mChannelGroupView.scrollToPosition(groupIndex);
            mLiveChannelView.scrollToPosition(liveChannelIndex);
            // playChannel: group channel isChangeSource
            sourceTimeout=8000;
            playChannel(groupIndex, liveChannelIndex, false);
        }
    }

    private boolean isNeedInputPassword(int groupIndex) {
        return !liveChannelGroupList.get(groupIndex).getGroupPassword().isEmpty()
                && !isPasswordConfirmed(groupIndex);
    }

    private boolean isPasswordConfirmed(int groupIndex) {
        for (Integer confirmedNum : channelGroupPasswordConfirmed) {
            if (confirmedNum == groupIndex)
                return true;
        }
        return false;
    }

    private ArrayList<LiveChannelItem> getLiveChannels(int groupIndex) {
        if (!isNeedInputPassword(groupIndex)) {
            return liveChannelGroupList.get(groupIndex).getLiveChannels();
        } else {
            return new ArrayList<>();
        }
    }

    private Integer[] getNextChannel(int direction) {
        int channelGroupIndex = currentChannelGroupIndex;
        int liveChannelIndex = currentLiveChannelIndex;

        //跨选分组模式下跳过加密频道分组（遥控器上下键换台/超时换源）
        if (direction > 0) {
            liveChannelIndex++;
            if (liveChannelIndex >= getLiveChannels(channelGroupIndex).size()) {
                liveChannelIndex = 0;
                if(true){ // 上下频道 跨区
                    do {
                        channelGroupIndex++;
                        if (channelGroupIndex >= liveChannelGroupList.size())
                            channelGroupIndex = 0;
                    } while (!liveChannelGroupList.get(channelGroupIndex).getGroupPassword().isEmpty() || channelGroupIndex == currentChannelGroupIndex);
                }
            }
        } else {
            liveChannelIndex--;
            if (liveChannelIndex < 0) {
                if(true){ // 上下频道 跨区
                    do {
                        channelGroupIndex--;
                        if (channelGroupIndex < 0)
                            channelGroupIndex = liveChannelGroupList.size() - 1;
                    } while (!liveChannelGroupList.get(channelGroupIndex).getGroupPassword().isEmpty() || channelGroupIndex == currentChannelGroupIndex);
                }
                liveChannelIndex = getLiveChannels(channelGroupIndex).size() - 1;
            }
        }

        Integer[] groupChannelIndex = new Integer[2];
        groupChannelIndex[0] = channelGroupIndex;
        groupChannelIndex[1] = liveChannelIndex;

        return groupChannelIndex;
    }

    private int getFirstNoPasswordChannelGroup() {
        for (LiveChannelGroup liveChannelGroup : liveChannelGroupList) {
            if (liveChannelGroup.getGroupPassword().isEmpty())
                return liveChannelGroup.getGroupIndex();
        }
        return -1;
    }

    private boolean isCurrentLiveChannelValid() {
        if (currentLiveChannelItem == null) {
            Toast.makeText(App.getInstance(), "请先选择频道", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }



}