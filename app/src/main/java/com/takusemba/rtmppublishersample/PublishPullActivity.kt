package com.takusemba.rtmppublishersample

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.today.im.opus.PublisherListener
import com.today.im.opus.PublisherTask
import com.today.im.opus.PullerListener
import com.today.im.opus.PullerTask

class PublishPullActivity() : AppCompatActivity(), PublisherListener, PullerListener {
    private lateinit var container: RelativeLayout
    private lateinit var publishButton: Button
    private lateinit var pullButton: Button
    private lateinit var tvPublish: TextView
    private lateinit var tvPull: TextView
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
        tvPublish = findViewById<TextView>(R.id.tv_url_publish);
        tvPublish.setText(url);
        tvPull = findViewById<TextView>(R.id.tv_url_pull);
        tvPull.setText(url);

        audioManager = baseContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (url.isBlank()) {
            Toast.makeText(this, R.string.error_empty_url, Toast.LENGTH_SHORT)
                    .apply { setGravity(Gravity.CENTER, 0, 0) }
                    .run { show() }
        } else {
            publisherTask = PublisherTask(audioManager, this, url);

            publishButton.setOnClickListener {
                // 临时通话代码
                val intent = Intent(this@PublishPullActivity, CallerPersonalActivity::class.java)
                intent.putExtra("callType", 1)
                intent.putExtra("caller", "aaa")
                intent.putExtra("callerId", "1")
                intent.putExtra("callee", "bbb")
                intent.putExtra("calleeId", "2")
                startActivity(intent)

//                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
                //                publisherTask.setUrl(tvPublish.text.toString())
//                publisherTask.setUrl(url)
//                if (publisherTask.isPublishing) {
//                    publisherTask.stop()
//                } else {
//                    publisherTask.start()
//                }
            }

//            pullerTask = PullerTask(audioManager, this, url)
            pullButton.setOnClickListener {
                val intent2 = Intent(this@PublishPullActivity, CallerPersonalActivity::class.java)
                intent2.putExtra("callType", 2)
                intent2.putExtra("caller", "aaa")
                intent2.putExtra("callerId", "1")
                intent2.putExtra("callee", "209038")
                intent2.putExtra("calleeId", "209038")
                startActivity(intent2)
//                pullerTask.setUrl(url)
////                pullerTask.setUrl(tvPull.text.toString())
//                if (pullerTask.isPlaying) {
//                    pullerTask.stop()
//                } else {
//                    pullerTask.start()
//                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (url.isNotBlank()) {
            updatePublishControls()
        }
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
        tvLog.text = " s"
        tvLog.visibility = View.GONE
        thread?.interrupt()
    }

    override fun onPublishStarted() {
        Toast.makeText(this, R.string.started_publishing, Toast.LENGTH_SHORT)
                .apply { setGravity(Gravity.CENTER, 0, 0) }
                .run { show() }
        updatePublishControls()
        startCounting()
    }

    override fun onPublishStopped() {
        Toast.makeText(this, R.string.stopped_publishing, Toast.LENGTH_SHORT)
                .apply { setGravity(Gravity.CENTER, 0, 0) }
                .run { show() }
        updatePublishControls()
        stopCounting()
    }

    override fun onPublishDisconnected() {
        Toast.makeText(this, R.string.disconnected_publishing, Toast.LENGTH_SHORT)
                .apply { setGravity(Gravity.CENTER, 0, 0) }
                .run { show() }
        updatePublishControls()
        stopCounting()
    }

    override fun onPublishFailedToConnect() {
        Toast.makeText(this, R.string.failed_publishing, Toast.LENGTH_SHORT)
                .apply { setGravity(Gravity.CENTER, 0, 0) }
                .run { show() }
        updatePublishControls()
        stopCounting()
    }

    private fun updatePublishControls() {
        publishButton.text = getString(if (publisherTask.isPublishing) R.string.stop_publishing else R.string.start_publishing)
    }


    private fun Long.format(): String {
        return String.format("%02d", this)
    }

    override fun onPullStarted() {
        Toast.makeText(this, R.string.started_pulling, Toast.LENGTH_SHORT)
                .apply { setGravity(Gravity.CENTER, 0, 0) }
                .run { show() }
        updatePullControls()
        startCounting()
    }

    override fun onPullStopped() {
        Toast.makeText(this, R.string.stopped_pulling, Toast.LENGTH_SHORT)
                .apply { setGravity(Gravity.CENTER, 0, 0) }
                .run { show() }
        updatePullControls()
        stopCounting()
    }

    override fun onPullDisconnected() {
        Toast.makeText(this, R.string.disconnected_pulling, Toast.LENGTH_SHORT)
                .apply { setGravity(Gravity.CENTER, 0, 0) }
                .run { show() }
        updatePullControls()
        stopCounting()
    }

    override fun onPullFailedToConnect() {
        Toast.makeText(this, R.string.failed_pulling, Toast.LENGTH_SHORT)
                .apply { setGravity(Gravity.CENTER, 0, 0) }
                .run { show() }
        updatePullControls()
        stopCounting()
    }

    private fun updatePullControls() {
        pullButton.text = getString(if (pullerTask.isPlaying) R.string.stop_pulling else R.string.start_pulling)
    }
}