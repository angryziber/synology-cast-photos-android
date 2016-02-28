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
  Intent open, prev, pause, next;

  public NotificationWithControls(MainActivity activity) {
    this.activity = activity;
    notificationManager = (NotificationManager) activity.getSystemService(NOTIFICATION_SERVICE);

    open = new Intent(activity, activity.getClass());
    prev = new Intent(open).setAction("prev");
    pause = new Intent(open).setAction("pause");
    next = new Intent(open).setAction("next");
  }

  public void notify(String text) {
    TaskStackBuilder stackBuilder = TaskStackBuilder.create(activity);
    stackBuilder.addParentStack(MainActivity.class);
    stackBuilder.addNextIntent(open);

    PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

    Notification.Builder builder = new Notification.Builder(activity)
        .setSmallIcon(R.drawable.ic_launcher)
        .setContentTitle("Cast Photos").setContentText(text)
        .setContentIntent(resultPendingIntent);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      builder.setVisibility(VISIBILITY_PUBLIC);
      builder.addAction(android.R.drawable.ic_media_previous, "Previous", PendingIntent.getActivity(activity, 0, prev, 0));
      builder.addAction(android.R.drawable.ic_media_pause, "Pause", PendingIntent.getActivity(activity, 0, pause, 0));
      builder.addAction(android.R.drawable.ic_media_next, "Next", PendingIntent.getActivity(activity, 0, next, 0));
      builder.setStyle(new Notification.MediaStyle().setShowActionsInCompactView(0, 1, 2));
    }

    Notification notification = builder.build();
    notificationManager.notify(1, notification);
  }

  public void cancel() {
    notificationManager.cancelAll();
  }
}
