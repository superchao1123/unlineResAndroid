// IBusinessAction.aidl
package com.example.myapplication.core.cs;

import com.example.myapplication.core.cs.ResEntity;

interface IBusinessAction {
     int getPid();
     void preStart(String h5url);
     void loadAllRes(String h5url);
     void loadRes(String h5url, String resUrl);
     ResEntity getRes(String h5url, String resUrl);
}
