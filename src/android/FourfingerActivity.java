package com.outsystemsenterprise.entel.PEMiEntel.cordova.plugin;

// Cordova-required packages
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import android.Manifest;
import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
//import android.support.annotation.NonNull;
//import android.support.v7.app.AlertDialog;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.content.ContextCompat;

import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.veridiumid.sdk.activities.DefaultVeridiumSDKModelFactory;
import com.veridiumid.sdk.defaultdata.VeridiumSDKDataInitializer;
import com.veridiumid.sdk.fourf.VeridiumSDKFourFInitializer;
import com.veridiumid.sdk.model.exception.SDKInitializationException;

import com.veridiumid.sdk.IVeridiumSDK;
import com.veridiumid.sdk.VeridiumSDK;
import com.veridiumid.sdk.analytics.Analytics;
import com.veridiumid.sdk.fourf.ExportConfig;
import com.veridiumid.sdk.fourf.FourFInterface;
import com.veridiumid.sdk.fourf.FourFIntegrationWrapper;
import com.veridiumid.sdk.model.biometrics.packaging.IBiometricFormats;
import com.veridiumid.sdk.model.biometrics.results.BiometricResultsParser;
import com.veridiumid.sdk.model.biometrics.results.handling.IBiometricResultsHandler;
import com.veridiumid.sdk.licensing.exception.LicenseException;
import com.veridiumid.sdk.support.base.VeridiumBaseActivity;
import com.veridiumid.sdk.support.help.ToastHelper;
import insolutions.veridium.insolutionsveridiumsdk.ISVeridiumTracker;

import android.content.res.Resources;

import android.widget.Button;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.loopj.android.http.Base64;


import com.veridiumid.sdk.fourf.FourFIntegrationWrapper;


public class FourfingerActivity extends Activity {

    private IVeridiumSDK mBiometricSDK;

    // operation strings
    private static final int REQUEST_APP_PREF = 168;
    private static final int REQUEST_EXPORT = 314;
    private static final int REQUEST_ENROL = 324;
    private static final int REQUEST_AUTH = 334;

    private String package_name;
    private Resources resources;

    private AlertDialog dialog_permissions;

    private static final String[] requiredPermissions = new String[] {
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET
    };

    private static int RIGHT_THUMB = 1;
    private static int RIGHT_INDEX = 2;
    private static int RIGHT_MIDDLE = 3;
    private static int RIGHT_RING = 4;
    private static int RIGHT_PINKY = 5;

    private static int LEFT_THUMB = 6;
    private static int LEFT_INDEX = 7;
    private static int LEFT_MIDDLE = 8;
    private static int LEFT_RING = 9;
    private static int LEFT_PINKY = 10;

    private int BestFingerRight;
    private int BestFingerLeft;
    private boolean Liveness;
    private int Type;

    private String TAG = "FourfingerActivity";
	private String TAG_TMP = "LogTemporal";
    
	private String realMinutia="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(getResourceId("layout/activity_fourfinger"));

        Log.d(TAG, "On create");

        BestFingerRight = getIntent().getIntExtra("RightFinger", 0);
        BestFingerLeft = getIntent().getIntExtra("LeftFinger", 0);
        Liveness = getIntent().getBooleanExtra("Liveness", false);
        Type = getIntent().getIntExtra("Type", 0);
		
		
		 try {
			Log.d(TAG,"Version 4F FourFIntegrationWrapper: "+ FourFIntegrationWrapper.version());
			Log.d(TAG,"Version 4F VeridiumSDK: "+ VeridiumSDK.getSingleton().getVersionName());
        } catch (Exception e) {

        }


        Log.d(TAG, "intent got. Left " + String.valueOf(BestFingerLeft) + "Right " + String.valueOf(BestFingerRight));


        preInitSDK();

        if (initSDK()) {

            configureExportSettings();

        }

        Analytics.init(this);

        if (Build.VERSION.SDK_INT > 22) {

            Log.d(TAG, "Requesting Permissions");
            checkPermissions(requiredPermissions);
        } else {

            abrir4Finger();
        }

    }

    @TargetApi(Build.VERSION_CODES.M)
    public void checkPermissions(@NonNull String... permissions) {

        List<String> ungranted = new ArrayList<String>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this,
                    permission) != PackageManager.PERMISSION_GRANTED) {
                ungranted.add(permission);
            }
        }

        if (ungranted.size() != 0) {
            ActivityCompat.requestPermissions(this,
                    ungranted.toArray(new String[0]),
                    REQUEST_APP_PREF);
        } else {
            Log.d(TAG, "hasPermissions");
            abrir4Finger();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        Log.d(TAG, "onRequestPermissionsResult");
        Boolean hasAllPermissions = true;
        for (int r : grantResults) {
            if (r != PackageManager.PERMISSION_GRANTED) {
                hasAllPermissions = false;
                showDeniedPermissionsDialog();
                break;
            }
        }

        if (hasAllPermissions) {

            Log.d(TAG, "hasAllPermissions");
            abrir4Finger();
        }

    }

    private void showDeniedPermissionsDialog() {

        Log.d(TAG, "showDeniedPermissionsDialog");
        if (dialog_permissions != null) {
            if (dialog_permissions.isShowing()) {
                return;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(
                "Esta aplicación requiere los permisos solicitados. Por favor, acéptelos a en la configuración de Android.")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, id) -> finish());
        dialog_permissions = builder.create();
        dialog_permissions.show();
    }

    private void abrir4Finger() {

        Log.d(TAG, "Abrir 4Finger!");

        if (mBiometricSDK != null) {

            Intent exportIntent = mBiometricSDK.export(FourFInterface.UID);
            startActivityForResult(exportIntent, REQUEST_EXPORT);

        } else {
            ToastHelper.showMessage(FourfingerActivity.this, "Licence is invalid!");
            Log.e(TAG, "IVeridiumSDK object not initialised");
        }

    }

    private int getResourceId(String typeAndName) {
        if (package_name == null)
            package_name = getApplication().getPackageName();
        if (resources == null)
            resources = getApplication().getResources();
        return resources.getIdentifier(typeAndName, null, package_name);
    }

    /*
     * Initialise the SDK and ensure the licence is valid.
     */
    private boolean initSDK() {

        try {
            mBiometricSDK = VeridiumSDK.getSingleton();
            return true;
        } catch (LicenseException e) {
            ToastHelper.showMessage(FourfingerActivity.this, "Licence is invalid!");
            e.printStackTrace();
        }
        return false;
    }

    /*
     * Configure Export settings using ExportConfig
     * Capture either hand, JSON container format, PNG with multiple scales,
     * liveness on
     */
    private void configureExportSettings() {

        ExportConfig.setFormat(IBiometricFormats.TemplateFormat.FORMAT_JSON);

        if (BestFingerRight == RIGHT_THUMB) {
            ExportConfig.setFingersToCapture(Arrays.asList(ExportConfig.FingerID.THUMB_RIGHT));

        } else if (BestFingerLeft == LEFT_THUMB) {
            ExportConfig.setFingersToCapture(Arrays.asList(ExportConfig.FingerID.THUMB_LEFT));

        } else if (BestFingerRight == RIGHT_INDEX || BestFingerRight == RIGHT_MIDDLE || BestFingerRight == RIGHT_RING
                || BestFingerRight == RIGHT_PINKY) {
            ExportConfig.setFingersToCapture(ExportConfig.ExportMode.FOUR_F_RIGHT_ENFORCED);

        } else if (BestFingerLeft == LEFT_INDEX || BestFingerLeft == LEFT_MIDDLE || BestFingerLeft == LEFT_RING
                || BestFingerLeft == LEFT_PINKY) {
            ExportConfig.setFingersToCapture(ExportConfig.ExportMode.FOUR_F_LEFT_ENFORCED);

        }

        /*
         * ExportConfig.setFormat(IBiometricFormats.TemplateFormat.FORMAT_JSON);
         * ExportConfig.setFixedPrintSize(512, 512);
         * ExportConfig.setWSQCompressionBitrate(0.5F);
         * ExportConfig.setPack_raw(false);
         * ExportConfig.setPack_wsq(true);
         * ExportConfig.setPack_png(false);
         * ExportConfig.setUseLiveness(true);
         * //ExportConfig.setLivenessFactor(99);
         * ExportConfig.setPackAuditImage(true);
         * ExportConfig.setPackDebugInfo(false);
         * ExportConfig.setUseNistType4(true);
         */

        // ExportConfig.setActiveLivenessBeta(false);
		
		Log.d(TAG_TMP, "Type:" + Type);


		if (Type==1)
		{
			Log.d(TAG_TMP, "Entro 1");
			ExportConfig.setFormat(IBiometricFormats.TemplateFormat.FORMAT_ISO_2_2005);
		}else{
			Log.d(TAG_TMP, "Entro 2");
			ExportConfig.setFormat(IBiometricFormats.TemplateFormat.FORMAT_JSON);
		}
        //ExportConfig.setActiveLivenessBeta(Liveness);
        ExportConfig.setLivenessFactor(99);
        ExportConfig.setPack_bmp(false);
        ExportConfig.setPack_png(false);
        ExportConfig.setPack_raw(false);
        ExportConfig.setPack_wsq(true);
        ExportConfig.setPackExtraScale(true);
        ExportConfig.setFixedPrintSize(512, 512);
        ExportConfig.setUseLiveness(true);
        ExportConfig.setCalculate_NFIQ(true);
        ExportConfig.setUseNistType4(false);
        //ExportConfig.setPackDebugInfo(true);
        ExportConfig.setPackAuditImage(true);
        ExportConfig.configureTimeout(true, 20, 3, false);
		ExportConfig.setPack_FMR_ANSI(true);
		ExportConfig.setPack_FMR_ISO(true);
    
        /*
         * ExportConfig.setWSQCompressRatio(ExportConfig.WSQCompressRatio.COMPRESS_10to1
         * );
         * ExportConfig.setFixedPrintSize(0,0);
         */
    }

    /*
     * A custom IBiometricResultsHandler, handles the resulting data from an
     * operation
     */
    IBiometricResultsHandler resultHandler = new IBiometricResultsHandler() {
        @Override
        public void handleSuccess(Map<String, byte[][]> results) {
			
			Log.d(TAG_TMP, "Escaneo exitoso");
			
            ToastHelper.showMessage(FourfingerActivity.this, "Escaneo Exitoso");
            // Handle exported templates here
            if (results != null && results.size() > 0) {
                ISVeridiumTracker.trackEvent(getApplicationContext(), "ENTEL AA", "Veridium", "Captura", "Success");
            } else {
                ISVeridiumTracker.trackEvent(getApplicationContext(), "ENTEL AA", "Veridium", "Captura", "Review");
            }

            byte[] template = null;
            for (Map.Entry<String, byte[][]> entry : results.entrySet()) {
                String bio_key = entry.getKey();
                byte[][] data = entry.getValue();

                String templateString;
				Log.d(TAG_TMP, "Escaneo exitoso");
                if (bio_key.equals(FourFInterface.UID)) {
                    Log.d(TAG_TMP, "template data is contained with the first element 1");
                    template = data[1];
					Log.d(TAG_TMP, "template data is contained with the first element 2");
					if(Type==1){
						Log.d(TAG_TMP, "Entro 1");
						realMinutia = Base64.encodeToString(data[1], Base64.NO_WRAP);
						Log.d(TAG_TMP, "Obtuvo ISO 2 y lo convirtió");
						Log.d(TAG_TMP, "ISO 2: " + realMinutia);
						
						
						Log.d(TAG_TMP, "Enviando el log");
						Intent i = new Intent();
						i.putExtra("base64String", "");
						i.putExtra("hand", "");
						i.putExtra("img", "");
						i.putExtra("minutia", realMinutia);
						setResult(Activity.RESULT_OK, i);
						finish();
						Log.d(TAG_TMP, "se envió el log");
						
						
					} else {
                    ConvertByteArray(template);
					}
                }
            }
        }

        @Override
        public void handleFailure() {
            ToastHelper.showMessage(FourfingerActivity.this, "Escaneo Fallido");
            Intent i = new Intent();
            i.putExtra("status", "Escaneo Fallido");
            setResult(Activity.RESULT_CANCELED, i);
            ISVeridiumTracker.trackEvent(getApplicationContext(), "ENTEL AA", "Veridium", "Capture", "Error");
            finish();
        }

        @Override
        public void handleCancellation() {
            ToastHelper.showMessage(FourfingerActivity.this, "Escaneo Cancelado");

            Intent i = new Intent();
            i.putExtra("status", "Escaneo Cancelado");
            setResult(Activity.RESULT_CANCELED, i);
            ISVeridiumTracker.trackEvent(getApplicationContext(), "ENTEL AA", "Veridium", "Capture", "Cancel");
            finish();
        }

        @Override
        public void handleError(String message) {
            ToastHelper.showMessage(FourfingerActivity.this, "Error: " + message, Toast.LENGTH_LONG);

            Intent i = new Intent();
            i.putExtra("status", "Escaneo Erroneo");
            setResult(Activity.RESULT_CANCELED, i);
            ISVeridiumTracker.trackEvent(getApplicationContext(), "ENTEL AA", "Veridium", "Capture", "Error" + message);
            finish();
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d(TAG, "onActivityResult");

        if (requestCode == REQUEST_APP_PREF) {
            // do nothing
        } else if (requestCode == REQUEST_EXPORT ||
                requestCode == REQUEST_ENROL ||
                requestCode == REQUEST_AUTH) {
            BiometricResultsParser.parse(resultCode, data, resultHandler);

        } else {
            Toast.makeText(this, "Error: unknown request result", Toast.LENGTH_LONG).show();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void ConvertByteArray(byte[] byteResponse) {

        String BinaryBase64ObjectObjectJPG="";
        String respuestaWSQ="";
        String minutia="";

        try {
            JSONObject object = new JSONObject(new String(byteResponse));
            JSONObject Scale085 = object.getJSONObject("SCALE085");
            JSONArray fingerprints = Scale085.getJSONArray("Fingerprints");

            // added
            JSONObject Scale100 = object.getJSONObject("SCALE100");
            JSONObject auditImage = Scale100.getJSONObject("AuditImage_0");
            // BinaryBase64ObjectObjectJPG = auditImage.getString("BinaryBase64ObjectJPG");

            int bestFingerI = 0;
            for (int i = 0; i < fingerprints.length(); i++) {
                JSONObject currentFingerprint = fingerprints.getJSONObject(i);
                int fingerPositionCode = currentFingerprint.getInt("FingerPositionCode");
                if (fingerPositionCode == BestFingerLeft || fingerPositionCode == BestFingerRight) {
                    bestFingerI = i;
                    break;
                }
            }

            JSONObject currentFingerprint = fingerprints.getJSONObject(bestFingerI);
            int fingerPositionCode = currentFingerprint.getInt("FingerPositionCode");
            JSONObject fingerImpressionImage = currentFingerprint.getJSONObject("FingerImpressionImage");
            



            String Hand = "";
            if (fingerPositionCode == LEFT_INDEX || fingerPositionCode == LEFT_MIDDLE ||
                    fingerPositionCode == LEFT_RING || fingerPositionCode == LEFT_PINKY ||
                    fingerPositionCode == LEFT_THUMB) {
                Hand = "LEFT";
            } else if (fingerPositionCode == RIGHT_INDEX || fingerPositionCode == RIGHT_MIDDLE ||
                    fingerPositionCode == RIGHT_RING || fingerPositionCode == RIGHT_PINKY ||
                    fingerPositionCode == RIGHT_THUMB) {
                Hand = "RIGHT";
            }

            
            
            if (Type==0)
            {
                respuestaWSQ = fingerImpressionImage.getString("BinaryBase64ObjectWSQ");
				minutia = Scale085.getString("ANSI378_FMR");
            }


	    
            Intent i = new Intent();
            i.putExtra("base64String", respuestaWSQ);
            i.putExtra("hand", Hand);
            i.putExtra("img", BinaryBase64ObjectObjectJPG);
	    i.putExtra("minutia", minutia);
            setResult(Activity.RESULT_OK, i);
            finish();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void preInitSDK() {

        Context appContext = getApplicationContext();

        try {
            // TODO add here the 4 FingersID Licence
			String fourfLicence = "l8wAnJgcvAH2pJX3Lvt+SebKfDPb1o9DpyBIBGyr2+WXG3iK/Y0yu2yY9INuF0VZTbWcCB31+ufz4d3Y6KLzBXsiZGV2aWNlRmluZ2VycHJpbnQiOiJtUjdUVUNaNlMrWnpMMlZtRzZuaU8yaTM5TGFLNDRMYS9yV1JVRDB1aGRjPSIsImxpY2Vuc2UiOiJXNTcxS21mTTZuOWVERElycjMvK0Q2UEh4ZzNsMHhNMFErdXY0dUFRYnZYeUFZZ2p6U3ptbGdUVmZZa0hBSld5VlVhUHNaVVRTbHdjMWxKbXk2ZVRCWHNpZEhsd1pTSTZJa0pKVDB4SlFsTWlMQ0p1WVcxbElqb2lORVlpTENKc1lYTjBUVzlrYVdacFpXUWlPakUzTXpBek1EYzBPVGswTXpVc0ltTnZiWEJoYm5sT1lXMWxJam9pU1c1emIyeDFkR2x2Ym5NZ0xTQkZiblJsYkNCTWIyZHBjM1JwWTNNZ0tHaHZiV1VnWkdWc2FYWmxjbmtwSWl3aVkyOXVkR0ZqZEVsdVptOGlPaUpGYm5SbGJDQXRJR2h2YldVZ1pHVnNhWFpsY25rZ05FWkZJR052YlM1bGJuUmxiQzV0YjNacGJDNWhjSEJrWld4cGRtVnllU0JsZUhBZ1QyTjBJRE14SURJd01qVWlMQ0pqYjI1MFlXTjBSVzFoYVd3aU9pSnRhV2QxWld3dWFHVnlibUZ1WkdWNlFHbHVjMjlzZFhScGIyNXpMbkJsSWl3aWMzVmlUR2xqWlc1emFXNW5VSFZpYkdsalMyVjVJam9pT1RkQ2RGVnFUVk5PUTNJeGRYbFNSM3BHWlZGYVlrRlVObWRUZDFOMGMzTnJNRmhRVmtob1FYWXdVVDBpTENKemRHRnlkRVJoZEdVaU9qRTJPVGcxTlRJd01EQXdNREFzSW1WNGNHbHlZWFJwYjI1RVlYUmxJam94TnpZeE9EZ3pNakF3TURBd0xDSm5jbUZqWlVWdVpFUmhkR1VpT2pFM05qSXdOVFl3TURBd01EQXNJblZ6YVc1blUwRk5URlJ2YTJWdUlqcG1ZV3h6WlN3aWRYTnBibWRHY21WbFVrRkVTVlZUSWpwbVlXeHpaU3dpZFhOcGJtZEJZM1JwZG1WRWFYSmxZM1J2Y25raU9tWmhiSE5sTENKaWFXOXNhV0pHWVdObFJYaHdiM0owUlc1aFlteGxaQ0k2Wm1Gc2MyVXNJbkoxYm5ScGJXVkZiblpwY205dWJXVnVkQ0k2ZXlKelpYSjJaWElpT21aaGJITmxMQ0prWlhacFkyVlVhV1ZrSWpwbVlXeHpaWDBzSW1abFlYUjFjbVZ6SWpwN0ltSmhjMlVpT25SeWRXVXNJbk4wWlhKbGIweHBkbVZ1WlhOeklqcDBjblZsTENKbGVIQnZjblFpT25SeWRXVjlMQ0psYm1admNtTmxaRkJ5WldabGNtVnVZMlZ6SWpwN0ltMWhibVJoZEc5eWVVeHBkbVZ1WlhOeklqcG1ZV3h6Wlgwc0luWmxjbk5wYjI0aU9pSXFMaW9pZlE9PSJ9";
            VeridiumSDK.init(appContext,
                    new DefaultVeridiumSDKModelFactory(appContext),
                    new VeridiumSDKFourFInitializer(fourfLicence),
                    new VeridiumSDKDataInitializer());
            // To get VeridiumVersion

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static boolean tienePermisos(Context context, String... permisos) {
        if (context != null && permisos != null) {
            for (String permiso : permisos) {
                if (ActivityCompat.checkSelfPermission(context, permiso) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

}
