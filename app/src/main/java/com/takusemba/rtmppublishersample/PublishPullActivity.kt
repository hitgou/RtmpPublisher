package com.takusemba.rtmppublishersample

import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.View
import android.widget.*
import com.takusemba.rtmppublisher.PublisherListener
import com.takusemba.rtmppublisher.PublisherTask
import com.takusemba.rtmppublisher.PullerTask

class PublishPullActivity : AppCompatActivity(), PublisherListener {

    private lateinit var container: RelativeLayout
    private lateinit var publishButton: Button
    private lateinit var pullButton: Button
    private lateinit var tvLog: TextView

    private val url = BuildConfig.STREAMING_URL
    private val handler = Handler()
    private var thread: Thread? = null
    private var isCounting = false
    private lateinit var audioManager: AudioManager

    private lateinit var publisherTask: PublisherTask
    private lateinit var pullerTask: PullerTask

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_publish_pull)
        container = findViewById(R.id.container)
        publishButton = findViewById(R.id.toggle_publish)
        pullButton = findViewById(R.id.toggle_go_pull)
        tvLog = findViewById(R.id.tv_log)
        findViewById<TextView>(R.id.tv_url_publish).setText(url);
        findViewById<TextView>(R.id.tv_url_pull).setText(url);

        audioManager = getSystemService(AudioManager::class.java)

        if (url.isBlank()) {
            Toast.makeText(this, R.string.error_empty_url, Toast.LENGTH_SHORT)
                    .apply { setGravity(Gravity.CENTER, 0, 0) }
                    .run { show() }
        } else {
            publisherTask = PublisherTask(audioManager, this, url);

            publishButton.setOnClickListener {
                if (publisherTask.isPublishing) {
                    publisherTask.stop()
                } else {
                    publisherTask.start()
                }
            }

            pullerTask = PullerTask(audioManager, this, url)
            pullButton.setOnClickListener {
                if (pullerTask.isPlaying) {
                    pullerTask.stop()
                } else {
                    pullerTask.start()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (url.isNotBlank()) {
            updateControls()
        }
    }

    override fun onStarted() {
        Toast.makeText(this, R.string.started_publishing, Toast.LENGTH_SHORT)
                .apply { setGravity(Gravity.CENTER, 0, 0) }
                .run { show() }
        updateControls()
        startCounting()
    }

    override fun onStopped() {
        Toast.makeText(this, R.string.stopped_publishing, Toast.LENGTH_SHORT)
                .apply { setGravity(Gravity.CENTER, 0, 0) }
                .run { show() }
        updateControls()
        stopCounting()
    }

    override fun onDisconnected() {
        Toast.makeText(this, R.string.disconnected_publishing, Toast.LENGTH_SHORT)
                .apply { setGravity(Gravity.CENTER, 0, 0) }
                .run { show() }
        updateControls()
        stopCounting()
    }

    override fun onFailedToConnect() {
        Toast.makeText(this, R.string.failed_publishing, Toast.LENGTH_SHORT)
                .apply { setGravity(Gravity.CENTER, 0, 0) }
                .run { show() }
        updateControls()
        stopCounting()
    }

    private fun updateControls() {
        publishButton.text = getString(if (publisherTask.isPublishing) R.string.stop_publishing else R.string.start_publishing)
    }

    private fun startCounting() {
        isCounting = true
        tvLog.text = getString(R.string.publishing_label, 0L.format(), 0L.format())
        val startedAt = System.currentTimeMillis()
        var updatedAt = System.currentTimeMillis()
        thread = Thread {
            while (isCounting) {
                if (System.currentTimeMillis() - updatedAt > 1000) {
                    updatedAt = System.currentTimeMillis()
                    handler.post {
                        val diff = System.currentTimeMillis() - startedAt
                        val second = diff / 1000 % 60
                        val min = diff / 1000 / 60
                        tvLog.text = getString(R.string.publishing_label, min.format(), second.format())
                    }
                }
            }
        }
        thread?.start()
    }

    private fun stopCounting() {
        isCounting = false
        tvLog.text = ""
        tvLog.visibility = View.GONE
        thread?.interrupt()
    }

    private fun Long.format(): String {
        return String.format("%02d", this)
    }
}