package com.example.remo

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build // Import Build for version checks
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import android.Manifest // For permission string
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
// No need to import java.util.Calendar if you're just using System.currentTimeMillis()

class MainActivity : AppCompatActivity(), View.OnClickListener {

     companion object {
        private const val TAG = "MainActivity"
        private const val ALARM_REQUEST_CODE = 1001
        const val CHANNEL_ID = "TIMER_REMINDER_CHANNEL"
        // --- MOVED TIME CONSTANTS HERE ---
        private const val FIVE_MINUTES_MS = 1 * 60 * 1000L
        private const val TEN_MINUTES_MS = 10 * 60 * 1000L
        private const val FIFTEEN_MINUTES_MS = 15 * 60 * 1000L
        private const val THIRTY_MINUTES_MS = 30 * 60 * 1000L
        private const val ONE_HOUR_MS = 60 * 60 * 1000L
        private const val TWO_HOURS_MS = 2 * 60 * 60 * 1000L
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1003 // For notification permission
    }

    private lateinit var btn5Min: MaterialButton
    private lateinit var btn10Min: MaterialButton
    private lateinit var btn15Min: MaterialButton
    private lateinit var btn30Min: MaterialButton
    private lateinit var btn1Hour: MaterialButton
    private lateinit var btn2Hours: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn5Min = findViewById(R.id.btn5Min)
        btn10Min = findViewById(R.id.btn10Min)
        btn15Min = findViewById(R.id.btn15Min)
        btn30Min = findViewById(R.id.btn30Min)
        btn1Hour = findViewById(R.id.btn1Hour)
        btn2Hours = findViewById(R.id.btn2Hours)

        btn5Min.setOnClickListener(this)
        btn10Min.setOnClickListener(this)
        btn15Min.setOnClickListener(this)
        btn30Min.setOnClickListener(this)
        btn1Hour.setOnClickListener(this)
        btn2Hours.setOnClickListener(this)

        // TODO LATER: Create Notification Channel here or in Application class (for Android 8+)
        createNotificationChannel()
        requestNotificationPermissionIfNeeded() // Request notification permission
    }
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // TIRAMISU is API 33
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission not granted, request it.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            } else {
                // Permission has already been granted
                Log.d(TAG, "POST_NOTIFICATIONS permission already granted.")
            }
        }
    }
    // Handle the result of the permission request (optional for this basic version,
    // but good practice for more complex flows)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "POST_NOTIFICATIONS permission granted by user.")
            } else {
                Toast.makeText(this, "Notification permission denied.", Toast.LENGTH_SHORT).show()
                Log.w(TAG, "POST_NOTIFICATIONS permission denied by user.")
                // You might want to explain to the user why the permission is needed
            }
        }
        // You would also handle SCHEDULE_EXACT_ALARM result here if you request it at runtime
    }
    override fun onClick(v: View?) {
        var durationMs = 0L
        var durationText = ""

        when (v?.id) {
            R.id.btn5Min -> {
                durationMs = FIVE_MINUTES_MS; durationText = "5 Minutes"
            }
            R.id.btn10Min -> {
                durationMs = TEN_MINUTES_MS; durationText = "10 Minutes"
            }
            R.id.btn15Min -> {
                durationMs = FIFTEEN_MINUTES_MS; durationText = "15 Minutes"
            }
            R.id.btn30Min -> {
                durationMs = THIRTY_MINUTES_MS; durationText = "30 Minutes"
            }
            R.id.btn1Hour -> {
                durationMs = ONE_HOUR_MS; durationText = "1 Hour"
            }
            R.id.btn2Hours -> {
                durationMs = TWO_HOURS_MS; durationText = "2 Hours"
            }
        }

        if (durationMs > 0) {
            // For the reminder text, let's be more specific for the notification
            val reminderNotificationText = "Time's up! Your $durationText reminder."
            val toastConfirmationText = "Reminder set for $durationText"

            Toast.makeText(this, toastConfirmationText, Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Selected duration: $durationText ($durationMs ms)")

            scheduleReminder(durationMs, reminderNotificationText)
        }
    }

    private fun scheduleReminder(durationMs: Long, reminderText: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("REMINDER_TEXT", reminderText)
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            ALARM_REQUEST_CODE,
            intent,
            pendingIntentFlags
        )

        val triggerAtMillis = System.currentTimeMillis() + durationMs

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { // KITKAT is API 19
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else { // For versions older than KitKat, set() is the best option
                alarmManager.set( // Less precise, but available
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Reminder scheduled: \"$reminderText\" at $triggerAtMillis")
            // Toast confirming actual scheduling might be better here or after successful try
            // Toast.makeText(this, "Reminder set!", Toast.LENGTH_SHORT).show(); // Already shown in onClick
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Missing SCHEDULE_EXACT_ALARM permission?", e)
            Toast.makeText(this, "Error: Permission required to set exact alarm.", Toast.LENGTH_LONG).show()
            // TODO: Guide user to grant SCHEDULE_EXACT_ALARM permission or handle gracefully
        }
    }

    // We'll create this function in a later step
    // private fun createNotificationChannel() { ... }
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name) // You'll need to add this string resource
            val descriptionText = getString(R.string.channel_description) // And this one
            val importance = NotificationManager.IMPORTANCE_HIGH // Set importance (HIGH makes it pop up)
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                // You can set other channel properties here, like lights, vibration, etc.
                // enableLights(true)
                // lightColor = Color.RED
                // enableVibration(true)
                // vibrationPattern = longArrayOf(100, 200, 300, 400, 500)
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created: $CHANNEL_ID")
        }
    }
}