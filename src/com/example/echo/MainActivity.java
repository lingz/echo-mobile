package com.example.echo;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;

import org.json.JSONException;
import org.json.JSONObject;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;

public class MainActivity extends Activity {

	MediaRecorder mrec ;
	File audiofile = null;
	private static final String TAG="SoundRecordingDemo";
	Firebase myFirebaseRef;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        myFirebaseRef = new Firebase("https://echokaffy.firebaseio.com/");
        myFirebaseRef.child("message").addValueEventListener(new ValueEventListener() {
    	    @Override
    	    public void onDataChange(DataSnapshot snapshot) {
    	        System.out.println(snapshot.getValue());  //prints "Do you have data? You'll love Firebase."
    	    }
    	    @Override public void onCancelled(FirebaseError error) { }
    	});
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    public void startRecording(View v) throws IOException {
    	mrec = new MediaRecorder();
    	mrec.setAudioSource(MediaRecorder.AudioSource.MIC);
    	mrec.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
    	mrec.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
    	System.out.println("start recording");
    	
    	if (audiofile == null) {
           File sampleDir = Environment.getExternalStorageDirectory();
           System.out.println(sampleDir);
           try { 
              audiofile = File.createTempFile("ibm", ".3gp", sampleDir);
           }
           catch (IOException e) {
               Log.e(TAG,"sdcard access error");
               return;
           }
       }
    	mrec.setOutputFile(audiofile.getAbsolutePath());
    	mrec.prepare();
    	mrec.start(); 
    }
    public void stopRecording(View v) throws MalformedURLException {
    	System.out.println("stopRecording");
       mrec.stop();
       mrec.reset();
       mrec.release();
       mrec = null;
       processaudiofile();
    }
    protected void processaudiofile() throws MalformedURLException { // Saves the audio file
       ContentValues values = new ContentValues(3);
       long current = System.currentTimeMillis();
       values.put(MediaStore.Audio.Media.TITLE, "audio" + audiofile.getName());
       values.put(MediaStore.Audio.Media.DATE_ADDED, (int) (current / 1000));
       values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/3gpp");
       values.put(MediaStore.Audio.Media.DATA, audiofile.getAbsolutePath());
       ContentResolver contentResolver = getContentResolver();
        
       Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
       Uri newUri = contentResolver.insert(base, values);
       System.out.println(audiofile.getAbsolutePath());
       
       //newUri = Uri.parse("http://4773af77.ngrok.com/test");
       
       System.out.println(newUri);
       sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, newUri));   
       myFirebaseRef.child("message").setValue("Do you have data? You'll love Firebase.");   
    }
    
}
