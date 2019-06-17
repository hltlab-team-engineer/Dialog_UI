package xyz.hiroshifuu.speechapp.service;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.voice.AlwaysOnHotwordDetector;
import android.service.voice.VoiceInteractionService;
import android.speech.SpeechRecognizer;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.Locale;

import javax.security.auth.callback.Callback;

import xyz.hiroshifuu.speechapp.activity.SpeechActivity;

public class VoiceCommandService extends VoiceInteractionService {
    private static final String TAG = "AlwaysOnHotwordDetector";
    Locale locale = new Locale("en-US");
    protected SpeechRecognizer mSpeechRecognizer;
    protected Intent mSpeechRecognizerIntent;

    public final Callback mHotwordCallback = new Callback() {
//        @Override
        public void onAvailabilityChanged(int status) {
            Log.i(TAG, "onAvailabilityChanged(" + status + ")");
            hotwordAvailabilityChangeHelper(status);
        }

//        @Override
        public void onDetected(AlwaysOnHotwordDetector.EventPayload eventPayload) {
            Log.i(TAG, "onDetected");
        }

//        @Override
        public void onError() {
            Log.i(TAG, "onError");
        }

//        @Override
        public void onRecognitionPaused() {
            Log.i(TAG, "onRecognitionPaused");
        }

//        @Override
        public void onRecognitionResumed() {
            Log.i(TAG, "onRecognitionResumed");
        }
    };

    private AlwaysOnHotwordDetector mHotwordDetector;

    @Override
    public void onCreate(){
        Log.d(TAG, "Entered on create");
        super.onCreate();
    }

    @Override
    public void onReady() {
        super.onReady();
        Log.i(TAG, "Creating " + this);
        mHotwordDetector = createAlwaysOnHotwordDetector(
                "Hello", Locale.forLanguageTag("en-US"), (AlwaysOnHotwordDetector.Callback) mHotwordCallback);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle args = new Bundle();
        args.putParcelable("intent", new Intent(this, SpeechActivity.class));
        showSession(args, 0);
        stopSelf(startId);
        return START_NOT_STICKY;
    }

    private void hotwordAvailabilityChangeHelper(int availability) {
        Log.i(TAG, "Hotword availability = " + availability);
        switch (availability) {
            case AlwaysOnHotwordDetector.STATE_HARDWARE_UNAVAILABLE:
                Log.i(TAG, "STATE_HARDWARE_UNAVAILABLE");
                break;
            case AlwaysOnHotwordDetector.STATE_KEYPHRASE_UNSUPPORTED:
                Log.i(TAG, "STATE_KEYPHRASE_UNSUPPORTED");
                break;
            case AlwaysOnHotwordDetector.STATE_KEYPHRASE_UNENROLLED:
                Log.i(TAG, "STATE_KEYPHRASE_UNENROLLED");
                Intent enroll = mHotwordDetector.createEnrollIntent();
                Log.i(TAG, "Need to enroll with " + enroll);
                break;
            case AlwaysOnHotwordDetector.STATE_KEYPHRASE_ENROLLED:
                Log.i(TAG, "STATE_KEYPHRASE_ENROLLED - starting recognition");
                if (mHotwordDetector.startRecognition(0)) {
                    Log.i(TAG, "startRecognition succeeded");
                } else {
                    Log.i(TAG, "startRecognition failed");
                }
                break;
        }
    }
}
