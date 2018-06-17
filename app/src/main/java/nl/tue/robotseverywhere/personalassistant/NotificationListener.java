package nl.tue.robotseverywhere.personalassistant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import pma.PersonalMessagingAssistant;
import pma.chatparsers.MessageParser;
import pma.contact.Contact;
import pma.message.Message;

public class NotificationListener extends NotificationListenerService {

    private PersonalMessagingAssistant pa;
    private MessageParser mp;
    private int batchSize = 200;

    private String notificationChannelId = "PA-Channel01";
    private String notificationChannelName = "Personal Assistant Channel";

    @Override
    public void onCreate() {
        super.onCreate();

        mp = new MessageParser();
        pa = new PersonalMessagingAssistant(mp, batchSize);

        try {
            pa.getUserPreferences().load(getAssets().open("pa-network-storage/emre-es3.prefs.txt"));
            pa.getBayesianEvaluation().load(getAssets().open("pa-network-storage/emre-es3.bayesian.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        SharedPreferences sharedPref = this.getSharedPreferences("pa-prefs", Context.MODE_PRIVATE);
        if (!sharedPref.getBoolean("process_whatsapp", false)) {
            return;
        }


        Log.d("WORKING", sbn.getPackageName());
        if (sbn.getPackageName().equals("com.whatsapp")) {
            if (sbn.getTag() == null) {
                return;
            }

            Bundle extras = sbn.getNotification().extras;
            String title = extras.getString("android.title");
            String text = extras.getCharSequence("android.text").toString();

            cancelNotification(sbn.getKey());


            Message message = new Message(text, new Date().getTime(), new Contact("You", title));
            PersonalMessagingAssistant.EvalResult result = processMessage(message);
            message.setResult(result);

            if (result != PersonalMessagingAssistant.EvalResult.low) {
                showNotification(message);
            }
        }
    }

    private PersonalMessagingAssistant.EvalResult processMessage(Message m) {
        ArrayList<Message> messages = new ArrayList<>();
        messages.add(m);
        return pa.process(messages)[0];
    }

    private void showNotification(Message m) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel notificationChannel = new NotificationChannel(notificationChannelId, notificationChannelName, importance);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            notificationManager.createNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, notificationChannelId);
        String resultString = m.getResult().toString();
        mBuilder.setContentTitle(m.getSender().toString() + " (" + resultString + ")");
        mBuilder.setContentText(m.getOriginalText());
        mBuilder.setTicker("ticker");
        mBuilder.setSmallIcon(R.drawable.ic_assistant);
        mBuilder.setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int)(System.currentTimeMillis()/1000), mBuilder.build());
    }
}
