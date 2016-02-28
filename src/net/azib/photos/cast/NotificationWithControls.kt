package net.azib.photos.cast

import android.app.Notification
import android.app.Notification.VISIBILITY_PUBLIC
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build

class NotificationWithControls(val activity: MainActivity) {
  val notificationManager = activity.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
  val open = Intent(activity, activity.javaClass)
  val prev = Intent(open).setAction("prev")
  val pause = Intent(open).setAction("prev")
  val next = Intent(open).setAction("next")

  fun notify(text: String) {
    val stackBuilder = TaskStackBuilder.create(activity)
    stackBuilder.addParentStack(MainActivity::class.java)
    stackBuilder.addNextIntent(open)

    val resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

    val builder = Notification.Builder(activity).setSmallIcon(R.drawable.ic_launcher).setContentTitle("Cast Photos").setContentText(text).setContentIntent(resultPendingIntent)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      builder.setVisibility(VISIBILITY_PUBLIC)
      builder.addAction(android.R.drawable.ic_media_previous, "Previous", PendingIntent.getActivity(activity, 0, prev, 0))
      builder.addAction(android.R.drawable.ic_media_pause, "Pause", PendingIntent.getActivity(activity, 0, pause, 0))
      builder.addAction(android.R.drawable.ic_media_next, "Next", PendingIntent.getActivity(activity, 0, next, 0))
      builder.setStyle(Notification.MediaStyle().setShowActionsInCompactView(0, 1, 2))
    }

    val notification = builder.build()
    notificationManager.notify(1, notification)
  }

  fun cancel() {
    notificationManager.cancelAll()
  }
}
