package io.agora.tutorials1v1vcall;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import io.agora.rtm.ErrorInfo;
import io.agora.rtm.LocalInvitation;
import io.agora.rtm.ResultCallback;
import io.agora.rtm.RtmCallManager;
import io.agora.rtm.RtmClient;

public class LoginActivity extends Activity {
    private final String TAG = LoginActivity.class.getSimpleName();
    private static final int PERMISSION_REQ_ID = 22;
    private TextView mLoginBtn;
    private EditText mUserIdEditText;
    private String mUserId;

    private TextView mCallBtn;
    private EditText mDesUserIdEt;

    private RtmCallManager rtmCallManager;
    private RtmClient mRtmClient;
    private boolean mIsInChat = false;
    private LocalInvitation invitation;
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mUserIdEditText = findViewById(R.id.user_id);
        mLoginBtn = findViewById(R.id.button_login);

        mCallBtn = findViewById(R.id.button_call);
        mDesUserIdEt = findViewById(R.id.user_des);

        ChatManager mChatManager = AGApplication.the().getChatManager();
        rtmCallManager = mChatManager.getRtmCallManager();
        mRtmClient = mChatManager.getRtmClient();

        checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) ;
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) ;
                checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID) ;
    }

    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
            return false;
        }

        return true;
    }

    public void onClickLogin(View v) {
        mUserId = mUserIdEditText.getText().toString();
        if (mUserId.equals("")) {
            showToast(getString(R.string.account_empty));
        } else if (mUserId.length() > 64) {
            showToast(getString(R.string.account_too_long));
        } else if (mUserId.startsWith(" ")) {
            showToast(getString(R.string.account_starts_with_space));
        } else if (mUserId.equals("null")) {
            showToast(getString(R.string.account_literal_null));
        } else {
            mLoginBtn.setEnabled(false);
            doLogin();
        }
    }

    /**
     * 呼叫
     * @param v
     */
    public void onClickCall(View v) {
        String desUserid = mDesUserIdEt.getText().toString();

        invitation = rtmCallManager.createLocalInvitation(desUserid);
        invitation.setChannelId("rtstest");
        rtmCallManager.sendLocalInvitation(invitation, new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "call send onSuccess");
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                Log.e(TAG, "call send fail : " + errorInfo.toString());
            }
        });
    }

    /**
     * 取消呼叫
     * @param v
     */
    public void onClickCancel(View v) {
       rtmCallManager.cancelLocalInvitation(invitation, new ResultCallback<Void>() {
           @Override
           public void onSuccess(Void aVoid) {
               Log.d(TAG, "cancel onSuccess");
           }

           @Override
           public void onFailure(ErrorInfo errorInfo) {
               Log.e(TAG, "cancel fail : " + errorInfo.toString());
           }
       });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLoginBtn.setEnabled(true);
    }

    /**
     * 登录
     */
    private void doLogin() {
        mIsInChat = true;
        mRtmClient.login(null, mUserId, new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void responseInfo) {
                Log.i(TAG, "login success");
                runOnUiThread(() -> {
                    showToast("login success");
                });
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                Log.i(TAG, "login failed: " + errorInfo.getErrorCode());
                runOnUiThread(() -> {
                    showToast("login fail");
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[1] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[2] != PackageManager.PERMISSION_GRANTED) {
            }
        }
    }

    /**
     * 退出登录
     */
    private void doLogout() {
        mRtmClient.logout(null);
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
