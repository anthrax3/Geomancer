package tacoball.com.geomancer;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class MapUpdaterFragment extends Fragment {

    private static String TAG = "MapUpdaterFragment";

    TextView    mTxvAction;
    ProgressBar mPgbAction;
    Button      mBtnRepair;

    Handler mHandler;
    FileUpdateManager fum;
    int fileIndex = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup layout = (ViewGroup)inflater.inflate(R.layout.fragment_updater, container, false);
        mTxvAction = (TextView)layout.findViewById(R.id.txvAction);
        mTxvAction.setText("檢查網路連線 ...");
        mPgbAction = (ProgressBar)layout.findViewById(R.id.pgbAction);
        mPgbAction.setProgress(0);
        mBtnRepair = (Button)layout.findViewById(R.id.btnRepair);
        mBtnRepair.setVisibility(View.INVISIBLE);
        mBtnRepair.setOnClickListener(repairListener);
        mHandler = new Handler();

        // 檢查網路連線
        boolean hasNetwork = false;
        ConnectivityManager connMgr = (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        for (NetworkInfo ni : connMgr.getAllNetworkInfo()) {
            if (ni.isConnected()) {
                hasNetwork = true;
                break;
            }
        }

        if (hasNetwork) {
            try {
                // 啟動檔案更新循環
                String fileURL = MainUtils.getRemoteURL(fileIndex);
                File   saveTo  = MainUtils.getSavePath(getActivity(), fileIndex);
                fum = new FileUpdateManager(saveTo);
                fum.setListener(listener);
                fum.checkVersion(fileURL);
            } catch(IOException ex) {
                mTxvAction.setText("無法存取檔案，是否儲存空間已用盡？");
            }
        } else {
            mTxvAction.setText("需要網路連線更新地圖，請打開網路後重試");
        }

        return layout;
    }

    @Override
    public void onDestroy() {
        fum.cancel();
        super.onDestroy();
    }

    private void setProgress(final String action, final int progress) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String msg = String.format(Locale.getDefault(), "%s %d%%", action, progress);
                mTxvAction.setText(msg);
                mPgbAction.setProgress(progress);
            }
        });
    }

    private void setErrorMessage(final String msg) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mTxvAction.setText(msg);
                mBtnRepair.setVisibility(View.VISIBLE);
            }
        });
    }

    private void gotoMap() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Activity activity = getActivity();
                Intent restartIntent = new Intent(activity, MainActivity.class);
                activity.finish();
                activity.startActivity(restartIntent);
            }
        });
    }

    FileUpdateManager.ProgressListener listener = new FileUpdateManager.ProgressListener() {

        private void checkNext() {
            fileIndex++;
            if (fileIndex<MainUtils.REQUIRED_FILES.length) {
                try {
                    String fileURL = MainUtils.getRemoteURL(fileIndex);
                    File saveTo = MainUtils.getSavePath(getActivity(), fileIndex);
                    fum.setSaveTo(saveTo);
                    fum.checkVersion(fileURL);
                } catch(IOException ex) {
                    // TODO
                }
            } else {
                gotoMap();
            }
        }

        @Override
        public void onCheckVersion(boolean hasNew, long mtime) {
            if (hasNew) {
                String fileURL = MainUtils.getRemoteURL(fileIndex);
                fum.update(fileURL);
            } else {
                checkNext();
            }
        }

        @Override
        public void onNewProgress(int step, int percent) {
            setProgress(getStepName(step, true), percent);
        }

        @Override
        public void onComplete() {
            checkNext();
        }

        @Override
        public void onCancel() {
            Log.i(TAG, "已取消更新");
        }

        @Override
        public void onError(int step, String reason) {
            String msg = String.format(Locale.getDefault(), "%s階段發生錯誤", getStepName(step, false));
            setErrorMessage(msg);
        }

        private String getStepName(int step, boolean withTarget) {
            String[] targets = {"地圖檔", "凶宅資料庫", "血汗勞工資料庫"};

            String stepname = "準備下載";
            switch (step) {
                case FileUpdateManager.STEP_DOWNLOAD:
                    stepname = "正在下載";
                    break;
                case FileUpdateManager.STEP_EXTRACT:
                    stepname = "正在解壓縮";
                    break;
                case FileUpdateManager.STEP_REPAIR:
                    stepname = "正在修復";
            }

            if (withTarget) {
                return String.format(Locale.getDefault(), "%s%s", stepname, targets[fileIndex]);
            }

            return stepname;
        }

    };

    View.OnClickListener repairListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            mBtnRepair.setVisibility(View.INVISIBLE);
            String fileURL = MainUtils.getRemoteURL(fileIndex);
            fum.repair(fileURL);
        }

    };

}
