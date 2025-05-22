package com.example.remo

// Add these imports
import android.app.NotificationManager // Correct import for NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
// Remove android.widget.Toast if you are no longer using it here
import androidx.core.app.NotificationCompat // Use NotificationCompat for compatibility

class ReminderReceiver : BroadcastReceiver() {

     companion object {
        private const val TAG = "ReminderReceiver"
        private const val NOTIFICATION_ID = 2002 // Unique ID for this notification type
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderText = intent.getStringExtra("REMINDER_TEXT") ?: "Reminder!"
        Log.d(TAG, "Alarm triggered! Reminder: $reminderText")

        showNotification(context, reminderText)
    }

    private fun showNotification(context: Context, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to launch MainActivity when notification is tapped
        val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // PendingIntent flags considering API 31+ (Android 12) mutability requirements
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, mainActivityIntent, pendingIntentFlags)


        // Build the notification
        // MainActivity.CHANNEL_ID refers to the const val in MainActivity's companion object
        val builder = NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // Replace with your actual notification icon
            .setContentTitle("Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // For heads-up notification
            .setContentIntent(pendingIntent) // Set the intent that will fire when the user taps the notification
            .setAutoCancel(true) // Automatically removes the notification when the user taps it
        // .setDefaults(NotificationCompat.DEFAULT_ALL) // For default sound, vibration, etc.

        // Show the notification
        // Before Android 13, we don't need to check for POST_NOTIFICATIONS at the point of notifying
        // if it's declared in manifest. The check happens at runtime for API 33+.
        // The permission check will be in MainActivity before scheduling.
        notificationManager.notify(NOTIFICATION_ID, builder.build())
        Log.d(TAG, "Notification shown: $message")
    }
}