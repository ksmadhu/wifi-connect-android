package com.lisnr.wificonnect;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.lisnr.core.LISNRAndroidService;
import com.lisnr.core.LisnrDataTone;
import com.lisnr.core.LisnrTone;
import com.lisnr.sdk.SDKState;

public class ClientActivity extends AppCompatActivity {
    static final int PERMISSIONS_REQUEST_MICROPHONE_ACCESS = 0;
    private TextView mMessageTextView = null;
    private TextView mWifiMessageTextView = null;
    private View mSpinner = null;
    private String mSsid = null;

    private BroadcastReceiver mToneHeardReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LisnrTone tone = intent.getParcelableExtra(LISNRAndroidService.EXTRA_TONE_OBJECT);
            if (tone instanceof LisnrDataTone) {
                WifiPayload wifiPayload = WifiPayload.create(((LisnrDataTone) tone).getData());
                if (wifiPayload != null) {
                    // do the connection
                    WifiConfiguration wifiConfiguration = new WifiConfiguration();
                    wifiConfiguration.SSID = String.format("\"%s\"", wifiPayload.getSsid());
                    if (wifiPayload.getPassphrase() != null) {
                        wifiConfiguration.preSharedKey = String.format("\"%s\"", wifiPayload.getPassphrase());
                    } else {
                        wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    }

                    setUiConnecting(wifiPayload.getSsid());

                    WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                    int netId = wifiManager.addNetwork(wifiConfiguration);
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(netId, true);
                    wifiManager.reconnect();
                }
            }
        }
    };

    private BroadcastReceiver mStateChangeBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mMessageTextView != null) {
                SDKState state = (SDKState) intent.getSerializableExtra(LISNRAndroidService.EXTRA_SDK_STATE);
                if (state == SDKState.ListeningForTone) {
                    mMessageTextView.setText(R.string.listening);
                } else {
                    mMessageTextView.setText(R.string.waiting_to_listen);
                }
            }
        }
    };

    private BroadcastReceiver mWifiStatusChangeBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }

            final String action = intent.getAction();
            if (!action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION) || mSsid == null) {
                return;
            }

            // a network change is happening and we haven't yet connected...
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo.getSSID().equals(String.format("\"%s\"", mSsid)) && networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                    // connection completed
                    setUiConnected();
                }
            }
        }
    };

    private void setUiConnecting(String ssid) {
        mSsid = ssid;
        mSpinner.setVisibility(View.VISIBLE);
        mWifiMessageTextView.setText(getString(R.string.connecting_to, ssid));
    }

    private void setUiConnected() {
        mSsid = null;
        mSpinner.setVisibility(View.INVISIBLE);
        mWifiMessageTextView.setText(R.string.connected);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        mMessageTextView = findViewById(R.id.message);
        mWifiMessageTextView = findViewById(R.id.wifi_message);
        mSpinner = findViewById(R.id.spinner);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_MICROPHONE_ACCESS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent service = new Intent(ClientActivity.this, LISNRAndroidService.class);
                service.setAction(LISNRAndroidService.ACTION_START_LISTENING);
                service.putExtra(LISNRAndroidService.EXTRA_JWT_APP_TOKEN, Config.APP_JWT);
                startService(service);
            } else {
                Log.w(this.getClass().getSimpleName(), "Microphone permissions denied. Please enable them in app settings");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.i(this.getClass().getSimpleName(), "Microphone permissions not granted");
            /* Code to handle getting microphone permissions on Android 6.0+ */
            if (Build.VERSION.SDK_INT >= 23) {
                if(ActivityCompat.shouldShowRequestPermissionRationale(ClientActivity.this, Manifest.permission.RECORD_AUDIO)) {
                    new android.app.AlertDialog.Builder(ClientActivity.this)
                            .setTitle("Requesting Microphone Permissions")
                            .setMessage("In a moment " + getApplication().getString(getApplication().getApplicationInfo().labelRes) + " will request permission to access your microphone. Microphone access is used only to listen for high-frequency data tones that are used to unlock extra content and improve your experience while using the app. Your data is only processed locally on this device, and never saved on or uploaded to a server")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions(ClientActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_MICROPHONE_ACCESS);
                                }
                            }).show();
                } else {
                    ActivityCompat.requestPermissions(ClientActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_MICROPHONE_ACCESS);
                }
            }
        } else {
            Intent service = new Intent(ClientActivity.this, LISNRAndroidService.class);
            service.setAction(LISNRAndroidService.ACTION_START_LISTENING);
            service.putExtra(LISNRAndroidService.EXTRA_JWT_APP_TOKEN, Config.APP_JWT);
            startService(service);
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(mStateChangeBroadcastReceiver, new IntentFilter(LISNRAndroidService.ACTION_SDK_STATE_CHANGED));
        LocalBroadcastManager.getInstance(this).registerReceiver(mToneHeardReceiver, new IntentFilter(LISNRAndroidService.ACTION_TONE_HEARD));
        registerReceiver(mWifiStatusChangeBroadcastReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();

        mMessageTextView.setText(R.string.waiting_to_listen);

        Intent service = new Intent(ClientActivity.this, LISNRAndroidService.class);
        service.setAction(LISNRAndroidService.ACTION_STOP_LISTENING);
        service.putExtra(LISNRAndroidService.EXTRA_JWT_APP_TOKEN, Config.APP_JWT);
        startService(service);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mStateChangeBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mToneHeardReceiver);
        unregisterReceiver(mWifiStatusChangeBroadcastReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.client, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.open_transmit) {
            finish();
            startActivity(new Intent(this, TransmitActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }
}
