package com.example.echo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import android.util.Base64;
import android.util.Log;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.MutableData;
import com.firebase.client.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

public class FirebaseUtility {

	private static final String url = "https://echo-transcript.firebaseio.com/";
    private final String userId = Math.round(Math.random()*10000000)+"";
	private final Firebase urlRef, jobQueue, dataQueue;
	private int count = 0;
    private String conferenceCode;
	
	public FirebaseUtility(String conferenceCode, String userId) {
		urlRef = new Firebase(url);
        jobQueue = urlRef.child("jobs");
        dataQueue = urlRef.child("data");
        this.conferenceCode = conferenceCode;
//        this.userId = userId;
	}

	public void pushSegment(byte[] segmentByteArray, long starttime, long endtime) {
        String data = speechFragmentToString(segmentByteArray);
        String uniqueId = conferenceCode+"_"+userId+"_"+count;
        Map<String, String> map = createJobPackage(starttime, endtime);
        map.put("dataId", uniqueId);
        jobQueue.push().setValue(map);
		dataQueue.child(uniqueId).setValue(data);
        count++;
    }

    private Map<String, String> createJobPackage(long starttime, long endtime)  {
        Map<String, String> jobPackage = new HashMap<String, String>();
        jobPackage.put("userId", userId);
        jobPackage.put("count", count+"");
        jobPackage.put("startTime", starttime+"");
        jobPackage.put("endTime", endtime+"");
        jobPackage.put("conferenceCode", conferenceCode);
        return jobPackage;
    }

	public static String speechFragmentToString(byte[] audioData) {
		String b64 = Base64.encodeToString(audioData, Base64.DEFAULT);
		return b64;
	}
	
	public static String serialize(Serializable serial) {
	    try {
	        ByteArrayOutputStream bo = new ByteArrayOutputStream();
	        ObjectOutputStream so = new ObjectOutputStream(bo);
	        so.writeObject(serial);
	        so.flush();
	        
	        // This encoding induces a bijection between byte[] and String (unlike UTF-8)
	        // Must also use this encoding when decoding the String
	        return Base64.encodeToString(bo.toByteArray(), Base64.DEFAULT);
	    } catch (Exception e) {
            Log.e(FirebaseUtility.class.getSimpleName(),
                    "Exception serializing serializable object", e);
	    }
	    
	    return null;
	}
	
}
