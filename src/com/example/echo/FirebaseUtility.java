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
    private final String userId = Math.round(Math.random())*10000000+"";
	private final Firebase urlRef, jobQueue, dataQueue;
	private int count = 0;
    private String conferenceCode;
	
	public FirebaseUtility(String conferenceCode, String userId) {
		urlRef = new Firebase(url);
        jobQueue = urlRef.child("jobs");
        dataQueue = urlRef.child("data");
        this.conferenceCode = conferenceCode;
        // this.userId = userId;
	}

	public void pushSegment(byte[] segmentByteArray) {
        String data = speechFragmentToString(segmentByteArray);
        jobQueue.push().setValue(conferenceCode+userId+count);
		dataQueue.child(conferenceCode+userId+count).setValue(data);
        count++;
    }

    private Map<String, String> createJobPackage()  {
        Map<String, String> jobPackage = new HashMap<String, String>();
        jobPackage.put("conferenceCode", conferenceCode);
        jobPackage.put("userId", userId);
        jobPackage.put("count", count+"");

        return jobPackage;
    }
//
//	public static Map<String, String> fragmentToMap(SpeechFragment fragment) {
//		Map<String, String> map = new HashMap<String, String>();
//		map.put("starttime", fragment.starttime);
//		map.put("endtime", fragment.endtime);
//		map.put("sound", speechFragmentToString(fragment.audioData));
//		map.put("words", fragment.speechText);
//		map.put("prob", fragment.probability);
//		return map;
//	}

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
