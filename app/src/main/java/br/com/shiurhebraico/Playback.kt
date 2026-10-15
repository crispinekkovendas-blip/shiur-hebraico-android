package br.com.shiurhebraico

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Listening with the screen off (2026-10-15): while a lesson is open, a media notification (and the
 * lock-screen controls) keeps the app alive and lets the listener pause and play. The lesson screen
 * registers what play / pause do; the YouTube view keeps playing when the screen goes dark.
 */
object PlayerBridge {
    var onPlay: (() -> Unit)? = null
    var onPause: (() -> Unit)? = null
    var onBack10: (() -> Unit)? = null
    var onFwd10: (() -> Unit)? = null
}

class PlaybackService : Service() {
    private var session: MediaSessionCompat? = null
    private var title = ""
    private var who = ""
    private var playing = false
    private var position = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Aula tocando", NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) })
        }
        session = MediaSessionCompat(this, "shiur").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { PlayerBridge.onPlay?.invoke() }
                override fun onPause() { PlayerBridge.onPause?.invoke() }
                override fun onRewind() { PlayerBridge.onBack10?.invoke() }
                override fun onFastForward() { PlayerBridge.onFwd10?.invoke() }
                override fun onSkipToPrevious() { PlayerBridge.onBack10?.invoke() }
                override fun onSkipToNext() { PlayerBridge.onFwd10?.invoke() }
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> PlayerBridge.onPlay?.invoke()
            ACTION_PAUSE -> PlayerBridge.onPause?.invoke()
            ACTION_BACK -> PlayerBridge.onBack10?.invoke()
            ACTION_FWD -> PlayerBridge.onFwd10?.invoke()
            ACTION_UPDATE -> {
                title = intent.getStringExtra("title") ?: title
                who = intent.getStringExtra("who") ?: who
                playing = intent.getBooleanExtra("playing", playing)
                position = intent.getLongExtra("position", position)
            }
        }
        show()
        return START_NOT_STICKY
    }

    private fun pending(action: String, code: Int) =
        PendingIntent.getService(this, code, Intent(this, PlaybackService::class.java).setAction(action), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun show() {
        val s = session ?: return
        s.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, who)
                .build(),
        )
        s.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_REWIND or PlaybackStateCompat.ACTION_FAST_FORWARD or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_SKIP_TO_NEXT,
                )
                .setState(if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED, position, 1f)
                .build(),
        )
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(who)
            .setContentIntent(open)
            .setOngoing(playing)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_rew, "Voltar 10 s", pending(ACTION_BACK, 1))
            .addAction(
                if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (playing) "Pausar" else "Tocar",
                pending(if (playing) ACTION_PAUSE else ACTION_PLAY, 2),
            )
            .addAction(android.R.drawable.ic_media_ff, "Avançar 10 s", pending(ACTION_FWD, 3))
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(s.sessionToken).setShowActionsInCompactView(0, 1, 2))
            .build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) else startForeground(ID, n)
    }

    override fun onDestroy() {
        session?.run { isActive = false; release() }
        session = null
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "playback"
        private const val ID = 7
        const val ACTION_PLAY = "play"
        const val ACTION_PAUSE = "pause"
        const val ACTION_BACK = "back10"
        const val ACTION_FWD = "fwd10"
        const val ACTION_UPDATE = "update"

        fun update(ctx: Context, title: String, who: String, playing: Boolean, position: Double) {
            val i = Intent(ctx, PlaybackService::class.java).setAction(ACTION_UPDATE)
                .putExtra("title", title).putExtra("who", who).putExtra("playing", playing).putExtra("position", (position * 1000).toLong())
            runCatching { ContextCompat.startForegroundService(ctx, i) }
        }

        fun stop(ctx: Context) {
            runCatching { ctx.stopService(Intent(ctx, PlaybackService::class.java)) }
        }
    }
}
