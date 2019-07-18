package com.foureight;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Message;
import android.provider.Browser;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MAINACITIVITYSS";
    public WebView mWebview;
    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mFilePathCallback;
    private static final String TYPE_IMAGE = "image/*";
    private static final int INPUT_FILE_REQUEST_CODE = 1;
    private static final int CAMERA_REQUEST_CODE = 30;
    private static final int CAMERA_EDIT_REQUEST_CODE = 31;
    private static final int VIDEO_EDIT_REQUEST_CODE = 32;
    private String mCameraPhotoPath;

    Location location;
    protected LocationManager locationManager;
    boolean isGPSEnabled = false;
    boolean isNetworkEnabled = false;
    boolean isGetLocation = false;

    public String regId;

    public String url = "";
    public String fcmUrl = "";
    private String mb_id,title,cate1,cate2,type1,type2,skip,filename="",videoname="",wr_price,wr_price2,index,pd_price_type;
    String[] files;

    private final long FINISH_INTERVAL_TIME = 2000;
    private long backPressedTime = 0;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    private ProgressBar progressBar;

    private MyFirebaseMessagingService fcmService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PRRUN.bAppRunned = true;

        mb_id=getIntent().getStringExtra("mb_id");
        title=getIntent().getStringExtra("title");
        type1=getIntent().getStringExtra("type1");
        type2=getIntent().getStringExtra("type2");
        cate1=getIntent().getStringExtra("cate1");
        cate2=getIntent().getStringExtra("cate2");
        wr_price=getIntent().getStringExtra("wr_price");
        wr_price2=getIntent().getStringExtra("wr_price2");
        pd_price_type=getIntent().getStringExtra("pd_price_type");
        index=getIntent().getStringExtra("index");
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
            notificationManager =
                    getSystemService(NotificationManager.class);
            Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            AudioAttributes att = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();
            NotificationChannel defaultChannel = new NotificationChannel(channelId,channelName,NotificationManager.IMPORTANCE_HIGH);
            defaultChannel.setSound(defaultSoundUri,att);
            defaultChannel.setDescription("기본알림에 관한 설정 입니다.");
            defaultChannel.enableVibration(true);
            defaultChannel.setVibrationPattern(new long[]{100,0,0,400,0,0,100,0,0,500,0,0});
            notificationManager.createNotificationChannel(defaultChannel);

            String commentId  = getString(R.string.comment_notification_channel_id);
            String commentName = getString(R.string.comment_notification_channel_name);

            NotificationChannel commentChannel = new NotificationChannel(commentId,commentName,NotificationManager.IMPORTANCE_HIGH);
            commentChannel.setSound(defaultSoundUri,att);
            commentChannel.setDescription("댓글에 관한 설정 입니다.");
            commentChannel.enableVibration(true);
            commentChannel.setVibrationPattern(new long[]{100,0,0,400,0,0,100,0,0,500,0,0});

            notificationManager.createNotificationChannel(commentChannel);

            String buyId  = getString(R.string.buy_notification_channel_id);
            String buyName = getString(R.string.buy_notification_channel_name);

            NotificationChannel buyChannel = new NotificationChannel(buyId,buyName,NotificationManager.IMPORTANCE_HIGH);
            buyChannel.setSound(defaultSoundUri,att);
            buyChannel.setDescription("구매관련 설정 입니다.");
            buyChannel.enableVibration(true);
            buyChannel.setVibrationPattern(new long[]{100,0,0,400,0,0,100,0,0,500,0,0});

            notificationManager.createNotificationChannel(buyChannel);

            String pricingId  = getString(R.string.pricing_notification_channel_id);
            String pricingName= getString(R.string.pricing_notification_channel_name);

            NotificationChannel pricingChannel = new NotificationChannel(pricingId,pricingName,NotificationManager.IMPORTANCE_HIGH);
            pricingChannel.setSound(defaultSoundUri,att);
            pricingChannel.setDescription("제시/딜 관련 설정 입니다.");
            pricingChannel.enableVibration(true);
            pricingChannel.setVibrationPattern(new long[]{100,0,0,400,0,0,100,0,0,500,0,0});

            notificationManager.createNotificationChannel(pricingChannel);

            String chatId  = getString(R.string.chat_notification_channel_id);
            String chatName = getString(R.string.chat_notification_channel_name);

            NotificationChannel chatChannel = new NotificationChannel(chatId,chatName,NotificationManager.IMPORTANCE_HIGH);
            chatChannel.setSound(defaultSoundUri,att);
            chatChannel.setDescription("대화관련 설정 입니다.");
            chatChannel.enableVibration(true);
            chatChannel.setVibrationPattern(new long[]{100,0,0});

            notificationManager.createNotificationChannel(chatChannel);

            String searchId  = getString(R.string.search_notification_channel_id);
            String searchName = getString(R.string.search_notification_channel_name);

            NotificationChannel searchChannel = new NotificationChannel(searchId,searchName,NotificationManager.IMPORTANCE_HIGH);
            searchChannel.setSound(defaultSoundUri,att);
            searchChannel.setDescription("검색관련 설정 입니다.");
            searchChannel.enableVibration(true);
            searchChannel.setVibrationPattern(new long[]{100,0,0,400,0,0,100,0,0,500,0,0});

            notificationManager.createNotificationChannel(searchChannel);
        }

        if (getIntent().getExtras() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                Object value = getIntent().getExtras().get(key);
            }
        }

        progressBar = findViewById(R.id.progressBar);
        mWebview = findViewById(R.id.webview);

        mWebview.setWebChromeClient(new CustomWebChromeClient());
        mWebview.setWebViewClient(new CustomWebClient());

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            mWebview.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

            CookieManager cookieManager = CookieManager.getInstance();

            cookieManager.setAcceptCookie(true);

            cookieManager.setAcceptThirdPartyCookies(mWebview, true);

        }
        WebSettings webSettings = mWebview.getSettings();
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        webSettings.setEnableSmoothTransition(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSaveFormData(false);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setUseWideViewPort(false);
        webSettings.setGeolocationEnabled(true);
        String userAgent = mWebview.getSettings().getUserAgentString();
        webSettings.setUserAgentString(userAgent+"/foureight");
        mWebview.addJavascriptInterface(new AndroidBridge(), "android");
        url = "http://mave01.cafe24.com/";

        String urlTitle = "";
        try {
            if(title!=null) {
                urlTitle = URLEncoder.encode(title, "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if(files != null && files.length > 0){
            Log.d(TAG, "files: " + files[0]);
            url = url+"mobile/page/write.php?mb_id="+mb_id+"&wr_type1="+type1+"&pd_type2="+type2+"&cate1="+cate1+"&cate2="+cate2+"&filename="+filename+"&title="+urlTitle+"&wr_price="+wr_price+"&wr_price2="+wr_price2+"&pd_price_type="+pd_price_type;
        }
        if(videoname != null && filename != null){
            url = url + "&videoname="+videoname;
        }else if(videoname != null && filename == null){
            url = url + "mobile/page/write.php?mb_id="+mb_id+"&wr_type1="+type1+"&pd_type2="+type2+"&title="+urlTitle+"&cate1="+cate1+"&cate2="+cate2+"&videoname="+videoname+"&wr_price="+wr_price+"&wr_price2="+wr_price2+"&pd_price_type="+pd_price_type;
        }
        location = getLocation();

        if (location != null && files == null) {
            url = url + "index.php?lat=" + location.getLatitude() + "&lng=" + location.getLongitude();
        }else if(location != null && files != null){
            url = url + "&lat=" + location.getLatitude() + "&lng=" + location.getLongitude();
        }
        if(index==null) {
            if (skip != null) {
                url = "http://mave01.cafe24.com/mobile/page/write.php?mb_id=" + mb_id + "&wr_type1=" + type1 + "&pd_type2=" + type2 + "&title=" + urlTitle + "&cate1=" + cate1 + "&cate2=" + cate2 + "&wr_price=" + wr_price + "&wr_price2=" + wr_price2+"&pd_price_type="+pd_price_type;
            }
        }

        SharedPreferences pref = getSharedPreferences("AppLogin", MODE_PRIVATE);
        String mb_id_chk = pref.getString("mb_id", "");

        fcmUrl = getIntent().getStringExtra("url");
        if(fcmUrl!=null){
            url = fcmUrl;
        }

        if(mb_id_chk!="" || mb_id_chk != null){
            Log.d(TAG, "onCreate: login" );

            if(url.indexOf("?")!=-1) {
                url = url + "&app_mb_id=" + mb_id_chk;
            }else {
                url = url + "?app_mb_id=" + mb_id_chk;
            }
        }else{
            Log.d(TAG, "onCreate: notLogin");
        }

        Log.d(TAG, "onCreate: url = " + url + " // "+ urlTitle + "// fcmUrl : "+ fcmUrl);

        //mWebview.clearCache(true);

        mWebview.loadUrl(url);

        final SoftKeyboardDectectorView softKeyboardDectectorView = new SoftKeyboardDectectorView(this);
        addContentView(softKeyboardDectectorView, new FrameLayout.LayoutParams(-1,-1));
        softKeyboardDectectorView.setOnHiddenKeyboard(new SoftKeyboardDectectorView.OnHiddenKeyboardListener() {
            @Override
            public void onHiddenSoftKeyboard() {
                mWebview.loadUrl("javascript:showMenu()");
            }
        });
    }


    private class AndroidBridge {
        @JavascriptInterface
        public void copyLink(String link){
            Log.d(TAG, "copyLink: " + link);
            ClipboardManager clipboardManager = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("48링크", link);
            clipboardManager.setPrimaryClip(clipData);

        }

        @JavascriptInterface
        public String setLogin(String mb_id){
            Log.d(TAG, "AndroidBridge: "+ mb_id);
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
            Log.d(TAG, "AndroidBridge: logout");
            SharedPreferences pref = getSharedPreferences("AppLogin", MODE_PRIVATE);
            pref.getString("login", "");
            pref.getString("mb_id", "");
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
        public String getSdkVersion() {
            return String.valueOf(Build.VERSION.SDK_INT);
        }

        @JavascriptInterface
        public void settingOn() {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        }

        @JavascriptInterface
        public void camereOn(final String mb_id, final String title, final String cate1, final String cate2,final String type1, final String type2,final String wr_price, final String wr_price2, final String pd_price_type) {
            Log.d(TAG, "cameraOn :" + title);
            Intent intent = new Intent(MainActivity.this,CameraActivity.class);
            intent.putExtra("mb_id",mb_id);
            intent.putExtra("title",title);
            intent.putExtra("cate1",cate1);
            intent.putExtra("cate2",cate2);
            intent.putExtra("type1",type1);
            intent.putExtra("type2",type2);
            intent.putExtra("wr_price",wr_price);
            intent.putExtra("wr_price2",wr_price2);
            intent.putExtra("pd_price_type",pd_price_type);
            startActivity(intent);
            finish();
        }

        @JavascriptInterface
        public void camereOn2(final String mb_id,final String index) {
            Intent intent = new Intent(MainActivity.this,CameraEditActivity.class);
            intent.putExtra("mb_id",mb_id);
            intent.putExtra("index",index);
            startActivityForResult(intent,CAMERA_EDIT_REQUEST_CODE);
            //finish();
        }

        @JavascriptInterface
        public void camereOn3(final String mb_id) {
            Log.d(TAG, "camereOn3: " + mb_id);
            Intent intent = new Intent(MainActivity.this,CameraEditCameraActivity.class);
            intent.putExtra("mb_id",mb_id);
            startActivityForResult(intent,VIDEO_EDIT_REQUEST_CODE);
        }

        @JavascriptInterface
        public void write_complete(){
            SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("write_active", "false");
            editor.commit();
        }

        @JavascriptInterface
        public void test(){
            Intent intent = new Intent(Settings.ACTION_APPLICATION_SETTINGS);
            startActivity(intent);
        }

        @JavascriptInterface
        public void Onkeyboard(){
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        }

        @JavascriptInterface
        public void HideKeyboard(){
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, InputMethodManager.SHOW_FORCED);
        }

        @JavascriptInterface
        public void resetBadge(){
            Intent badgeIntent = new Intent("android.intent.action.BADGE_COUNT_UPDATE");
            badgeIntent.putExtra("badge_count",0);
            badgeIntent.putExtra("badge_count_package_name", getPackageName());
            badgeIntent.putExtra("badge_count_class_name", SplashActivity.class.getName());
            sendBroadcast(badgeIntent);
        }

        @JavascriptInterface
        public String getLocation() {
            String loc = "";
            isGetLocation = true;
            LocationImpl myLoc = new LocationImpl();
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
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
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            if(mWebview.canGoBack()) {
                WebBackForwardList webBackForwardList = mWebview.copyBackForwardList();
                String backUrl = webBackForwardList.getItemAtIndex(webBackForwardList.getCurrentIndex()-1).getUrl();
                String curruntUrl = mWebview.getUrl();
                Log.d(TAG, "뒤로가기: " + backUrl + "//" + curruntUrl);

                if (backUrl.contains("login") != true) {
                    
                    if (mWebview.getUrl().contains("#modal") == true) {
                        mWebview.loadUrl("javascript:modalClose()");
                        mWebview.goBackOrForward(-1);
                    } else if (mWebview.getUrl().contains("#menu") == true) {
                        mWebview.loadUrl("javascript:closeMenu()");
                        mWebview.goBackOrForward(-1);
                    } else if (mWebview.getUrl().contains("#view") == true) {
                        mWebview.loadUrl("javascript:modalCloseThis()");
                        mWebview.goBackOrForward(-1);
                    } else if (mWebview.getUrl().contains("#search") == true) {
                        mWebview.loadUrl("javascript:fnSetting()");
                        mWebview.goBackOrForward(-1);
                    } else if (mWebview.getUrl().contains("#preview") == true) {
                        mWebview.loadUrl("javascript:hidePreview()");
                        mWebview.goBackOrForward(-1);
                    } else if (mWebview.getUrl().contains("#detailview") == true) {
                        Log.d(TAG, "onKeyDown: Action");
                        mWebview.loadUrl("javascript:hideDetail()");
                        mWebview.goBackOrForward(-1);
                    } else if (mWebview.getUrl().contains("#blind") == true) {
                        mWebview.loadUrl("javascript:blindClose()");
                        mWebview.goBackOrForward(-1);
                    } else if (mWebview.getUrl().contains("#category") == true) {
                        mWebview.loadUrl("javascript:cateClose()");
                        mWebview.goBackOrForward(-1);
                    }  else if (mWebview.getUrl().contains("#talkView") == true) {
                        mWebview.loadUrl("javascript:modalCloseTalk()");
                        mWebview.goBackOrForward(-1);
                    } else if (mWebview.getUrl().contains("#mapView") == true) {
                        mWebview.loadUrl("javascript:mapViewClose()");
                        mWebview.goBackOrForward(-1);
                    } else {
                        if(curruntUrl.contains("cafe24.com/index.php") == true || (curruntUrl.equals("http://mave01.cafe24.com/") == true || curruntUrl.equals("http://mave01.cafe24.com/#") == true)){
                            long tempTime = System.currentTimeMillis();
                            long intervalTime = tempTime - backPressedTime;

                            if (0 <= intervalTime && FINISH_INTERVAL_TIME >= intervalTime) {
                                finish();
                            } else {
                                backPressedTime = tempTime;
                                Toast.makeText(this, "뒤로가기를 한번더 누르면 종료 됩니다.", Toast.LENGTH_SHORT).show();
                            }
                        }else {
                            mWebview.goBack();
                        }
                    }
                } else {
                    long tempTime = System.currentTimeMillis();
                    long intervalTime = tempTime - backPressedTime;

                    if (0 <= intervalTime && FINISH_INTERVAL_TIME >= intervalTime) {
                        finish();
                    } else {
                        backPressedTime = tempTime;
                        Toast.makeText(this, "뒤로가기를 한번더 누르면 종료 됩니다.", Toast.LENGTH_SHORT).show();
                    }
                }

            }else{
                long tempTime = System.currentTimeMillis();
                long intervalTime = tempTime - backPressedTime;

                if (0 <= intervalTime && FINISH_INTERVAL_TIME >= intervalTime) {
                    finish();
                } else {
                    backPressedTime = tempTime;
                    Toast.makeText(this, "뒤로가기를 한번더 누르면 종료 됩니다.", Toast.LENGTH_SHORT).show();
                }
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
            Log.d(TAG, "openFileChooser: 3.0");
            mUploadMessage = uploadMsg;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType(TYPE_IMAGE);
            startActivityForResult(intent, INPUT_FILE_REQUEST_CODE);
        }

        // For 3.0 <= Android Version < 4.1
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType){
            Log.d(TAG, "openFileChooser: 4.1");
            openFileChooser(uploadMsg, acceptType, "");
        }

        // For 4.1 <= Android Version < 5.0
        public void openFileChooser(ValueCallback<Uri> uploadFile, String acceptType, String capture){
            Log.d(TAG, "openFileChooser: 5.0");
            Log.d(getClass().getName(), "openFileChooser : " + acceptType + "/" + capture);
            mUploadMessage = uploadFile;
            imageChooser();
        }

        // For Android Version 5.0+
        // Ref: https://github.com/GoogleChrome/chromium-webview-samples/blob/master/input-file-example/app/src/main/java/inputfilesample/android/chrome/google/com/inputfilesample/MainFragment.java
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            Log.d(TAG, "onShowFileChooser: 5.0 + ");
            //System.out.println("WebViewActivity A>5, OS Version : " + Build.VERSION.SDK_INT + "\t onSFC(WV,VCUB,FCP), n=3");
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = filePathCallback;
            imageChooser();
            return true;
        }


        private void imageChooser(){

            /*Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
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
            }*/

            Intent chooserIntent = new Intent(Intent.ACTION_PICK);
            chooserIntent.setType(TYPE_IMAGE);
            //chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

            startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, final android.webkit.JsResult result){
            new AlertDialog.Builder(view.getContext())
                    .setTitle("48 알림")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok,
                            new AlertDialog.OnClickListener(){
                                public void onClick(DialogInterface dialog, int which) {
                                    result.confirm();
                                }
                            })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            result.cancel();
                        }
                    })
                    .setCancelable(false)
                    .create()
                    .show();
            return true;
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, final android.webkit.JsResult result){
            new AlertDialog.Builder(view.getContext())
                    .setTitle("48 알림")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    result.confirm();
                                }
                            })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    result.cancel();
                                }
                            })
                    .setCancelable(false)
                    .create()
                    .show();

            return true;
        }
    }

    private class CustomWebClient extends WebViewClient {



        @Override
        public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
            Log.d(TAG, "onUnhandledKeyEvent: " + view.getUrl());
            if(event.getKeyCode() == 66 && view.getUrl().contains("my_location.php")){
                view.loadUrl("javascript:mapKeySet()");
            }
            if(event.getKeyCode() == 66 && view.getUrl().contains("#writes")){
                view.loadUrl("javascript:fnOnCam()");
            }
            //super.onUnhandledKeyEvent(view, event);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if(!isNetworkConnected(MainActivity.this)){
                /*CommonDialogs cm = new CommonDialogs(MainActivity.this);

                cm.showAlertDialog(MainActivity.this, "현재 인터넷 연결이 안되어 있어 앱을 종료합니다.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                },null);*/
                finish();
                Toast.makeText(MainActivity.this,"현재 인터넷이 연결되어 있지 않아 앱을 종료 하였습니다.",Toast.LENGTH_SHORT).show();
            }

            if (url != null && url.indexOf("search.naver") > -1) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setPackage("com.android.chrome");
                i.setData(Uri.parse(url));
                startActivity(i);
                return true;
            }

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

            if (url.startsWith("intent:")) {
                try {
                    Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    Intent existPackage = getPackageManager().getLaunchIntentForPackage(intent.getPackage());
                    if (existPackage != null) {
                        startActivity(intent);
                    } else {
                        Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                        marketIntent.setData(Uri.parse("market://details?id=" + intent.getPackage()));
                        startActivity(marketIntent);
                    }
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (url.startsWith("http://") || url.startsWith("https://")) {
                view.loadUrl(url);
                return true;
            }

            if ((url.startsWith("http://") || url.startsWith("https://")) && (url.contains("market.android.com") || url.contains("m.ahnlab.com/kr/site/download"))) {
                Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    startActivity(intent);
                    return true;
                } catch (ActivityNotFoundException e) {
                    return false;
                }
            } else if (url != null
                    && (url.contains("vguard") || url.contains("droidxantivirus") || url.contains("smhyundaiansimclick://")
                    || url.contains("smshinhanansimclick://") || url.contains("smshinhancardusim://") || url.contains("smartwall://") || url.contains("appfree://")
                    || url.contains("v3mobile") || url.endsWith(".apk") || url.contains("market://") || url.contains("ansimclick")
                    || url.contains("market://details?id=com.shcard.smartpay") || url.contains("shinhan-sr-ansimclick://"))) {
                Intent intent = null;
                // 인텐트 정합성 체크
                try {
                    intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    Log.e("getScheme :", intent.getScheme());
                    Log.e("getDataString :", intent.getDataString());
                } catch (URISyntaxException ex) {
                    Log.e("Browser", "Bad URI " + url + ":" + ex.getMessage());
                    return false;
                }
                try {
                    boolean retval = true;
//chrome 버젼 방식
                    if (url.startsWith("intent")) {
// 앱설치 체크를 합니다.
                        if (getPackageManager().resolveActivity(intent, 0) == null) {
                            String packagename = intent.getPackage();
                            if (packagename != null) {
                                Uri uri = Uri.parse("market://search?q=pname:" + packagename);
                                intent = new Intent(Intent.ACTION_VIEW, uri);
                                startActivity(intent);
                                retval = true;
                            }
                        } else {
                            intent.addCategory(Intent.CATEGORY_BROWSABLE);
                            intent.setComponent(null);
                            try {
                                if (startActivityIfNeeded(intent, -1)) {
                                    retval = true;
                                }
                            } catch (ActivityNotFoundException ex) {
                                retval = false;
                            }
                        }
                    } else { // 구 방식
                        Uri uri = Uri.parse(url);
                        intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                        retval = true;
                    }
                    return retval;
                } catch (ActivityNotFoundException e) {
                    Log.e("error ===>", e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            }

            // 계좌이체 커스텀 스키마
            if (url.startsWith("smartxpay-transfer://")) {
                boolean isatallFlag = isPackageInstalled(getApplicationContext(), "kr.co.uplus.ecredit");
                if (isatallFlag) {
                    boolean override = false;
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.addCategory(Intent.CATEGORY_BROWSABLE);
                    intent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());
                    try {
                        startActivity(intent);
                        override = true;
                    } catch (ActivityNotFoundException ex) {
                    }
                    return override;
                } else {
                    showAlert("확인버튼을 누르시면 구글플레이로 이동합니다.", "확인", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(("market://details?id=kr.co.uplus.ecredit")));
                            intent.addCategory(Intent.CATEGORY_BROWSABLE);
                            intent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());
                            startActivity(intent);
                            overridePendingTransition(0, 0);
                        }
                    } , "취소", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    return true;
                }
            }else if(url.startsWith("lguthepay-xpay://")) {
                boolean isatallFlag = isPackageInstalled(getApplicationContext(), "com.lguplus.paynow");
                if (isatallFlag) {
                    boolean override = false;
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.addCategory(Intent.CATEGORY_BROWSABLE);
                    intent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());

                    try {
                        startActivity(intent);
                        override = true;
                    } catch (ActivityNotFoundException ex) {
                    }
                    return override;
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(("market://details?id=com.lguplus.paynow")));
                    intent.addCategory(Intent.CATEGORY_BROWSABLE);
                    intent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                }
            }

            // 모바일ISP 커스텀 스키마
            if (url.startsWith("ispmobile://")) {
                boolean isatallFlag = isPackageInstalled(getApplicationContext(), "kvp.jjy.MispAndroid320");
                if (isatallFlag) {
                    boolean override = false;
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.addCategory(Intent.CATEGORY_BROWSABLE);
                    intent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());
                    try {
                        startActivity(intent);
                        override = true;
                    } catch (ActivityNotFoundException ex) {
                    }
                    return override;
                } else {
                    showAlert("확인버튼을 누르시면 구글플레이로 이동합니다.", "확인", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mWebview.loadUrl("http://mobile.vpay.co.kr/jsp/MISP/andown.jsp");
                        }
                    } , "취소", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    return true;
                }
            }

            return false;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progressBar.setVisibility(View.GONE);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                CookieSyncManager.getInstance().sync();
            } else {
                CookieManager.getInstance().flush();
            }
        }

    }

    // App 체크 메소드 // 존재:true, 존재하지않음:false
    public static boolean isPackageInstalled(Context ctx, String pkgName) {
        try {
            ctx.getPackageManager().getPackageInfo(pkgName, PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void showAlert(String message, String positiveButton, DialogInterface.OnClickListener positiveListener, String negativeButton, DialogInterface.OnClickListener negativeListener) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage(message);
        alert.setPositiveButton(positiveButton, positiveListener);
        alert.setNegativeButton(negativeButton, negativeListener);
        alert.show();
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
        Log.d(TAG, "onActivityResult: " + requestCode + "// resultCode : " + resultCode + "// data: " + data);
        if(requestCode == CAMERA_EDIT_REQUEST_CODE && resultCode == RESULT_OK){
            String index = data.getStringExtra("index");
            String filename = data.getStringExtra("filename");
            String skip = data.getStringExtra("skip");
            Log.d(TAG, "index: " + index + "// filename : " + filename + "// skip: " + skip);
            if(skip==null || skip==""){
                Log.d(TAG, "index: " + index + "// filename : " + filename + "// skip: " + skip);
                mWebview.loadUrl("javascript:setImages('"+filename+"','"+index+"');");
            }
        }else if(requestCode == INPUT_FILE_REQUEST_CODE && resultCode == RESULT_OK){
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
        }else if(requestCode == VIDEO_EDIT_REQUEST_CODE && resultCode == RESULT_OK){
            String videoname = data.getStringExtra("videoname");
            String skip = data.getStringExtra("skip");
            Log.d(TAG, "index: " + index + "// videoname : " + videoname);
            if(skip==null || skip==""){
                Log.d(TAG, "index: " + index + "// videoname : " + videoname + "// skip: " + skip);
                mWebview.loadUrl("javascript:setVideo('"+videoname+"');");
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

        if(!isGPSEnabled && !isNetworkEnabled){
            CommonDialogs cm = new CommonDialogs(MainActivity.this);
            cm.showAlertDialog(MainActivity.this, "GPS정보를 찾을 수 없습니다. \r설정창으로 이동하시겠습니까?", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //확인
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            }, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //취소
                }
            });
            return null;
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

        //Log.d(TAG, "getLocation: " + location.getLatitude());

        locationManager.removeUpdates(myLoc);
        return location;
    }

    @Override
    protected void onDestroy() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(mWebview.getWindowToken(),0);
        PRRUN.bAppRunned = false;
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.getInstance().stopSync();
        }
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(mWebview.getWindowToken(),0);
        Log.d(TAG, "onPause: ");
        PRRUN.bAppRunned = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(this);
        }
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(mWebview.getWindowToken(),0);
        Log.d(TAG, "onResume: ");
        PRRUN.bAppRunned = true;
    }

    /*
    * 네트워크 상태 체크
    */
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
