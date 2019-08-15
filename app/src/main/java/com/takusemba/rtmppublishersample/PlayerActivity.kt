package com.takusemba.rtmppublishersample

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.View
import android.widget.*
import com.takusemba.rtmppublisher.PublisherListener
import com.takusemba.rtmppublisher.Puller

class PlayerActivity : AppCompatActivity(), PublisherListener {

    private lateinit var puller: Puller
    private lateinit var glView: GLSurfaceView
    private lateinit var container: RelativeLayout
    private lateinit var pullButton: Button
    private lateinit var backButton: Button
    private lateinit var cameraButton: ImageView
    private lateinit var label: TextView

    private val url = BuildConfig.STREAMING_URL
    private val handler = Handler()
    private var thread: Thread? = null
    private var isCounting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        glView = findViewById(R.id.surface_view)
        container = findViewById(R.id.container)
        pullButton = findViewById(R.id.toggle_pull)
        backButton = findViewById(R.id.toggle_back)
        cameraButton = findViewById(R.id.toggle_camera)
        label = findViewById(R.id.live_label)

        if (url.isBlank()) {
            Toast.makeText(this, R.string.error_empty_url, Toast.LENGTH_SHORT)
                    .apply { setGravity(Gravity.CENTER, 0, 0) }
                    .run { show() }
        } else {
            puller = Puller.Builder(this)
                    .setUrl(url)
                    .setAudioBitrate(Puller.Builder.DEFAULT_AUDIO_BITRATE)
                    .setPublisherListener(this)
                    .build()

            pullButton.setOnClickListener {
                if (puller.isPulling) {
                    puller.stopPulling()
                } else {
                    puller.startPulling()
                }
            }

            backButton.setOnClickListener {
                finish();
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
        pullButton.text = getString(if (puller.isPulling) R.string.stop_pull else R.string.start_pull)
    }

    private fun startCounting() {
        isCounting = true
        label.text = getString(R.string.publishing_label, 0L.format(), 0L.format())
        label.visibility = View.VISIBLE
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
                        label.text = getString(R.string.publishing_label, min.format(), second.format())
                    }
                }
            }
        }
        thread?.start()
    }

    private fun stopCounting() {
        isCounting = false
        label.text = ""
        label.visibility = View.GONE
        thread?.interrupt()
    }

    private fun Long.format(): String {
        return String.format("%02d", this)
    }
}