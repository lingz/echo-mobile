package com.example.echo;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends Activity {

    FirebaseUtility fbase;
    String admin = "admin";

    private Thread recordingThread = null;

    private static final int RECORDER_BPP = 16;
    private static final int RECORDER_SAMPLERATE = 16000;
    private static final int RECORDER_CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int RECORDER_CHANNELS_NUM = 1;
    private static final int RECORDER_BUFFER_SIZE = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT);


    private long recordingStartTime;
    private long lastEndTime;

    private int fragmentBufferLength = 0;

    private ArrayList<byte[]> fragmentBuffer = new ArrayList<byte[]>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        fbase = new FirebaseUtility("ABCD", admin);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    public void startRecording(View v) {

        recordingThread = new RecognizerThread(recordingStartTime);
        recordingThread.start();
    }


    public void stopRecording(View v) {
        recordingThread.interrupt();
        try {
            recordingThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        recordingThread = null;
    }




    private final class RecognizerThread extends Thread {

        private boolean isRecording = false;
        private AudioRecord recorder = null;

        public RecognizerThread(long recordingStartTime) {
            super();
        }


        @Override
        public void run() {

            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    RECORDER_SAMPLERATE, RECORDER_CHANNEL, RECORDER_AUDIO_ENCODING,
                    RECORDER_BUFFER_SIZE);

            int i = recorder.getState();
            if (i == 1)
                recorder.startRecording();

            isRecording = true;

            recordingStartTime = System.currentTimeMillis();
            lastEndTime = recordingStartTime;
            byte[] buffer = new byte[RECORDER_BUFFER_SIZE];


            while (!interrupted()) {
                int nread = recorder.read(buffer, 0, buffer.length);
                if (-1 == nread) {
                    throw new RuntimeException("error reading audio buffer");
                } else if (nread > 0) {

                    fragmentBuffer.add(Arrays.copyOfRange(buffer, 0, nread));
                    fragmentBufferLength += nread;
                }
            }

            byte[] soundSegmentData = processSoundSegment();
            fbase.pushSegment(soundSegmentData);
            recorder.stop();
            recorder.release();

        }

        private byte[] processSoundSegment() {


            byte[] flattenedRecordBuffer = new byte[fragmentBufferLength + 44];

            byte[] wavHeader = generateWaveFileHeader(fragmentBufferLength);

            System.arraycopy(wavHeader, 0, flattenedRecordBuffer, 0, 44);

            int bufferProgress = 44;

            for (byte[] miniBuffer : fragmentBuffer) {
                System.arraycopy(miniBuffer, 0, flattenedRecordBuffer, bufferProgress, miniBuffer.length);
                bufferProgress += miniBuffer.length;
            }

            long newEndTime = System.currentTimeMillis();

            lastEndTime = newEndTime;


            fragmentBufferLength = 0;
            fragmentBuffer = new ArrayList<byte[]>();

            System.out.println("DONE: " + flattenedRecordBuffer.length);

            return flattenedRecordBuffer;
        }
//
//        private byte[] shortArrayToByteArray(short[] input) {
//            int short_index, byte_index;
//            int iterations = input.length;
//
//            byte[] buffer = new byte[input.length * 2];
//
//            short_index = byte_index = 0;
//
//            for (; short_index != iterations; ) {
//                if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
//                    buffer[byte_index] = (byte) (input[short_index] & 0x00FF);
//                    buffer[byte_index + 1] = (byte) ((input[short_index] & 0xFF00) >> 8);
//                } else {
//                    buffer[byte_index] = (byte) ((input[short_index] & 0xFF00) >> 8);
//                    buffer[byte_index + 1] = (byte) (input[short_index] & 0x00FF);
//                }
//
//                short_index++;
//                byte_index += 2;
//            }
//
//            return buffer;
//        }


        private byte[] generateWaveFileHeader(long totalAudioLen) {

            long longSampleRate = RECORDER_SAMPLERATE;
            long totalDataLen = totalAudioLen + 44;
            int channels = 1;
            long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * RECORDER_CHANNELS_NUM / 8;

            byte[] header = new byte[44];

            header[0] = 'R';  // RIFF/WAVE header
            header[1] = 'I';
            header[2] = 'F';
            header[3] = 'F';
            header[4] = (byte) (totalDataLen & 0xff);
            header[5] = (byte) ((totalDataLen >> 8) & 0xff);
            header[6] = (byte) ((totalDataLen >> 16) & 0xff);
            header[7] = (byte) ((totalDataLen >> 24) & 0xff);
            header[8] = 'W';
            header[9] = 'A';
            header[10] = 'V';
            header[11] = 'E';
            header[12] = 'f';  // 'fmt ' chunk
            header[13] = 'm';
            header[14] = 't';
            header[15] = ' ';
            header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
            header[17] = 0;
            header[18] = 0;
            header[19] = 0;
            header[20] = 1;  // format = 1
            header[21] = 0;
            header[22] = (byte) channels;
            header[23] = 0;
            header[24] = (byte) (longSampleRate & 0xff);
            header[25] = (byte) ((longSampleRate >> 8) & 0xff);
            header[26] = (byte) ((longSampleRate >> 16) & 0xff);
            header[27] = (byte) ((longSampleRate >> 24) & 0xff);
            header[28] = (byte) (byteRate & 0xff);
            header[29] = (byte) ((byteRate >> 8) & 0xff);
            header[30] = (byte) ((byteRate >> 16) & 0xff);
            header[31] = (byte) ((byteRate >> 24) & 0xff);
            header[32] = (byte) (RECORDER_CHANNELS_NUM * 16 / 8);  // block align
            header[33] = 0;
            header[34] = RECORDER_BPP;  // bits per sample
            header[35] = 0;
            header[36] = 'd';
            header[37] = 'a';
            header[38] = 't';
            header[39] = 'a';
            header[40] = (byte) (totalAudioLen & 0xff);
            header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
            header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
            header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

            return header;

        }

    }
}




