package com.lisnr.wificonnect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.lisnr.core.LISNRAndroidService;
import com.lisnr.core.LisnrDataTone;
import com.lisnr.sdk.SDKState;
import com.lisnr.sdk.exceptions.InvalidTonePayloadException;
import com.lisnr.sdk.exceptions.InvalidToneProfileException;

public class TransmitActivity extends AppCompatActivity {

    private static final String TAG = TransmitActivity.class.getCanonicalName();
    private static final String PREF_KEY_SSID = "ssid";
    private static final String PREF_KEY_PASSPHRASE = "passphrase";

    private EditText mSsidField;
    private EditText mPassphraseField;
    private Button mTransmitButton;
    private boolean mIsTransmitting;

    private BroadcastReceiver mStateChangeBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SDKState state = (SDKState) intent.getSerializableExtra(LISNRAndroidService.EXTRA_SDK_STATE);
            if (state == SDKState.Idle) {
                mTransmitButton.setText(R.string.transmit_button_idle);
                mIsTransmitting = false;
                mTransmitButton.setEnabled(WifiPayload.isValidSsid(mSsidField.getText())
                        && WifiPayload.isValidPassphrase(mPassphraseField.getText()));
            } else if (state == SDKState.BroadcastingTone) {
                mIsTransmitting = true;
                mTransmitButton.setText(R.string.transmit_button_transmitting);
                mTransmitButton.setEnabled(false);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transmit);

        mSsidField = findViewById(R.id.ssid_field);
        mPassphraseField = findViewById(R.id.passphrase_field);
        mTransmitButton = findViewById(R.id.transmit_button);
        mIsTransmitting = false;

        SharedPreferences sharedPrefs = getPreferences(MODE_PRIVATE);
        if (sharedPrefs.contains(PREF_KEY_SSID)) {
            mSsidField.setText(sharedPrefs.getString(PREF_KEY_SSID, ""));
        }
        if (sharedPrefs.contains(PREF_KEY_PASSPHRASE)) {
            mPassphraseField.setText(sharedPrefs.getString(PREF_KEY_PASSPHRASE, ""));
        }


        mSsidField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                mTransmitButton.setEnabled(WifiPayload.isValidSsid(mSsidField.getText())
                        && WifiPayload.isValidPassphrase(mPassphraseField.getText()) && !mIsTransmitting);
            }
        });
        mPassphraseField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                mTransmitButton.setEnabled(WifiPayload.isValidSsid(mSsidField.getText())
                        && WifiPayload.isValidPassphrase(mPassphraseField.getText()) && !mIsTransmitting);
            }
        });
        mTransmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WifiPayload wifiPayload = WifiPayload.create(
                        mSsidField.getText().toString(),mPassphraseField.getText().toString());
                if (wifiPayload != null) {
                    broadcastNetworkInfo(wifiPayload);
                }
            }
        });
        mTransmitButton.setEnabled(WifiPayload.isValidSsid(mSsidField.getText()) &&
                WifiPayload.isValidPassphrase(mPassphraseField.getText()) && !mIsTransmitting);
    }

    private void broadcastNetworkInfo(WifiPayload wifiPayload) {
        LisnrDataTone dataTone;
        try {
            dataTone = new LisnrDataTone(wifiPayload.getPayload(), Config.PROFILE);

            Intent startBroadcasting = new Intent(getApplicationContext(), LISNRAndroidService.class);
            startBroadcasting.setAction(LISNRAndroidService.ACTION_START_BROADCASTING);
            startBroadcasting.putExtra(LISNRAndroidService.EXTRA_JWT_APP_TOKEN, Config.APP_JWT);
            startBroadcasting.putExtra(LISNRAndroidService.EXTRA_TONE_OBJECT, dataTone);
            startService(startBroadcasting);
        } catch (InvalidTonePayloadException | InvalidToneProfileException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        editor.putString(PREF_KEY_SSID, mSsidField.getText().toString());
        editor.putString(PREF_KEY_PASSPHRASE, mPassphraseField.getText().toString());
        editor.apply();

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mStateChangeBroadcastReceiver, new IntentFilter(LISNRAndroidService.ACTION_SDK_STATE_CHANGED));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mStateChangeBroadcastReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.transmit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.open_client) {
            finish();
            startActivity(new Intent(this, ClientActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }
}
