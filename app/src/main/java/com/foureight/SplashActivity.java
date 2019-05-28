package com.foureight;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

public class SplashActivity extends AppCompatActivity {
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_splash);

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);

        SharedPreferences pref = getSharedPreferences("badge_count",MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.remove("badgeCount");
        editor.commit();

        Intent badgeIntent = new Intent("android.intent.action.BADGE_COUNT_UPDATE");
        badgeIntent.putExtra("badge_count",0);
        badgeIntent.putExtra("badge_count_package_name", getPackageName());
        badgeIntent.putExtra("badge_count_class_name",SplashActivity.class.getName());
        sendBroadcast(badgeIntent);

        if(!isNetworkConnected(SplashActivity.this)){
            CommonDialogs cm = new CommonDialogs(this);

            cm.showAlertDialog(this, "현재 인터넷 연결이 안되어 있어 앱을 종료합니다.", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            },null);

        }else {

            this.handler = new Handler();
            this.handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    PermissionListener permissionlistener = new PermissionListener() {
                        @Override
                        public void onPermissionGranted() {
                            //Toast.makeText(SplashActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
                            Intent localIntent2 = new Intent(SplashActivity.this, MainActivity.class);
                            startActivity(localIntent2);
                            finish();
                        }

                        @Override
                        public void onPermissionDenied(ArrayList<String> deniedPermissions) {
                            //Toast.makeText(SplashActivity.this, "Permission Denied\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    };
                    TedPermission.with(SplashActivity.this)
                            .setPermissionListener(permissionlistener)
                            .setDeniedMessage("[설정] > [권한] 에서 권한을 허용할 수 있습니다.")
                            .setPermissions(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                            .check();
                }
            }, 3000L);
        }
    }

    public static boolean isNetworkConnected(Context context){
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mobile = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo winmax = manager.getNetworkInfo(ConnectivityManager.TYPE_WIMAX);
        boolean bwinmax = false;
        if(winmax != null){
            bwinmax = winmax.isConnected();
        }
        if(mobile != null){
            if(mobile.isConnected() || wifi.isConnected() || bwinmax) {
                return true;
            }
        }else{
            if(wifi.isConnected() || bwinmax) {
                return false;
            }
        }
        return false;
        //return manager.getActiveNetworkInfo() != null;
    }
}
