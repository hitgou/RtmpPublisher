package com.takusemba.rtmppublishersample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.rx2androidnetworking.Rx2AndroidNetworking;
import com.today.im.opus.PublisherListener;
import com.today.im.opus.PublisherTask;
import com.today.im.opus.PullerListener;
import com.today.im.opus.PullerTask;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.ObservableSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * 单人来电待接页面
 * Created by Colin on 2019/8/16.
 */
public class CallerPersonalActivity extends Activity implements PullerListener, PublisherListener {
    public static String TAG = "Voip";

    @BindView(R.id.iv_collapse)
    ImageView ivCollapse;

    @BindView(R.id.iv_caller_head)
    ImageView ivCallerHead;

    @BindView(R.id.iv_hands_free)
    ImageView ivHandsFree;

    @BindView(R.id.tv_caller_name)
    TextView tvCallerName;

    @BindView(R.id.tv_call_hint)
    TextView tvCallHint;

    @BindView(R.id.tv_calling_time)
    TextView tvCallingTime;

    @BindView(R.id.ll_mute)
    LinearLayout llMute;

    @BindView(R.id.ll_hangup)
    LinearLayout llHangup;

    @BindView(R.id.ll_callee_pickup)
    LinearLayout llCalleePickup;

    @BindView(R.id.ll_hands_free)
    LinearLayout llHandsFree;

    @BindView(R.id.tv_test_add)
    TextView tvTestAdd;

    @BindView(R.id.tv_test_subtract)
    TextView tvTestSubtract;

    @BindView(R.id.tv_test_did)
    TextView tvTestDid;

    /**
     * 呼叫类型，1-拨打、2-接听
     */
    int callType = 1;
    boolean isSpeakerphoneOn = false;
    int callStatus = 1; // 1-拨号中、2-通话中、3-待接电话、4-待接电话中
    int did = 0;
    String caller;
    String callerId;
    String callee;
    String calleeId;
    private static final int Tel_Caller_Dialing = 1;
    private static final int Tel_Caller_In_Calling = 2;
    private static final int Tel_Callee_For_Answer = 3;
//    static final int Tel_Calling = 1;

    private boolean isMuting = false;
    private boolean isCounting = false;
    private Thread thread;

    CompositeDisposable compositeDisposable = new CompositeDisposable();

    private PublisherTask publisherTask;
    private PullerTask pullerTask;
    private Handler handler = new Handler();
    private Handler myHandler = new MyHandler();
    private boolean isPublishing = false;
    private boolean isPulling = false;

    private AudioManager audioManager;

    private String voipCode = "791252931";

//    private String publishUrl = "rtmp://47.106.33.6:9936/voip/1752648526";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_caller_personal);
        ButterKnife.bind(this);

        audioManager = (AudioManager) getBaseContext().getSystemService(Context.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        } else {
            audioManager.setMode(AudioManager.MODE_IN_CALL);
        }

        setSpeakerphoneOn(isSpeakerphoneOn);

        publisherTask = new PublisherTask(audioManager, this, "");
        pullerTask = new PullerTask(audioManager, this, "");

        this.did = getInt(getBaseContext(), "did", 2475);
        this.tvTestDid.setText(did + "");

        Intent intent = getIntent();
        int callType = intent.getIntExtra("callType", 1);
        String caller = intent.getStringExtra("caller");
        String callerId = intent.getStringExtra("callerId");
        String callee = intent.getStringExtra("callee");
        String calleeId = intent.getStringExtra("calleeId");
        this.callType = callType;
        this.caller = caller;
        this.callerId = callerId;
        this.callee = callee;
        this.calleeId = calleeId;
        if (callType == 1) {
            this.callStatus = 1;
            startCall(callType, caller, callerId, callee, calleeId);
        } else {
            this.callStatus = 3;
        }
        updateControls();
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.dispose();
        super.onDestroy();
    }

    @OnClick({R.id.iv_collapse, R.id.ll_mute, R.id.ll_hangup, R.id.ll_callee_pickup, R.id.ll_hands_free,
            R.id.tv_test_add, R.id.tv_test_subtract})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.iv_collapse:
                finish();
                break;
            case R.id.ll_mute: // 测试，固定位接听 2
                swapMute();
                break;
            case R.id.ll_hangup: // 挂机
                stopCallAndCallee();
                break;
            case R.id.ll_callee_pickup: // 测试，固定位拨打 1
                startPickup(this.did, this.caller, this.callerId, this.callee, this.calleeId);
                break;
            case R.id.ll_hands_free:
                setSpeakerphoneOn(!isSpeakerphoneOn);
                if (isSpeakerphoneOn) {
                    ivHandsFree.setAlpha(1f);
                } else {
                    ivHandsFree.setAlpha(0.2f);
                }
                break;
            case R.id.tv_test_add:
                this.did = Integer.parseInt(tvTestDid.getText().toString());
                this.did++;
                tvTestDid.setText(this.did + "");
                putInt(getBaseContext(), "did", this.did);
                break;
            case R.id.tv_test_subtract:
                this.did = Integer.parseInt(tvTestDid.getText().toString());
                this.did--;
                tvTestDid.setText(this.did + "");
                putInt(getBaseContext(), "did", this.did);
                break;
        }
    }

    private void swapMute() {
        this.isMuting = !this.isMuting;
        publisherTask.setMute(this.isMuting);
    }

    private void setSpeakerphoneOn(boolean on) {
        isSpeakerphoneOn = on;
        if (on) {
            audioManager.setSpeakerphoneOn(true);
        } else {
            audioManager.setSpeakerphoneOn(false);//关闭扬声器
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                        audioManager.getStreamMaxVolume(AudioManager.MODE_IN_COMMUNICATION), AudioManager.FX_KEY_CLICK);
            } else {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                        audioManager.getStreamMaxVolume(AudioManager.MODE_IN_CALL), AudioManager.FX_KEY_CLICK);
            }
        }
    }

    public static String PREFERENCE_NAME = "TrineaAndroidCommon";

    public boolean putInt(Context context, String key, int value) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(key, value);
        return editor.commit();
    }

    public int getInt(Context context, String key, int defaultValue) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return settings.getInt(key, defaultValue);
    }

    public int startCall(final int callType, final String caller, String callerId, final String callee, String calleeId) {
        long timestamp = System.currentTimeMillis();
        String hashToken = Voip.getHashToken(caller, callerId, callee, calleeId, timestamp);
        String params = String.format(Locale.getDefault(), "caller=%s&caller_user_id=%s&callee=%s&callee_user_id=%s&timestamp=%d",
                caller, callerId, callee, calleeId, timestamp);
        // 启动对话
        Disposable disposable = Rx2AndroidNetworking.get(Voip.VOIP_ADDRESS + "/voip/start_session?" + params)
                .addHeaders("hashtoken", hashToken)
                .build()
                .getObjectObservable(VoipObject.class)
                .subscribeOn(Schedulers.io())
                .doOnNext(new Consumer<VoipObject>() {
                    @Override
                    public void accept(@NonNull VoipObject voipObject) throws Exception {
                        Log.d(TAG, "startCall voipObject = " + voipObject.toString());
                        did = voipObject.getDid();
                        // 通知服务器，有对话生成
                        Message message = Message.obtain();
                        message.obj = did + "";
                        myHandler.sendMessage(message);
                    }
                })
                .observeOn(Schedulers.io())
                .flatMap(new Function<VoipObject, ObservableSource<VoipObject>>() {
                    @Override
                    public ObservableSource<VoipObject> apply(@NonNull VoipObject sessionObject) throws Exception {
                        // 获取线路
                        if (sessionObject.getStatusCode() == Voip.OK) {
                            return getAccessPointObservableSource(sessionObject, callType, caller, callee);
                        }
                        return null;
                    }
                })
                .subscribe(
                        new Consumer<VoipObject>() {
                            @Override
                            public void accept(@NonNull VoipObject accessPoint) throws Exception {
                                if (accessPoint.getStatusCode() == 200) {
                                    publisherTask.setUrl(accessPoint.getPushUri(), voipCode);
//                                    publisherTask.setUrl(publishUrl);
                                    publisherTask.start();

                                    pullerTask.setUrl(accessPoint.getSubUri(), voipCode);
                                    pullerTask.start();
                                } else {

                                }

                                Log.e(TAG, "startCall accept: success ：" + accessPoint.toString());
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                Log.e(TAG, "startCall: accept error :" + throwable.getMessage());
                            }
                        }
                );
        compositeDisposable.add(disposable);

        return 1;
    }

    private ObservableSource<VoipObject> getAccessPointObservableSource(@NonNull VoipObject sessionObject, int callType, String caller, String callee) {
        String tokenString = String.format("%s-%s-%s", caller, sessionObject.getDid(), Voip.SALT);
        String params = String.format("caller=%s&did=%s", caller, sessionObject.getDid());
        String hashToken = Voip.md5(tokenString);
        return Rx2AndroidNetworking.get(Voip.VOIP_ADDRESS + "/voip/get_access_endpoint?" + params)
                .addHeaders("hashtoken", hashToken)
                .build()
                .getObjectObservable(VoipObject.class);
    }

    private int startPickup(int did, String caller, String callerId, String callee, String calleeId) {
        String tokenString;
        String params;
        tokenString = String.format("%s-%s-%s", callee, did, Voip.SALT);
        params = String.format("callee=%s&did=%s", callee, did);
        String hashToken = Voip.md5(tokenString);
        // 启动对话
        Disposable disposable = Rx2AndroidNetworking.get(Voip.VOIP_ADDRESS + "/voip/get_access_endpoint?" + params)
                .addHeaders("hashtoken", hashToken)
                .build()
                .getObjectObservable(VoipObject.class)
                .subscribeOn(Schedulers.io())
                .doOnNext(new Consumer<VoipObject>() {
                    @Override
                    public void accept(@NonNull VoipObject voipObject) throws Exception {
                        Log.d(TAG, "startPickup voipObject = " + voipObject.toString());
//                        pullerTask.setUrl();
                    }
                })
                .observeOn(Schedulers.io())
                .subscribe(
                        new Consumer<VoipObject>() {
                            @Override
                            public void accept(@NonNull VoipObject accessPoint) throws Exception {
                                publisherTask.setUrl(accessPoint.getPushUri(), voipCode);
                                publisherTask.start();

                                pullerTask.setUrl(accessPoint.getSubUri(), voipCode);
//                                pullerTask.setUrl(publishUrl);
                                pullerTask.start();

                                Log.e(TAG, "startPickup accept: success ：" + accessPoint.toString());
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                Log.e(TAG, "startPickup accept: error :" + throwable.getMessage());
                            }
                        }
                );
        compositeDisposable.add(disposable);

        return 1;
    }

    private void stopCallAndCallee() {
        pullerTask.stop();
        publisherTask.stop();
        finish();
    }

    @Override
    public void onPublishStarted() {
        callStatus = 2;
        startCounting();
        updateControls();
    }

    @Override
    public void onPublishStopped() {
        stopCounting();
        updateControls();

        if (!publisherTask.isPublishing() && !pullerTask.isPlaying()) {
            finish();
        }
    }

    @Override
    public void onPublishDisconnected() {
        stopCounting();
        updateControls();
        finish();
    }

    @Override
    public void onPublishFailedToConnect() {
        stopCounting();
        updateControls();
        finish();
    }

    @Override
    public void onPullStarted() {
        callStatus = 2;
        startCounting();
        updateControls();
    }

    @Override
    public void onPullStopped() {
        stopCounting();
        updateControls();
        if (!publisherTask.isPublishing() && !pullerTask.isPlaying()) {
            finish();
        }
    }

    @Override
    public void onPullDisconnected() {
        stopCounting();
        updateControls();
        finish();
    }

    @Override
    public void onPullFailedToConnect() {
        stopCounting();
        updateControls();
        finish();
    }

    private void updateControls() {
        switch (callStatus) {
            case Tel_Caller_Dialing:
                tvCallHint.setVisibility(View.VISIBLE);
                tvCallingTime.setVisibility(View.INVISIBLE);
                llMute.setVisibility(View.INVISIBLE);
                llHangup.setVisibility(View.VISIBLE);
                llHandsFree.setVisibility(View.INVISIBLE);
                llCalleePickup.setVisibility(View.INVISIBLE);

                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) llHangup.getLayoutParams();
                params.addRule(RelativeLayout.CENTER_HORIZONTAL);
                llHangup.setLayoutParams(params);
                break;
            case Tel_Callee_For_Answer:
                tvCallHint.setVisibility(View.VISIBLE);
                tvCallHint.setText(R.string.tel_callee_hint);

                tvCallingTime.setVisibility(View.INVISIBLE);
                llMute.setVisibility(View.INVISIBLE);

                RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) llHangup.getLayoutParams();
                params1.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                llHangup.setLayoutParams(params1);
                llHangup.setVisibility(View.VISIBLE);

                llHandsFree.setVisibility(View.INVISIBLE);
                llCalleePickup.setVisibility(View.VISIBLE);
                break;
            case Tel_Caller_In_Calling:
                tvCallHint.setVisibility(View.INVISIBLE);
                tvCallingTime.setVisibility(View.VISIBLE);
                llMute.setVisibility(View.VISIBLE);

                RelativeLayout.LayoutParams params2 = (RelativeLayout.LayoutParams) llHangup.getLayoutParams();
                params2.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
                params2.addRule(RelativeLayout.CENTER_HORIZONTAL);
                llHangup.setLayoutParams(params2);
                llHangup.setVisibility(View.VISIBLE);

                llHandsFree.setVisibility(View.VISIBLE);
                llCalleePickup.setVisibility(View.INVISIBLE);

                break;
        }
    }

    private void startCounting() {
        isCounting = true;
        tvCallingTime.setText(getString(R.string.tel_calling_time, "0", "0"));
        final long startedAt = System.currentTimeMillis();
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                long updatedAt = System.currentTimeMillis();
                while (isCounting) {
                    if (System.currentTimeMillis() - updatedAt > 1000) {
                        updatedAt = System.currentTimeMillis();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                long diff = System.currentTimeMillis() - startedAt;
                                long second = diff / 1000 % 60;
                                long min = diff / 1000 / 60;
                                tvCallingTime.setText(getString(R.string.tel_calling_time, min + "", second + ""));
                            }
                        });
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    private void stopCounting() {
        isCounting = false;
        tvCallingTime.setText("");
        tvCallingTime.setVisibility(View.GONE);
        if (thread != null && (thread.isAlive() || !thread.isInterrupted())) {
            thread.interrupt();
        }
    }

    class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String name = (String) msg.obj;
            tvTestDid.setText(name);
        }
    }
}
