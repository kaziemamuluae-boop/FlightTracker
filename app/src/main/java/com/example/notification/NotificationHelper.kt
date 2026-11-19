package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
    const val CHANNEL_STATUS = "flight_status_alerts"
    const val CHANNEL_GATE = "gate_terminal_alerts"
    const val EXTRA_FLIGHT_CODE = "extra_flight_code"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val statusChannel = NotificationChannel(
            CHANNEL_STATUS,
            "Flight Status Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Departure, takeoff, arrival, landed, delay, and cancellation alerts"
        }
        val gateChannel = NotificationChannel(
            CHANNEL_GATE,
            "Gate & Terminal Changes",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Gate and terminal reassignment notifications"
        }
        manager.createNotificationChannel(statusChannel)
        manager.createNotificationChannel(gateChannel)
    }

    fun sendFlightAlertNotification(
        context: Context,
        flightCode: String,
        title: String,
        body: String,
        isGateOrTerminal: Boolean = false
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        ensureChannels(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_FLIGHT_CODE, flightCode)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            flightCode.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val channelId = if (isGateOrTerminal) CHANNEL_GATE else CHANNEL_STATUS
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(
            (flightCode + title).hashCode(),
            notification
        )
    }
}
