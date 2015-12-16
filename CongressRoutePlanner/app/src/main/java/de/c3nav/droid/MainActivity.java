package de.c3nav.droid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import de.c3nav.droid.BuildConfig;
import de.c3nav.droid.R;

public class MainActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    private WifiReceiver wifiReceiver;
    private WebView webView;
    private MobileClient mobileClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final View pView = LayoutInflater.from(this).inflate(R.layout.progressbar, null);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(pView);
        getSupportActionBar().getCustomView().setVisibility(View.GONE);

        mobileClient = new MobileClient();

        webView = (WebView) findViewById(R.id.webView);

        webView.getSettings().setSupportZoom(false);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setUserAgentString("c3navClient/Android/" + BuildConfig.VERSION_CODE);
        webView.addJavascriptInterface(mobileClient, "mobileclient");

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView v, int progress) {
                if (progress < 100 && pView.getVisibility() == View.GONE) {
                    pView.setVisibility(View.VISIBLE);
                }
                if (progress == 100) {
                    pView.setVisibility(View.GONE);
                }
            }

        });

        webView.loadUrl(BuildConfig.WEB_URL);

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiReceiver = new WifiReceiver();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        wifiManager.startScan();
    }

    class MobileClient {
        private JSONArray nearbyStations;

        @JavascriptInterface
        public String getNearbyStations() { return this.nearbyStations.toString(); }

        public void setNearbyStations(JSONArray nearbyStations) {
            this.nearbyStations = nearbyStations;
        }
    }

    class WifiReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
            webView.post(new Runnable() {
                @Override
                public void run() {
                    List<ScanResult> wifiList = wifiManager.getScanResults();
                    JSONArray ja = new JSONArray();
                    for(ScanResult result : wifiList) {
                        JSONObject jo = new JSONObject();
                        try {
                            jo.put("bssid", result.BSSID);
                            jo.put("level", result.level);
                            ja.put(jo);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    mobileClient.setNearbyStations(ja);
                    webView.loadUrl("javascript:nearby_stations_available();");
                }
            });
        }
    }
}
