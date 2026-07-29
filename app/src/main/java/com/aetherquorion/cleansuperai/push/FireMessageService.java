package com.aetherquorion.cleansuperai.push;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.aetherquorion.cleansuperai.R;
import com.aetherquorion.cleansuperai.SplashActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FireMessageService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        try {
            if (message.getNotification() != null) {
                createNotification(
                        message.getNotification().getTitle(),
                        message.getNotification().getBody(),
                        message.getNotification().getTitle()
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.e("FireMessageService", "onNewToken =" + token);
    }

    private final String chanleId = "10000";
    private final String chanleName = "chanel_bingel";

    private void createNotification(String messageTitle, String messageBody, String title) {
        try {
            Intent intent = new Intent(this, SplashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            int uniqueInt = (int) (System.currentTimeMillis() & 0xff);
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    : PendingIntent.FLAG_UPDATE_CURRENT;
            PendingIntent pendingIntent = PendingIntent.getActivity(this, uniqueInt, intent, flags);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder notificationBuilder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationBuilder = new NotificationCompat.Builder(this, chanleId);
                NotificationChannel channel = new NotificationChannel(chanleId, chanleName, NotificationManager.IMPORTANCE_HIGH);
                notificationManager.createNotificationChannel(channel);
            } else {
                notificationBuilder = new NotificationCompat.Builder(this);
            }
            if (messageTitle != null && !messageTitle.isEmpty()) {
                notificationBuilder.setContentTitle(messageTitle);
            } else {
                notificationBuilder.setContentTitle(getResources().getString(R.string.app_name));
            }
            if (messageBody != null && !messageBody.isEmpty()) {
                notificationBuilder.setContentText(messageBody);
            }
            notificationBuilder
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setAutoCancel(false)
                    .setWhen(System.currentTimeMillis())
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentTitle(title)
                    .setContentText(messageBody)
                    .setContentIntent(pendingIntent);
            notificationManager.notify(uniqueInt, notificationBuilder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
