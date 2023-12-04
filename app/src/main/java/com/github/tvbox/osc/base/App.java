package com.github.tvbox.osc.base;

import android.app.Activity;
import androidx.multidex.MultiDexApplication;

import com.github.tvbox.osc.callback.EmptyCallback;
import com.github.tvbox.osc.callback.LoadingCallback;
//import com.github.tvbox.osc.data.AppDataManager;
//import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.util.AppManager;
import com.github.tvbox.osc.util.EpgUtil;
import com.github.tvbox.osc.util.FileUtils;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.util.OkGoHelper;
import com.github.tvbox.osc.util.PlayerHelper;
import com.kingja.loadsir.core.LoadSir;
import com.orhanobut.hawk.Hawk;
//import com.undcover.freedom.pyramid.PythonLoader;
//import com.p2p.P2PClass;
//import com.whl.quickjs.android.QuickJSLoader;
import me.jessyan.autosize.AutoSizeConfig;
import me.jessyan.autosize.unit.Subunits;

/**
 * @author pj567
 * @date :2020/12/17
 * @description:
 */
public class App extends MultiDexApplication {
    private static App instance;

//    private static P2PClass p;
//    public static String burl;
    private static String dashData;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
//        RemoteConfig.Init(this);
		initParams();
        // OKGo
        OkGoHelper.init(); //台标获取
        EpgUtil.init();
//        // 初始化Web服务器
//        ControlManager.init(this);
        //初始化数据库
//        AppDataManager.init();
        LoadSir.beginBuilder()
                .addCallback(new EmptyCallback())
                .addCallback(new LoadingCallback())
                .commit();
        AutoSizeConfig.getInstance().setCustomFragment(true).getUnitsManager()
                .setSupportDP(false)
                .setSupportSP(false)
                .setSupportSubunits(Subunits.MM);
        PlayerHelper.init();
//        QuickJSLoader.init();
        // Add Pyramid support
//        PythonLoader.getInstance().setApplication(this);
		FileUtils.cleanPlayerCache();
    }

    private void initParams() {
        // Hawk
        Hawk.init(this).build();
//        Hawk.put(HawkConfig.DEBUG_OPEN, false);
        Hawk.put(HawkConfig.DEBUG_OPEN, true);
        // if (!Hawk.contains(HawkConfig.PLAY_TYPE)) {
        Hawk.put(HawkConfig.PLAY_TYPE, 1);
//        Hawk.put(HawkConfig.HOME_REC, 2);      				// Home Rec 0=豆瓣, 1=站点推荐, 2=历史
//        Hawk.put(HawkConfig.HOME_REC_STYLE, true);			// 0=首页单行(左右切换)，1=首页多行(上下切换)
//        Hawk.put(HawkConfig.SEARCH_VIEW, 1);    			// 0=文字搜索列表 1=缩略图搜索列表
        Hawk.put(HawkConfig.IJK_CODEC, "硬解码");    		// 硬解码
        // }


        // 放在 live 的逻辑里做了.  直接写死 true
        // LIVE_CHANNEL = "last_live_channel_name";
        // LIVE_CHANNEL_REVERSE = "live_channel_reverse";
        // LIVE_CROSS_GROUP = "live_cross_group";
        // LIVE_CONNECT_TIMEOUT = "live_connect_timeout";
        // LIVE_SHOW_NET_SPEED = "live_show_net_speed";
        // LIVE_SHOW_TIME = "live_show_time";

    }

    public static App getInstance() {
        return instance;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        //JsLoader.load();
    }



//    public static P2PClass getp2p() {
//        try {
//            if (p == null) {
//                p = new P2PClass(instance.getExternalCacheDir().getAbsolutePath());
//            }
//            return p;
//        } catch (Exception e) {
//            LOG.e(e.toString());
//            return null;
//        }
//    }

    public Activity getCurrentActivity() {
        return AppManager.getInstance().currentActivity();
    }

    public void setDashData(String data) {
        dashData = data;
    }
    public String getDashData() {
        return dashData;
    }
}
