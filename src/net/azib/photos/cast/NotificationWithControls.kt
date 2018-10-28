package net.azib.photos.cast

import android.R.drawable
import android.app.*
import android.app.Notification.VISIBILITY_PUBLIC
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.media.session.MediaSession
import android.os.Build
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.support.v4.media.session.MediaSessionCompat

class NotificationWithControls(val activity: Activity) {
  val notificationManager = activity.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
  val open = Intent(activity, activity.javaClass)
  val prev = Intent(open).setAction("prev")
  val pause = Intent(open).setAction("prev")
  val next = Intent(open).setAction("next")

  fun notify(text: String, token: MediaSessionCompat.Token?) {
    val stackBuilder = TaskStackBuilder.create(activity)
    stackBuilder.addParentStack(MainActivity::class.java)
    stackBuilder.addNextIntent(open)

    val resultPendingIntent = stackBuilder.getPendingIntent(0, FLAG_UPDATE_CURRENT)

    val notification = Notification.Builder(activity).apply {
      setSmallIcon(R.drawable.ic_launcher)
      setContentTitle(activity.getString(R.string.app_name))
      setContentText(text)
      setContentIntent(resultPendingIntent)

      if (Build.VERSION.SDK_INT >= LOLLIPOP) {
        setVisibility(VISIBILITY_PUBLIC)
        addAction(drawable.ic_media_previous, "Previous", PendingIntent.getActivity(activity, 0, prev, 0))
        addAction(drawable.ic_media_pause, "Pause", PendingIntent.getActivity(activity, 0, pause, 0))
        addAction(drawable.ic_media_next, "Next", PendingIntent.getActivity(activity, 0, next, 0))
        setStyle(Notification.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)
            .setMediaSession(token?.token as? MediaSession.Token))
        setOngoing(true)
      }
    }.build()

    notificationManager.notify(1, notification)
  }

  fun cancel() {
    notificationManager.cancelAll()
  }
}
