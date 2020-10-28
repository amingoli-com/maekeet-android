package com.amingoli.markeet.fcm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import com.amingoli.markeet.ActivityDialogNotification;
import com.amingoli.markeet.R;
import com.amingoli.markeet.data.Constant;
import com.amingoli.markeet.data.DatabaseHandler;
import com.amingoli.markeet.data.SharedPref;
import com.amingoli.markeet.model.Notification;
import com.amingoli.markeet.utils.CallbackImageNotif;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

public class FcmMessagingService extends FirebaseMessagingService {

    private static int VIBRATION_TIME = 500; // in millisecond
    private SharedPref sharedPref;
    private DatabaseHandler db;

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        sharedPref = new SharedPref(this);
        sharedPref.setFcmRegId(s);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        sharedPref = new SharedPref(this);
        db = new DatabaseHandler(this);
        if (!sharedPref.getNotification()) return;
        Notification notification = null;
        if (remoteMessage.getData().size() > 0) {
            Object obj = remoteMessage.getData();
            String data = new Gson().toJson(obj);
            notification = new Gson().fromJson(data, Notification.class);
        } else if (remoteMessage.getNotification() != null) {
            notification = new Notification();
            RemoteMessage.Notification rn = remoteMessage.getNotification();
            notification.title = rn.getTitle();
            notification.content = rn.getBody();
        }

        if (notification == null) return;
        notification.id = System.currentTimeMillis();
        notification.created_at = System.currentTimeMillis();
        notification.read = false;

        // display notification
        prepareImageNotification(notification);

        // save notification to realm db
        saveNotification(notification);
    }

    private void prepareImageNotification(final Notification notif) {
        String image_url = null;
        if (notif.type.equals("PRODUCT")) {
            image_url = Constant.getURLimgProduct(notif.image);
        } else if (notif.type.equals("NEWS_INFO")) {
            image_url = Constant.getURLimgNews(notif.image);
        } else if (notif.type.equals("PROCESS_ORDER")) {
            // update order status
            db.updateStatusOrder(notif.code, notif.status);
        }
        if (image_url != null) {
            glideLoadImageFromUrl(this, image_url, new CallbackImageNotif() {
                @Override
                public void onSuccess(Bitmap bitmap) {
                    showNotification(notif, bitmap);
                }

                @Override
                public void onFailed(String string) {
                    Log.e("onFailed", string);
                    showNotification(notif, null);
                }
            });
        } else {
            showNotification(notif, null);
        }
    }

    private void showNotification(Notification notif, Bitmap bitmap) {
        Intent intent = ActivityDialogNotification.navigateBase(this, notif, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);

        String channelId = getString(R.string.default_notification_channel_id);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);
        builder.setContentTitle(notif.title);
        builder.setContentText(notif.content);
        builder.setSmallIcon(R.drawable.ic_notification);
        builder.setDefaults(android.app.Notification.DEFAULT_LIGHTS);
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            builder.setPriority(android.app.Notification.PRIORITY_HIGH);
        }
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(notif.content));
        if (bitmap != null) {
            builder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bitmap).setSummaryText(notif.content));
        }

        // display push notif
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
        int unique_id = (int) System.currentTimeMillis();
        notificationManager.notify(unique_id, builder.build());

        vibrationAndPlaySound();
    }

    private void vibrationAndPlaySound() {
        // play vibration
        if (sharedPref.getVibration()) {
            ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(VIBRATION_TIME);
        }
        // play tone
        RingtoneManager.getRingtone(this, Uri.parse(sharedPref.getRingtone())).play();
    }


    // load image with callback
    Handler mainHandler = new Handler(Looper.getMainLooper());
    Runnable myRunnable;

    private void glideLoadImageFromUrl(final Context ctx, final String url, final CallbackImageNotif callback) {

        myRunnable = new Runnable() {
            @Override
            public void run() {
                Glide.with(ctx).load(url).asBitmap().into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                        callback.onSuccess(resource);
                        mainHandler.removeCallbacks(myRunnable);
                    }

                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        callback.onFailed(e.getMessage());
                        super.onLoadFailed(e, errorDrawable);
                        mainHandler.removeCallbacks(myRunnable);
                    }
                });
            }
        };
        mainHandler.post(myRunnable);
    }

    private void saveNotification(Notification notification) {
        db.saveNotification(notification);
    }

}
