package io.agora.tutorials1v1vcall;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.io.UnsupportedEncodingException;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;
import io.agora.rtm.ErrorInfo;
import io.agora.rtm.ResultCallback;
import io.agora.rtm.RtmCallManager;
import io.agora.uikit.logger.LoggerRecyclerView;

public class VideoChatViewActivity extends AppCompatActivity {
    private static final String TAG = VideoChatViewActivity.class.getSimpleName();


    // Permission WRITE_EXTERNAL_STORAGE is not mandatory
    // for Agora RTC SDK, just in case if you wanna save
    // logs to external sdcard.


    private int callId;

    private RtcEngine mRtcEngine;
    private boolean mCallEnd = true;
    private boolean mMuted;

    private FrameLayout mLocalContainer;
    private RelativeLayout mRemoteContainer;
    private SurfaceView mLocalView;
    private SurfaceView mRemoteView;

    private ImageView mDeclineBtn;
    private ImageView mCallBtn;
    private ImageView mMuteBtn;
    private ImageView mSwitchCameraBtn;
    private Button mOpenddoorBtn;

    // Customized logger view
    private LoggerRecyclerView mLogView;

    /**
     * 视频对讲事件回调
     */
    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {

        /**
         * 加入对讲频道成功的回调
         * @param channel
         * @param uid
         * @param elapsed
         */
        @Override
        public void onJoinChannelSuccess(String channel, final int uid, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLogView.logI("Join channel success, uid: " + (uid & 0xFFFFFFFFL));
                    mOpenddoorBtn.setVisibility(View.VISIBLE);
                }
            });
        }

        /**
         *
         * @param uid
         * @param state
         * @param reason
         * @param elapsed
         */
        @Override
        public void onRemoteVideoStateChanged(int uid, int state, int reason, int elapsed) {
            if (state == Constants.REMOTE_VIDEO_STATE_STARTING) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLogView.logI("First remote video decoded, uid: " + (uid & 0xFFFFFFFFL));
                        setupRemoteVideo(uid);
                    }
                });
            }
        }

        /**
         * 远端退出对讲
         * @param uid
         * @param reason 离线原因 USER_OFFLINE_QUIT(0)：用户主动离开
         * USER_OFFLINE_DROPPED(1)：因过长时间收不到对方数据包，超时掉线。注意：由于 SDK 使用的是不可靠通道，也有可能对方主动离开本方没收到对方离开消息而误判为超时掉线
         */
        @Override
        public void onUserOffline(final int uid, int reason) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLogView.logI("User offline, uid: " + (uid & 0xFFFFFFFFL));
                    //onRemoteUserLeft();
                    finish();
                }
            });
        }
    };

    /**
     * 设置所要接收远端视频的视图
     *
     * @param uid
     */
    private void setupRemoteVideo(int uid) {
        // Only one remote video view is available for this
        // tutorial. Here we check if there exists a surface
        // view tagged as this uid.
        int count = mRemoteContainer.getChildCount();
        View view = null;
        for (int i = 0; i < count; i++) {
            View v = mRemoteContainer.getChildAt(i);
            if (v.getTag() instanceof Integer && ((int) v.getTag()) == uid) {
                view = v;
            }
        }

        if (view != null) {
            return;
        }

        /*
          Creates the video renderer view.
          CreateRendererView returns the SurfaceView type. The operation and layout of the view
          are managed by the app, and the Agora SDK renders the view provided by the app.
          The video display view must be created using this method instead of directly
          calling SurfaceView.
         */
        mRemoteView = RtcEngine.CreateRendererView(getBaseContext());
        mRemoteContainer.addView(mRemoteView);
        // Initializes the video view of a remote user.
        mRtcEngine.setupRemoteVideo(new VideoCanvas(mRemoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
        mRemoteView.setTag(uid);
    }

    /**
     * 远端退出频道
     */
    private void onRemoteUserLeft() {
        removeRemoteVideo();
    }

    /**
     * 销毁远端视图
     */
    private void removeRemoteVideo() {
        if (mRemoteView != null) {
            mRemoteContainer.removeView(mRemoteView);
        }
        // Destroys remote view
        mRemoteView = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AGApplication.the().getChatManager().setActivity(this);
        setContentView(R.layout.activity_video_chat_view);
        Intent intent = getIntent();
        callId = intent.getIntExtra("call", 1);
        initUI();

    }

    private void initUI() {
        mLocalContainer = findViewById(R.id.local_video_view_container);
        mRemoteContainer = findViewById(R.id.remote_video_view_container);

        mDeclineBtn = findViewById(R.id.btn_decline);
        mCallBtn = findViewById(R.id.btn_call);
        mMuteBtn = findViewById(R.id.btn_mute);
        mSwitchCameraBtn = findViewById(R.id.btn_switch_camera);
        mOpenddoorBtn = findViewById(R.id.btn_opendoor);

        mLogView = findViewById(R.id.log_recycler_view);

        initEngineAndJoinChannel();

        if (callId == 1) {
            mDeclineBtn.setVisibility(View.GONE);
            mCallBtn.setImageResource(R.drawable.btn_endcall);
            showButtons(true);
            joinChannel();
        } else {
            mDeclineBtn.setVisibility(View.VISIBLE);
            mCallBtn.setImageResource(R.drawable.btn_startcall);
            showButtons(false);
        }

        showSampleLogs();
    }

    private void showSampleLogs() {
        mLogView.logI("Welcome to Agora 1v1 video call");
        mLogView.logW("You will see custom logs here");
        mLogView.logE("You can also use this to show errors");
    }

    private void initEngineAndJoinChannel() {
        initializeEngine();
        setupVideoConfig();
        setupLocalVideo();
    }

    /**
     * 初始化对讲引擎
     */
    private void initializeEngine() {
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), getString(R.string.agora_app_id), mRtcEventHandler);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    /**
     * 开启本地视频功能，并设置视频大小，帧率等
     */
    private void setupVideoConfig() {

        mRtcEngine.enableVideo();

        mRtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_640x360,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT));
    }

    /**
     * 创建本地视图并添加
     */
    private void setupLocalVideo() {
        // This is used to set a local preview.
        // The steps setting local and remote view are very similar.
        // But note that if the local user do not have a uid or do
        // not care what the uid is, he can set his uid as ZERO.
        // Our server will assign one and return the uid via the event
        // handler callback function (onJoinChannelSuccess) after
        // joining the channel successfully.
        mLocalView = RtcEngine.CreateRendererView(getBaseContext());
        mLocalView.setZOrderMediaOverlay(true);
        mLocalContainer.addView(mLocalView);
        // Initializes the local video view.
        // RENDER_MODE_HIDDEN: Uniformly scale the video until it fills the visible boundaries. One dimension of the video may have clipped contents.
        mRtcEngine.setupLocalVideo(new VideoCanvas(mLocalView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
    }

    /**
     * 加入频道
     */
    private void joinChannel() {
        // 1. Users can only see each other after they join the
        // same channel successfully using the same app id.
        // 2. One token is only valid for the channel name that
        // you use to generate this token.
        String token = getString(R.string.agora_access_token);
        if (TextUtils.isEmpty(token) || TextUtils.equals(token, "#YOUR ACCESS TOKEN#")) {
            token = null; // default, no token
        }

        if (callId == 1) {
            /**
             * 主叫端
             */
            mRtcEngine.joinChannel(token, AGApplication.the().getChatManager().getLocalInvitation().getChannelId(), "Extra Optional Data", 0);
        } else {

            /**
             * 被叫端
             */
            mRtcEngine.joinChannel(token, AGApplication.the().getChatManager().getRemoteInvitation().getChannelId(), "Extra Optional Data", 0);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!mCallEnd) {
            leaveChannel();
        }
        /*
          Destroys the RtcEngine instance and releases all resources used by the Agora SDK.

          This method is useful for apps that occasionally make voice or video calls,
          to free up resources for other operations when not making calls.
         */
        RtcEngine.destroy();
        AGApplication.the().getChatManager().setActivity(null);
    }

    private void leaveChannel() {
        mRtcEngine.leaveChannel();
    }

    /**
     * 麦克风设置
     *
     * @param view
     */
    public void onLocalAudioMuteClicked(View view) {
        mMuted = !mMuted;
        // Stops/Resumes sending the local audio stream.
        mRtcEngine.muteLocalAudioStream(mMuted);
        int res = mMuted ? R.drawable.btn_mute : R.drawable.btn_unmute;
        mMuteBtn.setImageResource(res);
    }

    /**
     * 开门
     * @param view
     */
    public void onOpenDoorClicked(View view) throws UnsupportedEncodingException {
        String opendoor = "cloud_message_opendoor";
        int messageId = mRtcEngine.createDataStream(true, true);
        int statuscode = mRtcEngine.sendStreamMessage(messageId, opendoor.getBytes("UTF-8"));
        if (statuscode == 0) {
            Log.d(TAG, "open door message send success");
        }
    }

    /**
     * 摄像头转换
     *
     * @param view
     */
    public void onSwitchCameraClicked(View view) {
        // Switches between front and rear cameras.
        mRtcEngine.switchCamera();
    }

    public void onCallClicked(View view) {
        if (mCallEnd) {
            startCall();
            mCallEnd = false;
            mCallBtn.setImageResource(R.drawable.btn_endcall);
            mDeclineBtn.setVisibility(View.GONE);
        } else {
            endCall();
            finish();
        }

        showButtons(!mCallEnd);
    }

    /**
     * 拒绝接听
     *
     * @param view
     */
    public void onDeclineClicked(View view) {
        Log.d(TAG, "onDeclineClicked");
        ChatManager mChatManager = AGApplication.the().getChatManager();
        RtmCallManager rtmCallManager = mChatManager.getRtmCallManager();
        rtmCallManager.refuseRemoteInvitation(mChatManager.getRemoteInvitation(), new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "refuse onSuccess");
                finish();
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                Log.d(TAG, errorInfo.toString());
            }
        });
    }


    /**
     * 接听并加入频道
     */
    private void startCall() {
        ChatManager mChatManager = AGApplication.the().getChatManager();
        RtmCallManager rtmCallManager = mChatManager.getRtmCallManager();
        rtmCallManager.acceptRemoteInvitation(mChatManager.getRemoteInvitation(), new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "accept onSuccess");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setupLocalVideo();
                        joinChannel();
                    }
                });
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                Log.d(TAG, errorInfo.toString());
            }
        });

    }

    private void endCall() {
        removeLocalVideo();
        removeRemoteVideo();
        leaveChannel();
    }

    private void removeLocalVideo() {
        if (mLocalView != null) {
            mLocalContainer.removeView(mLocalView);
        }
        mLocalView = null;
    }

    private void showButtons(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        mMuteBtn.setVisibility(visibility);
        mSwitchCameraBtn.setVisibility(visibility);
    }

}
