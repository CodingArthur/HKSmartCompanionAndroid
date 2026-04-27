package hk.edu.hku.cs7506.smartcompanion;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.navi.NaviSetting;
import com.amap.api.services.core.ServiceSettings;
import com.amap.apis.utils.core.api.AMapUtilCoreApi;

import hk.edu.hku.cs7506.smartcompanion.data.repository.AppRepository;

public class SmartCompanionApp extends Application {
    private static final String TAG = "SmartCompanionApp";

    @Override
    public void onCreate() {
        super.onCreate();
        String amapApiKey = resolveAmapKey();
        boolean useHttpProtocol = isLikelyEmulator();

        MapsInitializer.updatePrivacyShow(this, true, true);
        MapsInitializer.updatePrivacyAgree(this, true);
        MapsInitializer.setProtocol(useHttpProtocol ? MapsInitializer.HTTP : MapsInitializer.HTTPS);
        if (!TextUtils.isEmpty(amapApiKey)) {
            MapsInitializer.setApiKey(amapApiKey);
        }
        try {
            MapsInitializer.initialize(this);
        } catch (RemoteException exception) {
            Log.w(TAG, "Unable to pre-initialize AMap maps SDK", exception);
        }

        AMapLocationClient.updatePrivacyShow(this, true, true);
        AMapLocationClient.updatePrivacyAgree(this, true);
        if (!TextUtils.isEmpty(amapApiKey)) {
            AMapLocationClient.setApiKey(amapApiKey);
        }

        NaviSetting.updatePrivacyShow(this, true, true);
        NaviSetting.updatePrivacyAgree(this, true);
        AMapUtilCoreApi.setCollectInfoEnable(true);

        ServiceSettings.updatePrivacyShow(this, true, true);
        ServiceSettings.updatePrivacyAgree(this, true);
        ServiceSettings.getInstance().setProtocol(useHttpProtocol ? ServiceSettings.HTTP : ServiceSettings.HTTPS);
        if (!TextUtils.isEmpty(amapApiKey)) {
            ServiceSettings.getInstance().setApiKey(amapApiKey);
        }

        AppRepository.getInstance(this).prefetchOfficialDataInBackground();
    }

    private String resolveAmapKey() {
        try {
            ApplicationInfo applicationInfo = getPackageManager()
                    .getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            if (applicationInfo.metaData == null) {
                return "";
            }
            return applicationInfo.metaData.getString("com.amap.api.v2.apikey", "");
        } catch (PackageManager.NameNotFoundException ignored) {
            return "";
        }
    }

    private boolean isLikelyEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.contains("emulator")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("sdk_gphone")
                || "google_sdk".equals(Build.PRODUCT)
                || Build.HARDWARE.contains("ranchu");
    }
}
