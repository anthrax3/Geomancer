package tacoball.com.geomancer;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;

import java.util.Locale;

import io.fabric.sdk.android.Fabric;
import tacoball.com.geomancer.checkupdate.NetworkReceiver;
import tacoball.com.geomancer.view.TaiwanMapView;

/**
 * 前端程式進入點
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final boolean SIMULATE_OLD_MTIME = false;

    private MapViewFragment mMapFragment = new MapViewFragment();
    private Fragment mUpdateFragment = new UpdateToolFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());

        // 配置 Android 繪圖資源，必須在 inflate 之前完成
        AndroidGraphicFactory.createInstance(getApplication());
        setContentView(R.layout.activity_main);

        // 配置廣播接收器
        this.registerReceiver(receiver, MainUtils.buildFragmentSwitchIntentFilter());

        // 清理儲存空間
        MainUtils.cleanStorage(this);

        // 檢查是否殘留除錯設定，釋出前使用
        checkDebugParameters();

        // 先進入更新介面
        changeFragment(new UpdateToolFragment());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Override
    public void onBackPressed() {
        // 從設定頁返回主畫面要重新載入設定值
        // TODO: 目前的寫法會導致貢獻者頁面和授權頁面返回也重新載入
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            mMapFragment.reloadSettings();
        }

        super.onBackPressed();
    }

    // 地毯式檢查用到的除錯參數
    private void checkDebugParameters() {
        int cnt = 0;

        if (!NetworkReceiver.ENABLE_INTERVAL) {
            Log.w(TAG, getString(R.string.log_disable_interval_limit));
            cnt++;
        }

        if (!NetworkReceiver.ENABLE_PROBABILITY) {
            Log.w(TAG, getString(R.string.log_disable_probability_limit));
            cnt++;
        }

        if (MainUtils.MIRROR_NUM != 0) {
            Log.w(TAG, getString(R.string.log_use_debugging_mirror));
            cnt++;
        }

        if (TaiwanMapView.SEE_DEBUGGING_POINT) {
            Log.w(TAG, getString(R.string.log_see_debugging_point));
            cnt++;
        }

        if (SIMULATE_OLD_MTIME) {
            Log.w(TAG, getString(R.string.log_simulate_old_mtime));
            cnt++;
        }

        if (cnt > 0) {
            String pat = getString(R.string.pattern_enable_debugging);
            String msg = String.format(Locale.getDefault(), pat, cnt);
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
    }

    // 切換 Fragment
    private void changeFragment(Fragment nextFrag) {
        FragmentManager fm = getSupportFragmentManager();

        if (nextFrag == mMapFragment || nextFrag == mUpdateFragment) {
            // 放在堆疊底層
            fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            fm.beginTransaction()
                .replace(R.id.frag_container, nextFrag)
                .commit();
        } else {
            // 疊在 mMapFragment 上面
            fm.beginTransaction()
                .add(R.id.frag_container, nextFrag)
                .attach(nextFrag)
                .addToBackStack("detail")
                .commit();
        }
    }

    // 廣播接收器，處理使用者更新要求用
    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String msg = String.format(Locale.getDefault(), "Got broadcast intent action=%s", intent.getAction());
            Log.d(TAG, msg);

            if (intent.getAction().equals("MAIN")) {
                MainUtils.clearUpdateRequest(MainActivity.this);
                changeFragment(mMapFragment);
            }

            if (intent.getAction().equals("UPDATE")) {
                MainUtils.clearUpdateRequest(MainActivity.this);
                changeFragment(mUpdateFragment);
            }

            if (intent.getAction().equals("SETTINGS")) {
                Fragment f = new SettingsFragment();
                changeFragment(f);
            }

            if (intent.getAction().equals("CONTRIBUTORS")) {
                Fragment f = new SimpleFragment();
                Bundle args = new Bundle();
                args.putInt("LAYOUT_ID", R.layout.fragment_contributors);
                f.setArguments(args);
                changeFragment(f);
            }

            if (intent.getAction().equals("LICENSE")) {
                Fragment f = new SimpleFragment();
                Bundle args = new Bundle();
                args.putInt("LAYOUT_ID", R.layout.fragment_license);
                f.setArguments(args);
                changeFragment(f);
            }
        }

    };

}
