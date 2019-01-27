package fergaral.datetophoto.utils

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat

import fergaral.datetophoto.R
import fergaral.datetophoto.activities.PhotosActivity
import fergaral.datetophoto.activities.StoragePermissionDeniedFloatingActivity
import fergaral.datetophoto.receivers.ActionCancelReceiver

/**
 * Created by fer on 4/07/15.
 */
class NotificationUtils(private val mContext: Context) {

    private var mNotifBuilder: NotificationCompat.Builder? = null
    private var mNotifManager: NotificationManager? = null
    private var mNotifStartTime: Long = 0

    init {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannels()
    }

    fun setUpNotification(onGoing: Boolean, hasActions: Boolean, contentText: String, tickerText: String): Notification {
        return setUpNotification(NOTIFICATION_ID, onGoing, hasActions, contentText, tickerText)
    }

    fun setUpNotification(notificationId: Int, onGoing: Boolean, hasActions: Boolean, contentText: String, tickerText: String): Notification {
        val resultIntent = Intent(mContext, PhotosActivity::class.java)
        val resultPendingIntent = PendingIntent.getActivity(
                mContext,
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)

        mNotifBuilder = NotificationCompat.Builder(mContext, GENERAL_CHANNEL_ID)

        val lollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

        mNotifBuilder!!.setSmallIcon(R.drawable.ic_dtp_transp)
                .setContentTitle("Date To Photo")
                .setContentText(contentText)
                .setContentIntent(resultPendingIntent)
                .setTicker(tickerText)
                .setOngoing(onGoing)
                .setAutoCancel(true)

        if (lollipop) {
            mNotifBuilder!!.color = Color.parseColor("#1976D2")
        }

        if (hasActions) {
            val cancelIntent = Intent(mContext, ActionCancelReceiver::class.java)

            val cancelPendingIntent = PendingIntent.getBroadcast(
                    mContext,
                    0,
                    cancelIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            )

            mNotifBuilder!!.addAction(R.drawable.ic_action_ic_clear_24px, "Cancelar", cancelPendingIntent)
        }

        mNotifManager = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = mNotifBuilder!!.build()
        mNotifManager!!.notify(notificationId, notification)
        return notification
    }

    fun showProgressNotification(startText: String): Notification {
        return setUpNotification(true, true, startText, "Procesando fotos...")
    }

    fun showSearchingPhotosNotification(text: String) {
        setUpNotification(true, false, text, text)
    }

    fun setNotificationProgress(total: Int, actual: Int) {
        if (mNotifBuilder != null && mNotifManager != null) {
            val time = System.currentTimeMillis()

            if (time - mNotifStartTime >= 500) {
                mNotifBuilder!!.setProgress(total, actual, false)
                mNotifManager!!.notify(NOTIFICATION_ID, mNotifBuilder!!.build())
                mNotifStartTime = time
            }
        }
    }

    fun endProgressNotification(endText: String) {
        if (mNotifBuilder != null && mNotifManager != null) {
            setUpNotification(false, false, endText, "El proceso ha finalizado")
        }
    }

    fun showStandAloneNotification(text: String) {
        val lollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

        val resultIntent = Intent(mContext, PhotosActivity::class.java)
        val resultPendingIntent = PendingIntent.getActivity(
                mContext,
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)

        val notifBuilder = NotificationCompat.Builder(mContext, GENERAL_CHANNEL_ID)
        notifBuilder.setContentTitle("Date To Photo")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_dtp_transp)
                .setTicker(text)
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true)

        val notifManager = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifManager.notify(NOTIFICATION_ID, notifBuilder.build())
    }

    fun showPermissionNotification() {
        val lollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

        val resultIntent = Intent(mContext, StoragePermissionDeniedFloatingActivity::class.java)
        val resultPendingIntent = PendingIntent.getActivity(
                mContext,
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)

        val notifBuilder = NotificationCompat.Builder(mContext, GENERAL_CHANNEL_ID)
        notifBuilder.setContentTitle("Date To Photo")
                .setContentText("Permiso necesario")
                .setSmallIcon(R.drawable.ic_dtp_transp)
                .setTicker("Permiso necesario")
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true)

        val notifManager = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifManager.notify(NOTIFICATION_ID, notifBuilder.build())
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createChannels() {
        val generalChannel = NotificationChannel(GENERAL_CHANNEL_ID,
                mContext.getString(R.string.channel_general),
                NotificationManager.IMPORTANCE_LOW)

        val notificationManager = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(generalChannel)
    }

    companion object {

        private val GENERAL_CHANNEL_ID = "fergaral_datetophoto_channel_general"
        val NOTIFICATION_ID = 1
    }
}
