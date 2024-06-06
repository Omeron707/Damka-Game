package com.example.damka;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.widget.Toast;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;

public class UserCredentialsSaver {
    private static final String PREF_NAME = "userCredentials";
    public static final String USER_MAIL = "userMail";
    public static final String USER_PASSWORD = "userPassword";
    private static final String KEY_ALIAS = "MyKeyAlias";

    private static final String ALGORITHM = KeyProperties.KEY_ALGORITHM_AES;
    private static final String TRANSFORMATION = ALGORITHM + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7;

    public static void saveData(Context context, String key, String value) {
        try {
            Key secretKey = getKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] iv = cipher.getIV();
            byte[] encryptedData = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            String encryptedValue = Base64.encodeToString(encryptedData, Base64.DEFAULT);

            SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            sharedPreferences.edit().putString(key, encryptedValue).putString(key + "_iv", Base64.encodeToString(iv, Base64.DEFAULT)).apply();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "fail save Data", Toast.LENGTH_SHORT).show();
        }
    }

    public static String getData(Context context, String key) {
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String encryptedValue = sharedPreferences.getString(key, null);
            String ivString = sharedPreferences.getString(key + "_iv", null);
            if (encryptedValue != null && ivString != null) {
                byte[] encryptedData = Base64.decode(encryptedValue, Base64.DEFAULT);
                byte[] iv = Base64.decode(ivString, Base64.DEFAULT);

                Cipher cipher = Cipher.getInstance(TRANSFORMATION);
                Key secretKey = getKey();
                cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
                byte[] decryptedData = cipher.doFinal(encryptedData);

                return new String(decryptedData, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "fail get Data", Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    // Generate and store a symmetric key in the Android Keystore
    public static Key getKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            // Check if the key already exists
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                // Generate a new symmetric key
                KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
                KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                        .build();
                keyGenerator.init(keyGenParameterSpec);
                keyGenerator.generateKey();
            }

            // Retrieve the key
            return keyStore.getKey(KEY_ALIAS, null);
        } catch (KeyStoreException | NoSuchAlgorithmException | NoSuchProviderException |
                 InvalidAlgorithmParameterException | CertificateException | UnrecoverableKeyException | IOException e) {
            e.printStackTrace(); // Handle the exception appropriately, e.g., log it
        }
        return null;
    }


    public static void clearAllData(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            keyStore.deleteEntry(KEY_ALIAS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
