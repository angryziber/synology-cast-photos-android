package net.azib.photos.cast;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.os.Build;

import static android.app.Notification.VISIBILITY_PUBLIC;
import static android.content.Context.NOTIFICATION_SERVICE;

public class NotificationWithControls {
  MainActivity activity;
  NotificationManager notificationManager;

  public NotificationWithControls(MainActivity activity) {
    this.activity = activity;
    notificationManager = (NotificationManager) activity.getSystemService(NOTIFICATION_SERVICE);
  }

  public void notify(String text) {
    Intent resultIntent = new Intent(activity, activity.getClass());

    TaskStackBuilder stackBuilder = TaskStackBuilder.create(activity);
    stackBuilder.addParentStack(MainActivity.class);
    stackBuilder.addNextIntent(resultIntent);

    PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

    Notification.Builder builder = new Notification.Builder(activity)
        .setSmallIcon(R.drawable.ic_launcher)
        .setContentTitle("Cast Photos").setContentText(text)
        .setContentIntent(resultPendingIntent);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      builder.setVisibility(VISIBILITY_PUBLIC);
      builder.addAction(android.R.drawable.ic_media_previous, "Previous", resultPendingIntent);
      builder.addAction(android.R.drawable.ic_media_pause, "Pause", resultPendingIntent);
      builder.addAction(android.R.drawable.ic_media_next, "Next", resultPendingIntent);
      builder.setStyle(new Notification.MediaStyle().setShowActionsInCompactView(0, 1, 2));
    }

    Notification notification = builder.build();
    notificationManager.notify(1, notification);
  }
}
