package com.lisnr.wificonnect;

public class WifiPayload {
    private static final int SSID_MAX_LEN_BYTES = 64;
    private static final int PASSPHRASE_MAX_LEN_BYTES = 64;

    public static WifiPayload create(String ssid, String passphrase) {
        if (!isValidSsid(ssid) || !isValidPassphrase(passphrase)) {
            return null;
        }

        byte[] ssidBytes = getSsidBytesFromText(ssid);
        byte[] passphraseBytes = getPassphraseBytesFromText(passphrase);
        byte[] payload = new byte[3 + ssidBytes.length + passphraseBytes.length];
        payload[0] = 'w';
        payload[1] = 'i';
        payload[2] = (byte) ssidBytes.length;
        System.arraycopy(ssidBytes, 0, payload, 3, ssidBytes.length);
        System.arraycopy(passphraseBytes, 0, payload, 3 + ssidBytes.length, passphraseBytes.length);
        return new WifiPayload(ssid, passphrase, payload);
    }

    public static WifiPayload create(byte[] payload) {
        // validate
        if (payload.length < 3 || payload[0] != 'w' || payload[1] != 'i') {
            return null;
        }

        int ssidLength = payload[2];
        if (ssidLength <= 0 || ssidLength >= SSID_MAX_LEN_BYTES) {
            return null;
        }

        int passphraseLength = payload.length - 3 - ssidLength;
        if (passphraseLength < 0 || passphraseLength >= PASSPHRASE_MAX_LEN_BYTES) {
            return null;
        }

        if (payload.length != ssidLength + passphraseLength + 3) {
            return null;
        }

        // read ssid and passphrase
        String ssid = new String(payload, 3, ssidLength);
        String passphrase = null;
        if (passphraseLength > 0) {
            passphrase = new String(payload, 3 + ssidLength, passphraseLength);
        }

        return new WifiPayload(ssid, passphrase, payload.clone());
    }

    private static byte[] getSsidBytesFromText(CharSequence text) {
        // just validate length in bytes
        byte[] result = text.toString().getBytes();
        if (result.length > SSID_MAX_LEN_BYTES) {
            result = null;
        }
        return result;
    }

    public static boolean isValidSsid(CharSequence text) {
        byte[] ssid = getSsidBytesFromText(text);
        return ssid != null;
    }

    private static byte[] getPassphraseBytesFromText(CharSequence text) {
        byte[] result = text.toString().getBytes();
        if (result.length > PASSPHRASE_MAX_LEN_BYTES) {
            result = null;
        }
        return result;
    }

    public static boolean isValidPassphrase(CharSequence text) {
        byte[] passphrase = getPassphraseBytesFromText(text);
        return passphrase != null;
    }

    private String mSsid;
    private String mPassphrase;
    private byte[] mPayload;

    private WifiPayload(String ssid, String passphrase, byte[] payload) {
        mSsid = ssid;
        mPassphrase = passphrase;
        mPayload = payload;
    }

    public String getSsid() {
        return mSsid;
    }

    public String getPassphrase() {
        return mPassphrase;
    }

    public byte[] getPayload() {
        return mPayload;
    }
}
