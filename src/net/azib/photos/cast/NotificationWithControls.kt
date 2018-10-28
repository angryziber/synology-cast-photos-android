package net.azib.photos.cast

import android.R.drawable
import android.app.*
import android.app.Notification.VISIBILITY_PUBLIC
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION_CODES.LOLLIPOP

class NotificationWithControls(val activity: Activity) {
  val notificationManager = activity.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
  val open = Intent(activity, activity.javaClass)
  val prev = Intent(open).setAction("prev")
  val pause = Intent(open).setAction("prev")
  val next = Intent(open).setAction("next")

  fun notify(text: String) {
    val stackBuilder = TaskStackBuilder.create(activity)
    stackBuilder.addParentStack(MainActivity::class.java)
    stackBuilder.addNextIntent(open)

    val resultPendingIntent = stackBuilder.getPendingIntent(0, FLAG_UPDATE_CURRENT)

    val builder = Notification.Builder(activity).setSmallIcon(R.drawable.ic_launcher).setContentTitle("Cast Photos").setContentText(text).setContentIntent(resultPendingIntent)

    if (Build.VERSION.SDK_INT >= LOLLIPOP) {
      builder.setVisibility(VISIBILITY_PUBLIC)
      builder.addAction(drawable.ic_media_previous, "Previous", PendingIntent.getActivity(activity, 0, prev, 0))
      builder.addAction(drawable.ic_media_pause, "Pause", PendingIntent.getActivity(activity, 0, pause, 0))
      builder.addAction(drawable.ic_media_next, "Next", PendingIntent.getActivity(activity, 0, next, 0))
      builder.setStyle(Notification.MediaStyle().setShowActionsInCompactView(0, 1, 2))
    }

    val notification = builder.build()
    notificationManager.notify(1, notification)
  }

  fun cancel() {
    notificationManager.cancelAll()
  }
}
