package io.agora.tutorials1v1vcall;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.agora.rtm.LocalInvitation;
import io.agora.rtm.RemoteInvitation;
import io.agora.rtm.RtmCallEventListener;
import io.agora.rtm.RtmCallManager;
import io.agora.rtm.RtmClient;
import io.agora.rtm.RtmClientListener;
import io.agora.rtm.RtmFileMessage;
import io.agora.rtm.RtmImageMessage;
import io.agora.rtm.RtmMediaOperationProgress;
import io.agora.rtm.RtmMessage;
import io.agora.rtm.SendMessageOptions;

public class ChatManager {
    private static final String TAG = ChatManager.class.getSimpleName();
    private static final String REMOTE_INVITATION = "remote_invitation";


    private VideoChatViewActivity activity;
    private LoginActivity loginActivity;

    private RemoteInvitation remoteInvitation;
    private LocalInvitation localInvitation;
    private Context mContext;
    private RtmClient mRtmClient;

    private RtmCallManager rtmCallManager;
    private SendMessageOptions mSendMsgOptions;
    private List<RtmClientListener> mListenerList = new ArrayList<>();
    private RtmMessagePool mMessagePool = new RtmMessagePool();


    public ChatManager(Context context) {
        mContext = context;
    }

    public void init() {
        String appID = mContext.getString(R.string.agora_app_id);

        try {
            mRtmClient = RtmClient.createInstance(mContext, appID, new RtmClientListener() {
                @Override
                public void onConnectionStateChanged(int state, int reason) {
                    for (RtmClientListener listener : mListenerList) {
                        listener.onConnectionStateChanged(state, reason);
                    }
                }

                @Override
                public void onMessageReceived(RtmMessage rtmMessage, String peerId) {
                    if (mListenerList.isEmpty()) {
                        // If currently there is no callback to handle this
                        // message, this message is unread yet. Here we also
                        // take it as an offline message.
                        mMessagePool.insertOfflineMessage(rtmMessage, peerId);
                    } else {
                        for (RtmClientListener listener : mListenerList) {
                            listener.onMessageReceived(rtmMessage, peerId);
                        }
                    }
                }

                @Override
                public void onImageMessageReceivedFromPeer(final RtmImageMessage rtmImageMessage, final String peerId) {
                    if (mListenerList.isEmpty()) {
                        // If currently there is no callback to handle this
                        // message, this message is unread yet. Here we also
                        // take it as an offline message.
                        mMessagePool.insertOfflineMessage(rtmImageMessage, peerId);
                    } else {
                        for (RtmClientListener listener : mListenerList) {
                            listener.onImageMessageReceivedFromPeer(rtmImageMessage, peerId);
                        }
                    }
                }

                @Override
                public void onFileMessageReceivedFromPeer(RtmFileMessage rtmFileMessage, String s) {

                }

                @Override
                public void onMediaUploadingProgress(RtmMediaOperationProgress rtmMediaOperationProgress, long l) {

                }

                @Override
                public void onMediaDownloadingProgress(RtmMediaOperationProgress rtmMediaOperationProgress, long l) {

                }

                @Override
                public void onTokenExpired() {

                }

                @Override
                public void onPeersOnlineStatusChanged(Map<String, Integer> status) {

                }
            });

            if (BuildConfig.DEBUG) {
                //mRtmClient.setParameters("{\"rtm.log_filter\": 65535}");
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            throw new RuntimeException("NEED TO check rtm sdk init fatal error\n" + Log.getStackTraceString(e));
        }
        //mRtmClient.setLogFileSize(mRtmClient.LOG_FILTER_ERROR);

        // Global option, mainly used to determine whether
        // to support offline messages now.
        mSendMsgOptions = new SendMessageOptions();

        rtmCallManager = mRtmClient.getRtmCallManager();
        rtmCallManager.setEventListener(new RtmCallEventListener() {
            @Override
            public void onLocalInvitationReceivedByPeer(LocalInvitation localInvitation) {
                Log.d(TAG, "被叫收到呼叫邀请:" + localInvitation.toString());
                ChatManager.this.localInvitation = localInvitation;
                Intent intent = new Intent(mContext, VideoChatViewActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("call", 1);//主叫
                mContext.startActivity(intent);
            }

            @Override
            public void onLocalInvitationAccepted(LocalInvitation localInvitation, String s) {
                Log.d(TAG, "onLocalInvitationAccepted 被叫已接受呼叫邀请:" + localInvitation.toString() + ", s : " + s);
                if (loginActivity != null) {
                    loginActivity.onUserAccept(localInvitation.getCalleeId());
                }
            }

            @Override
            public void onLocalInvitationRefused(LocalInvitation localInvitation, String s) {
                Log.d(TAG, "onLocalInvitationRefused 被叫已拒绝呼叫邀请:" + localInvitation.toString() + ", s : " + s);
                if (activity != null) {
                    activity.finish();
                }
            }

            @Override
            public void onLocalInvitationCanceled(LocalInvitation localInvitation) {
                Log.d(TAG, "onLocalInvitationCanceled 呼叫邀请已成功取消:" + localInvitation.toString());
            }

            @Override
            public void onLocalInvitationFailure(LocalInvitation localInvitation, int i) {
                Log.d(TAG, "onLocalInvitationFailure 发出的呼叫邀请过程失败:" + localInvitation.toString() + "errorcode :" + i);
            }

            @Override
            public void onRemoteInvitationReceived(RemoteInvitation remoteInvitation) {
                Log.d(TAG, "onRemoteInvitationReceived 收到一条呼叫邀请:" + remoteInvitation.toString());
                ChatManager.this.remoteInvitation = remoteInvitation;
                Intent intent = new Intent(mContext, VideoChatViewActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("call", 2);//被叫
                mContext.startActivity(intent);
            }

            @Override
            public void onRemoteInvitationAccepted(RemoteInvitation remoteInvitation) {
                Log.d(TAG, "onRemoteInvitationAccepted 接受呼叫邀请成功:" + remoteInvitation.toString());
            }

            @Override
            public void onRemoteInvitationRefused(RemoteInvitation remoteInvitation) {
                Log.d(TAG, "onRemoteInvitationRefused 拒绝呼叫邀请成功:" + remoteInvitation.toString());

            }

            @Override
            public void onRemoteInvitationCanceled(RemoteInvitation remoteInvitation) {
                Log.d(TAG, "onRemoteInvitationCanceled 主叫已取消呼叫邀请:" + remoteInvitation.toString());
                if (activity != null) {
                    activity.finish();
                }
            }

            @Override
            public void onRemoteInvitationFailure(RemoteInvitation remoteInvitation, int i) {
                Log.d(TAG, "onRemoteInvitationFailure 来自主叫的邀请过程失败:" + remoteInvitation.toString() + "errorCode :" + i);
            }
        });
    }

    public RtmClient getRtmClient() {
        return mRtmClient;
    }


    public RtmCallManager getRtmCallManager() {
        return rtmCallManager;
    }


    public RemoteInvitation getRemoteInvitation() {
        return remoteInvitation;
    }

    public LocalInvitation getLocalInvitation() {
        return localInvitation;
    }


    public void setActivity(VideoChatViewActivity activity) {
        this.activity = activity;
    }

    public void setLoginActivity(LoginActivity activity) {
        this.loginActivity = activity;
    }

    public void registerListener(RtmClientListener listener) {
        mListenerList.add(listener);
    }

    public void unregisterListener(RtmClientListener listener) {
        mListenerList.remove(listener);
    }

    public void enableOfflineMessage(boolean enabled) {
        mSendMsgOptions.enableOfflineMessaging = enabled;
    }

    public boolean isOfflineMessageEnabled() {
        return mSendMsgOptions.enableOfflineMessaging;
    }

    public SendMessageOptions getSendMessageOptions() {
        return mSendMsgOptions;
    }

    public List<RtmMessage> getAllOfflineMessages(String peerId) {
        return mMessagePool.getAllOfflineMessages(peerId);
    }

    public void removeAllOfflineMessages(String peerId) {
        mMessagePool.removeAllOfflineMessages(peerId);
    }
}
