package com.foureight;
/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.webkit.WebView;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    Bitmap bigPicture;
    private int badgeCount = 0;
    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            /*if (/* Check if data needs to be processed by long running job  true) {
                // For long-running tasks (10 seconds or more) use Firebase Job Dispatcher.
                scheduleJob();
            } else {
                // Handle message within 10 seconds
                handleNow();
            }*/

        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
        sendNotification(remoteMessage.getData().get("title"), remoteMessage.getData().get("message"), remoteMessage.getData().get("message"), remoteMessage.getData().get("urls"), remoteMessage.getData().get("chennal"), remoteMessage.getData().get("channelname"), remoteMessage.getData().get("imgurlstr"),remoteMessage.getData().get("msg"),remoteMessage.getData().get("groupid"));
    }
    // [END receive_message]

    /**
     * Schedule a job using FirebaseJobDispatcher.
     */
    private void scheduleJob() {
        // [START dispatch_job]
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this));
        Job myJob = dispatcher.newJobBuilder()
                .setService(MyJobService.class)
                .setTag("my-job-tag")
                .build();
        dispatcher.schedule(myJob);
        // [END dispatch_job]
    }

    /**
     * Handle time allotted to BroadcastReceivers.
     */
    private void handleNow() {
        Log.d(TAG, "Short lived task is done.");
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private void sendNotification(String title, String messageBody, CharSequence longMessage, String url,String channelId,String channelName, String imgurlstr,String msg, String groupId) {
        Log.d(TAG, "sendNotification: " + PRRUN.bAppRunned);
        if(PRRUN.bAppRunned==false) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Bundle bundle = new Bundle();
            bundle.putString("url", url);
            intent.putExtras(bundle);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            if (imgurlstr != null) {
                try {
                    URL photos = new URL(imgurlstr);
                    HttpURLConnection connection = (HttpURLConnection) photos.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream bis = connection.getInputStream();
                    bigPicture = BitmapFactory.decodeStream(bis);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //String channelId = getString(R.string.default_notification_channel_id);
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            NotificationCompat.Builder notificationBuilder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationBuilder =
                        new NotificationCompat.Builder(this, channelId);
            } else {
                notificationBuilder =
                        new NotificationCompat.Builder(this);

            }
            notificationBuilder.setSmallIcon(R.drawable.ic_stat_name)
                    .setContentTitle(title)
                    .setContentText(messageBody)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setVibrate(new long[]{0,1500,1000,300,3000,200});


            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(longMessage));

            if (imgurlstr != null) {
                notificationBuilder.setLargeIcon(bigPicture);
            }

            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG");
            wakelock.acquire(5000);

            notificationManager.notify(0, notificationBuilder.build());

            SharedPreferences pref = getSharedPreferences("badge_count", MODE_PRIVATE);

            int badge = pref.getInt("badge_count", 0);

            if (badge > 0) {
                badgeCount = badge + 1;
            } else {
                badgeCount++;
            }

            Intent badgeIntent = new Intent("android.intent.action.BADGE_COUNT_UPDATE");
            badgeIntent.putExtra("badge_count", badgeCount);
            badgeIntent.putExtra("badge_count_package_name", getPackageName());
            badgeIntent.putExtra("badge_count_class_name", SplashActivity.class.getName());
            sendBroadcast(badgeIntent);

            SharedPreferences.Editor editor = pref.edit();
            editor.putInt("badgeCount", badgeCount);
            editor.commit();
        }else{
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALL);
            AudioAttributes att = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();
            String newId = "newChat";
            String newName = "실행중채팅알림";
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel buyChannel = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                buyChannel = new NotificationChannel(newId, newName, NotificationManager.IMPORTANCE_HIGH);
                buyChannel.setSound(defaultSoundUri, att);
                buyChannel.setDescription("앱실행중 채팅 설정 입니다.");
                buyChannel.enableVibration(true);
                buyChannel.setVibrationPattern(new long[]{0, 0});

                notificationManager.createNotificationChannel(buyChannel);
            }
            if(!channelId.equals("chat_alarm_set")){
                Log.d(TAG, "sendNotification: not alarm");
                Intent intent = new Intent(this, MainActivity.class);
                intent.setAction(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Bundle bundle = new Bundle();
                bundle.putString("url", url);
                intent.putExtras(bundle);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,PendingIntent.FLAG_UPDATE_CURRENT);
                if (imgurlstr != null) {
                    try {
                        URL photos = new URL(imgurlstr);
                        HttpURLConnection connection = (HttpURLConnection) photos.openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        InputStream bis = connection.getInputStream();
                        bigPicture = BitmapFactory.decodeStream(bis);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                //String channelId = getString(R.string.default_notification_channel_id);
                //Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                NotificationCompat.Builder notificationBuilder;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    notificationBuilder =
                            new NotificationCompat.Builder(this, channelId);
                } else {
                    notificationBuilder =
                            new NotificationCompat.Builder(this);

                }
                notificationBuilder.setSmallIcon(R.drawable.ic_stat_name)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setContentIntent(pendingIntent)
                        .setFullScreenIntent(pendingIntent, true)
                        .setAutoCancel(true)
                        .setSound(null)
                        .setVibrate(null);

                //NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                //notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(longMessage));

                /*if (imgurlstr != null) {
                    notificationBuilder.setLargeIcon(bigPicture);
                }*/

                /*PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
                PowerManager.WakeLock wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG");
                wakelock.acquire(5000);*/

                notificationManager.notify(0, notificationBuilder.build());
            }else{
                //todo: 이전 메시지가 언제 왔는지 체크후 얼마 되지 않았으면 그냥 무시 groupid로 체크?
                //현재시간을 가져온다
                long now = System.currentTimeMillis();
                Date date = new Date(now);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
                String nowDate = sdf.format(date);

                SharedPreferences pref = getSharedPreferences("chatAlarm", MODE_PRIVATE);
                String groupchk = pref.getString(groupId, "");
                Log.d(TAG, "sendNotification: " + groupchk);

                long min = 1;
                if(groupchk=="" || groupchk==null) {
                    SharedPreferences sharedPreferences = getSharedPreferences("chatAlarm", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(groupId, nowDate);
                    editor.commit();
                }else{
                    SharedPreferences sharedPreferences = getSharedPreferences("chatAlarm", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(groupId, nowDate);
                    editor.commit();
                    //현재시간과 받은시간을 계산 1분이내인지 파악
                    try {
                        //String date1 = sdf.format(groupchk);
                        Date startDate = sdf.parse(groupchk);
                        Date endDate = sdf.parse(nowDate);

                        long diff = endDate.getTime() - startDate.getTime();
                        min = diff / (60 * 1000);
                        //long hour = diff / (60 * 60 * 1000);

                        Log.d(TAG, "diff : " + min + "//"+ startDate.toString() + "//" + endDate.toString());
                    }catch (ParseException e){
                        e.printStackTrace();
                    }
                }


                if(min < 1){//1분 안이라면 스킵

                }else {


                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setAction(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_LAUNCHER);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Bundle bundle = new Bundle();
                    bundle.putString("url", url);
                    intent.putExtras(bundle);
                    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                    //String channelId = getString(R.string.default_notification_channel_id);
                    NotificationCompat.Builder notificationBuilder;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        notificationBuilder = new NotificationCompat.Builder(this, "newChat");
                    } else {
                        notificationBuilder = new NotificationCompat.Builder(this);
                    }
                    notificationBuilder.setSmallIcon(R.drawable.ic_stat_name)
                            .setContentTitle(title)
                            .setContentText(msg)
                            .setContentIntent(pendingIntent)
                            .setFullScreenIntent(pendingIntent, true)
                            .setAutoCancel(true)
                            .setSound(null)
                            .setVibrate(null);

                    //PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
                    //PowerManager.WakeLock wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG");
                    //wakelock.acquire(5000);

                    notificationManager.notify(0, notificationBuilder.build());
                }
            }
        }
    }
}
