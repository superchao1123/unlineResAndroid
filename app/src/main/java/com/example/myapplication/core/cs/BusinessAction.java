package com.example.myapplication.core.cs;

import android.os.RemoteException;

import com.example.myapplication.Config;
import com.example.myapplication.core.UnLineResManager;

public class BusinessAction extends IBusinessAction.Stub {
    @Override
    public void preStart() throws RemoteException {
        UnLineResManager.getInstance().preStart(Config.TEST_URL);
    }
}
