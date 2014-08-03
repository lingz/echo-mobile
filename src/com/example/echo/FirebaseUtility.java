package com.example.echo;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import android.util.Base64;
import android.util.Log;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

public class FirebaseUtility {

	private static final String url = "https://echokaffy.firebaseio.com/";
	private final Firebase myFirebaseRef, userFirebaseRef;
	private int transcriptionCounter = 0;
	private int serializedFileCounter = 0;
	
	public FirebaseUtility(String user) {
		myFirebaseRef = new Firebase(url);
		userFirebaseRef = myFirebaseRef.child(user);	
	}
	
	public void pushString(String toPush) {
		userFirebaseRef.child("transcriptionCounter").setValue(transcriptionCounter);
		userFirebaseRef.child("transcription"+transcriptionCounter).setValue(toPush);
		transcriptionCounter++;
	}
	
	public void pushSerializable(Serializable serial) {
		userFirebaseRef.child("serializedFileCounter").setValue(serializedFileCounter);
		String serialized = serialize(serial);
		userFirebaseRef.child("serializedFile"+serializedFileCounter).setValue(serialized);
		serializedFileCounter++;
	}
	
	public void pushSerializableBytes(byte[] bts) {
		String b64 = Base64.encodeToString(bts, Base64.DEFAULT);
		userFirebaseRef.child("serializedFile"+serializedFileCounter).setValue(b64);
		serializedFileCounter++;
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
