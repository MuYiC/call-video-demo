package io.agora.tutorials1v1vcall;

import android.app.Application;

public class AGApplication extends Application {
    private static AGApplication sInstance;
    private ChatManager mChatManager;


    public static AGApplication the() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

        mChatManager = new ChatManager(this);
        mChatManager.init();
    }

    public ChatManager getChatManager() {
        return mChatManager;
    }
}

