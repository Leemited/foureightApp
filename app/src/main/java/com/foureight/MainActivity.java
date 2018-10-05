package com.foureight;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toolbar;

import com.google.firebase.iid.FirebaseInstanceId;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MAINACITIVITY";
    public WebView mWebview;
    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mFilePathCallback;
    private static final String TYPE_IMAGE = "image/*";
    private static final int INPUT_FILE_REQUEST_CODE = 1;
    private static final int CAMERA_REQUEST_CODE = 30;
    private String mCameraPhotoPath;

    Location location;
    protected LocationManager locationManager;
    boolean isGPSEnabled = false;
    boolean isNetworkEnabled = false;
    boolean isGetLocation = false;

    public String regId;

    public String url = "";
    private String mb_id,title,cate1,cate2,type,skip,filename="",videoname="";
    String[] files;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // create our manager instance after the content view is set
        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        // enable status bar tint
        tintManager.setStatusBarTintEnabled(true);
        // enable navigation bar tint
        tintManager.setNavigationBarTintEnabled(true);

        tintManager.setTintColor(Color.parseColor("#20000000"));

        mb_id=getIntent().getStringExtra("mb_id");
        title=getIntent().getStringExtra("title");
        type=getIntent().getStringExtra("type");
        cate1=getIntent().getStringExtra("cate1");
        cate2=getIntent().getStringExtra("cate2");
        if(getIntent().getStringExtra("skip")!=null) {
            skip = getIntent().getStringExtra("skip");
        }
        filename = getIntent().getStringExtra("filename");
        videoname = getIntent().getStringExtra("videoname");
        if(filename != null) {
            files = filename.split(",");
        }

        regId = FirebaseInstanceId.getInstance().getToken();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            String channelId  = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string.default_notification_channel_name);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW));
        }

        if (getIntent().getExtras() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                Object value = getIntent().getExtras().get(key);
            }
        }

        mWebview = (WebView) findViewById(R.id.webview);

        mWebview.setWebChromeClient(new CustomWebChromeClient());
        mWebview.setWebViewClient(new CustomWebClient());

        WebSettings webSettings = mWebview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAppCacheEnabled(false);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setUseWideViewPort(false);
        webSettings.setGeolocationEnabled(true);
        String userAgent = mWebview.getSettings().getUserAgentString();
        webSettings.setUserAgentString(userAgent+"/foureight");
        mWebview.addJavascriptInterface(new AndroidBridge(), "android");
        url = "http://mave01.cafe24.com/";
        if(files != null && files.length > 0){
            Log.d(TAG, "files: " + files[0]);
            url = url+"mobile/page/write.php?mb_id="+mb_id+"&type="+type+"&title="+title+"&cate1="+cate1+"&cate2="+cate2+"&filename="+filename;
        }
        if(videoname != null && filename != null){
            url = url + "&videoname="+videoname;
        }else if(videoname != null && filename == null){
            url = url + "mobile/page/write.php?mb_id="+mb_id+"&type="+type+"&title="+title+"&cate1="+cate1+"&cate2="+cate2+"&videoname="+videoname;
        }
        location = getLocation();

        if (location != null && files == null) {
            url = url + "index.php?lat=" + location.getLatitude() + "&lng=" + location.getLongitude();
        }else if(location != null && files != null){
            url = url + "&lat=" + location.getLatitude() + "&lng=" + location.getLongitude();
        }

        if(skip!=null){
            url = "http://mave01.cafe24.com/mobile/page/write.php?mb_id="+mb_id+"&type="+type+"&title="+title+"&cate1="+cate1+"&cate2="+cate2;
        }

        SharedPreferences pref = getSharedPreferences("AppLogin", MODE_PRIVATE);
        String mb_id_chk = pref.getString("mb_id", "");

        if(mb_id_chk!="" || mb_id_chk != null){
            Log.d(TAG, "onCreate: login" );
            url = url+ "&app_mb_id="+mb_id_chk;
        }else{
            Log.d(TAG, "onCreate: notLogin");
        }

        Log.d(TAG, "onCreate: url = " + url);
        mWebview.loadUrl(url);


        /*Button btn = (Button)findViewById(R.id.cameraon);
        btn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);
            }
        });*/
    }


    private class AndroidBridge {
        @JavascriptInterface
        public String setLogin(String mb_id){
            Log.d(TAG, "setLogin: ");
            if(mb_id!=null){
                SharedPreferences pref = getSharedPreferences("AppLogin", MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("mb_id", mb_id);
                editor.putString("login", "true");
                editor.commit();
                return "true";
            }else {
                return "false";
            }
        }

        @JavascriptInterface
        public String setLogout(){
            Log.d(TAG, "setLogout: ");
            SharedPreferences pref = getSharedPreferences("AppLogin", MODE_PRIVATE);
            String login = pref.getString("login", "");
            String mb_id_chk = pref.getString("mb_id", "");
            SharedPreferences.Editor editor = pref.edit();
            editor.clear();
            editor.commit();
            return "true";

        }

        @JavascriptInterface
        public String getRegid() {
            return regId;
        }
        @JavascriptInterface
        public void camereOn(final String mb_id, final String title, final String cate1, final String cate2,final String type) {
            Log.d(TAG, "cameraOn");
            Intent intent = new Intent(MainActivity.this,CameraActivity.class);
            intent.putExtra("mb_id",mb_id);
            intent.putExtra("title",title);
            intent.putExtra("cate1",cate1);
            intent.putExtra("cate2",cate2);
            intent.putExtra("type",type);
            startActivity(intent);
            finish();
        }
        @JavascriptInterface
        public void Onkeyboard(){
            Log.d(TAG,"onkeyboard");
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        }
        @JavascriptInterface
        public String getLocation() {
            String loc = "";
            isGetLocation = true;
            LocationImpl myLoc = new LocationImpl();
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if(isNetworkEnabled){
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    location = null;
                }else {
                    locationManager.requestLocationUpdates(locationManager.NETWORK_PROVIDER, 100, 1, myLoc);
                    if(locationManager != null){
                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                }
            }
            if(isGPSEnabled){
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    location = null;
                }else {
                    locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 100, 1, myLoc);
                    if(locationManager != null){
                        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    }
                }
            }
            if (location != null) {
                loc = location.getLatitude()+"/"+location.getLongitude();
                locationManager.removeUpdates(myLoc);
            }else{
                locationManager.removeUpdates(myLoc);
            }

            return loc;
        }
    }



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //backkey Event
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            if(mWebview.canGoBack()) {
                WebBackForwardList webBackForwardList = mWebview.copyBackForwardList();
                String backUrl = webBackForwardList.getItemAtIndex(webBackForwardList.getCurrentIndex()-1).getUrl();
                if(backUrl.contains("login") != true){
                    Log.d(TAG, "onKeyDown: " + mWebview.getUrl());
                    if(mWebview.getUrl().contains("#modal") == true){
                        mWebview.loadUrl("javascript:modalclose()");
                        mWebview.goBackOrForward(-1);
                    }else if(mWebview.getUrl().contains("#menu") == true){
                        mWebview.loadUrl("javascript:closeMenu()");
                        mWebview.goBackOrForward(-1);
                    }else if(mWebview.getUrl().contains("#view") == true){
                        mWebview.loadUrl("javascript:modalCloseThis()");
                        mWebview.goBackOrForward(-1);
                    }else if(mWebview.getUrl().contains("#search") == true){
                        mWebview.loadUrl("javascript:fnSetting()");
                        mWebview.goBackOrForward(-1);
                    }else if(mWebview.getUrl().contains("#preview") == true){
                        mWebview.loadUrl("javascript:hidePreview()");
                        mWebview.goBackOrForward(-1);
                    }else if(mWebview.getUrl().contains("#detailview") == true){
                        mWebview.loadUrl("javascript:hideDetail()");
                        mWebview.goBackOrForward(-1);
                    }else if(mWebview.getUrl().contains("#blind") == true){
                        mWebview.loadUrl("javascript:blindClose()");
                        mWebview.goBackOrForward(-1);
                    }else if(mWebview.getUrl().contains("#category") == true){
                        mWebview.loadUrl("javascript:cateClose()");
                        mWebview.goBackOrForward(-1);
                    }else{
                        mWebview.goBack();
                    }
                }else{
                    
                }
            }else{
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle("앱 종료")
                        .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        })
                        .setPositiveButton("종료", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        });
                AlertDialog alertDialog = alert.create();
                alert.show();
            }
            return true;
        }
        return false;
    }

    private class CustomWebChromeClient extends WebChromeClient {
        @Override
        public boolean onConsoleMessage(ConsoleMessage cm) {
            Log.e("raon", cm.message() + " -- From Line " + cm.lineNumber() + "of" + cm.sourceId());
            return true;
        }

        @Override
        public void onCloseWindow(WebView window) {
            super.onCloseWindow(window);
            finish();
        }

        // For Android Version < 3.0
        public void openFileChooser(ValueCallback<Uri> uploadMsg) {
            mUploadMessage = uploadMsg;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType(TYPE_IMAGE);
            startActivityForResult(intent, INPUT_FILE_REQUEST_CODE);
        }

        // For 3.0 <= Android Version < 4.1
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType){
            openFileChooser(uploadMsg, acceptType, "");
        }

        // For 4.1 <= Android Version < 5.0
        public void openFileChooser(ValueCallback<Uri> uploadFile, String acceptType, String capture){
            Log.d(getClass().getName(), "openFileChooser : " + acceptType + "/" + capture);
            mUploadMessage = uploadFile;
            imageChooser();
        }

        // For Android Version 5.0+
        // Ref: https://github.com/GoogleChrome/chromium-webview-samples/blob/master/input-file-example/app/src/main/java/inputfilesample/android/chrome/google/com/inputfilesample/MainFragment.java
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            System.out.println("WebViewActivity A>5, OS Version : " + Build.VERSION.SDK_INT + "\t onSFC(WV,VCUB,FCP), n=3");
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = filePathCallback;
            imageChooser();
            return true;
        }


        private void imageChooser(){
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if(takePictureIntent.resolveActivity(getPackageManager()) != null){
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                    takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                }catch (IOException ex){
                    Log.e(TAG, "Unable to creat Image File -", ex);
                }

                if(photoFile !=null){
                    mCameraPhotoPath = "file:"+photoFile.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                }else{
                    takePictureIntent = null;
                }
            }

            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType(TYPE_IMAGE);

            Intent[] intentArray;
            if(takePictureIntent != null){
                intentArray = new Intent[]{takePictureIntent};
            }else{
                intentArray = new Intent[0];
            }

            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "파일 선택");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

            startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);
        }
    }

    private class CustomWebClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("tel:")) {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                startActivity(intent);
                return true;
            }
            if(url.startsWith("sms:")) {
                Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
                startActivity(intent);
                return true;
            }
            return false;
        }
    }

    private File createImageFile() throws IOException{
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);
        return imageFile;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "Result" + resultCode);
        if(requestCode == INPUT_FILE_REQUEST_CODE && requestCode == RESULT_OK){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                if(mFilePathCallback == null){
                    super.onActivityResult(requestCode, resultCode, data);
                    return;
                }

                Uri[] results = new Uri[]{getResultUri(data)};

                mFilePathCallback.onReceiveValue(results);
                mFilePathCallback = null;
            }else{
                if(mUploadMessage == null){
                    super.onActivityResult(requestCode, resultCode, data);
                    return;
                }
                Uri result = getResultUri(data);

                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;
            }
        }else{
            if(mFilePathCallback != null) mFilePathCallback.onReceiveValue(null);
            if(mUploadMessage != null) mUploadMessage.onReceiveValue(null);
            mFilePathCallback = null;
            mUploadMessage = null;
            super.onActivityResult(requestCode, resultCode, data);
        }

        if(requestCode == CAMERA_REQUEST_CODE  && requestCode == RESULT_OK){
            Log.d(TAG,"cam result : " + data.getType());
        }
    }

    private Uri getResultUri(Intent data){
        Uri result = null;

        if(data == null || TextUtils.isEmpty(data.getDataString())){
            if(mCameraPhotoPath != null){
                result = Uri.parse(mCameraPhotoPath);
            }
        }else{
            String filePath = "";
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                filePath = data.getDataString();
            }else{
                filePath = "file : " + RealPathUtil.getRealPath(this,data.getData());
            }
            result = Uri.parse(filePath);
        }
        return result;
    }

    public Location getLocation(){
        isGetLocation = true;
        LocationImpl myLoc = new LocationImpl();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if(isNetworkEnabled){
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                location = null;
            }else {
                locationManager.requestLocationUpdates(locationManager.NETWORK_PROVIDER, 100, 1, myLoc);
                if(locationManager != null){
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            }
        }
        if(isGPSEnabled){
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                location = null;
            }else {
                locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 100, 1, myLoc);
                if(locationManager != null){
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
            }
        }

        locationManager.removeUpdates(myLoc);
        return location;
    }
}
