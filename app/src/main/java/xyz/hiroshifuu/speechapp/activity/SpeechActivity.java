package xyz.hiroshifuu.speechapp.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import retrofit2.Call;
import xyz.hiroshifuu.speechapp.commons.HttpUtil;
import xyz.hiroshifuu.speechapp.commons.PermissionHandler;
import xyz.hiroshifuu.speechapp.commons.SpeechRecognizerManager;
import xyz.hiroshifuu.speechapp.messages.MessageInput;
import xyz.hiroshifuu.speechapp.messages.MessagesList;
import xyz.hiroshifuu.speechapp.messages.MessagesListAdapter;

import xyz.hiroshifuu.speechapp.commons.AppUtils;
import xyz.hiroshifuu.speechapp.commons.Message;
import xyz.hiroshifuu.speechapp.R;
import xyz.hiroshifuu.speechapp.commons.ProperUtil;
import xyz.hiroshifuu.speechapp.commons.MessagesFixtures;
import xyz.hiroshifuu.speechapp.models.TextMessage;
import xyz.hiroshifuu.speechapp.utils.RetrofitClientInstance;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import static android.widget.Toast.makeText;

public class SpeechActivity extends DemoMessagesActivity
        implements RecognitionListener, MessageInput.InputListener,
        MessageInput.TypingListener,
        TextToSpeech.OnInitListener {

//    static {
//        System.loadLibrary("native-lib");
//    }

    private Properties my_property;

    private TextToSpeech tts;
    private String bus = "NO_BUS";
    public String res;
    private TextView textView; //Show location in textview
    private LocationManager locationManager; //instance to access location services
    private LocationListener locationListener;//listen for location changes

    private MessagesList messagesList;
    private SpeechRecognizerManager mSpeechManager;
    private MessageInput input;
    private Activity that;

    private HttpUtil httpUtil;

    private ImageButton callPhone;
    private int requestCode;
    private String[] permissions;
    private int[] grantResults;

    private static final String KWS_SEARCH = "wakeup";
    private static final String KEYPHRASE = "Bus";
    private static final String TAG = "Hot words keys: ";
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private static final String MENU_SEARCH = "menu";

    private SpeechRecognizer recognizer;
    private HashMap<String, Integer> captions;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.avtive_chat_dialog);

        my_property = ProperUtil.getPropertiesURL(getApplicationContext());
        String base_url = my_property.getProperty("serverUrl");
        String port = my_property.getProperty("port");
        String path = base_url + ":" + port + "/";
        Log.d("path", path);

        httpUtil = RetrofitClientInstance.getRetrofitInstance(path).create(HttpUtil.class);

        tts = new TextToSpeech(getApplicationContext(), this);

        this.messagesList = (MessagesList) this.findViewById(R.id.messagesList2);
        initAdapter();

        String welcome_info = "hello, how can I help you?";
        super.messagesAdapter.addToStart(
                MessagesFixtures.getTextMessage(welcome_info, "1"), true);
//        TTS_speak(welcome_info);

        input = (MessageInput) this.findViewById(R.id.input2);
        input.setInputListener(this);
        input.setTypingListener(this);

        // add hot words function
//        Intent intent = new Intent(this, VoiceCommandService.class);
//        intent.setAction(Intent.ACTION_ASSIST);
//        startService(intent);

        input.attachmentButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            if (PermissionHandler.checkPermission(that, PermissionHandler.RECORD_AUDIO)) {
                if (mSpeechManager == null) {
                    SetSpeechListener();
                } else if (!mSpeechManager.ismIsListening()) {
                    mSpeechManager.destroy();
                    SetSpeechListener();
                }
                //status_tv.setText(getString(R.string.you_may_speak));
                input.attachmentButton.setClickable(false);
                input.attachmentButton.getBackground().setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);

            } else {
                PermissionHandler.askForPermission(PermissionHandler.RECORD_AUDIO, that);
            }
            }
        });


        final String phoneNumber = my_property.getProperty("emergenceCall");
        callPhone  =  findViewById(R.id.call_phone);

        callPhone.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            public void onClick(View arg0) {
            Log.d("press call phone", "pressed");
            if (!hasPermission()) {
                Log.d("press call phone", "can not call ");
                int curApiVersion = Build.VERSION.SDK_INT;
                if (curApiVersion >= Build.VERSION_CODES.M) {
                    requestPermissions(
                        new String[] { Manifest.permission.CALL_PHONE },
                        0x11);
//                        intentToCall("85443713");
                } else {
                    intentToCall(phoneNumber);
                }
            } else {
                intentToCall(phoneNumber);
            }
            }
        });
        checkPermission();

        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }
        new SetupTask(this).execute();
        that = this;
    }

    private static class SetupTask extends AsyncTask<Void, Void, Exception> {
        WeakReference<SpeechActivity> activityReference;
        SetupTask(SpeechActivity activity) {
            this.activityReference = new WeakReference<>(activity);
        }
        @Override
        protected Exception doInBackground(Void... params) {
            try {
                Assets assets = new Assets(activityReference.get());
                File assetDir = assets.syncAssets();
                Log.d(TAG, "assetDir: " + String.valueOf(assetDir));
                activityReference.get().setupRecognizer(assetDir);
            } catch (IOException e) {
                return e;
            }
            return null;
        }
        @Override
        protected void onPostExecute(Exception result) {
            if (result != null) {
//                ((TextView) activityReference.get().findViewById(R.id.caption_text))
//                        .setText("Failed to init recognizer " + result);
                Log.d(TAG, "post result "+ result);
            } else {
                Log.d(TAG, "post result2 "+ result);
                activityReference.get().switchSearch(KWS_SEARCH);
            }
        }
    }

    private void switchSearch(String searchName) {
        recognizer.stop();
        Log.d(TAG, "switch name: "+ searchName);
        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(KWS_SEARCH))
            recognizer.startListening(searchName);
        else
            recognizer.startListening(searchName, 10000);

        Log.d(TAG, "switch name2: " + searchName);
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))

                .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)

                .getRecognizer();
        recognizer.addListener(this);

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);
        Log.d(TAG, "setting up phase keys ");
        // Create grammar-based search for selection between demos
//        File menuGrammar = new File(assetsDir, "menu.gram");
//        recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);
    }

    @Override
    public void onBeginningOfSpeech() {

    }

    /**
     * In partial result we get quick updates about current hypothesis. In
     * keyword spotting mode we can react here, in other modes we need to wait
     * for final result in onResult.
     */
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;
        String text = hypothesis.getHypstr();
        Log.d(TAG, "hypothesis: " + text);
        if (text.equals(KEYPHRASE))
            switchSearch(MENU_SEARCH);
        else
            switchSearch(KWS_SEARCH);
    }

    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(KWS_SEARCH))
            switchSearch(KWS_SEARCH);
    }

    @Override
    public void onResult(Hypothesis hypothesis) {

    }

    @Override
    public void onError(Exception e) {

    }

    @Override
    public void onTimeout() {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }

    private boolean hasPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    private void intentToCall(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_CALL);
        Uri data = Uri.parse("tel:" + phoneNumber);
        intent.setData(data);
        startActivity(intent);
    }

    @Override
    public boolean onSubmit(final CharSequence input, final String userID) throws IOException {
        super.messagesAdapter.addToStart(
                MessagesFixtures.getTextMessage(input.toString(), userID), true);


        ExecutorService executor = Executors.newCachedThreadPool();
        ResponseMessage response_Message = new ResponseMessage(input.toString(), bus);
        Future<String> result = executor.submit(response_Message);

//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        String response_str = "";
        try {
            response_str = result.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (response_str != "") {
            String[] response_strs = response_str.split("::\\\\n");
            for(int index = 0; index < response_strs.length; index++){
                String newText = response_strs[index].replace("\\n","\n");
                super.messagesAdapter.addToStart(
                        MessagesFixtures.getTextMessage(newText, "1"), true);
                if(index==0){
                    TTS_speak(newText);
                }
            }

        } else {
            Log.d("adapter error:", "can not get response info!");
        }

        return true;
    }

    class ResponseMessage implements Callable<String> {
        private String response_str;
        private String input;
        private String bus_id;

        ResponseMessage(String input, String bus_id) {
            this.input = input;
            this.bus_id = bus_id;
        }

        @Override
        public String call() {
            Call<TextMessage> textInfo = null;
            response_str = "";
            try {
                textInfo = httpUtil.getTextMessage(bus, input);
                response_str = textInfo.execute().body().getResponse_str();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response_str;
        }
    }

    private String SetSpeechListener() {
        res = "";
        Log.d("start speech", "start speech");
        mSpeechManager = new SpeechRecognizerManager(this, new SpeechRecognizerManager.onResultsReady() {
            @Override
            public void onResults(ArrayList<String> results) {
            if (results != null && results.size() > 0) {
                res = results.get(0);
                Log.d("res info00 : ", res);
                sendSoundInfo2(res);
            } else {
                Log.d(TAG, "speech listener is null");
                //status_tv.setText(getString(R.string.no_results_found));
            }
            mSpeechManager.destroy();
            mSpeechManager = null;
            input.attachmentButton.setClickable(true);
            input.attachmentButton.getBackground().setColorFilter(null);

            }
        });

        Log.d(TAG, "after sound");

        return res;
    }

    private void sendSoundInfo2(String info){
        Log.d("sound input", info);
        super.messagesAdapter.addToStart(
                MessagesFixtures.getTextMessage(info, "0"), true);
        sendSoundInfo(info);
    }
    private void sendSoundInfo(String info) {
//        Log.d("sound input", info);
//        super.messagesAdapter.addToStart(
//                MessagesFixtures.getTextMessage(info, "0"), true);
        ExecutorService executor = Executors.newCachedThreadPool();
        ResponseMessage response_Message = new ResponseMessage(info, bus);
        Future<String> result = executor.submit(response_Message);

//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        String response_str = "";
        try {
            response_str = result.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (response_str != "") {
            Log.d("textMessage", response_str);
            String[] response_strs = response_str.split("::\\\\n");
            for(int index=0; index < response_strs.length; index++){
                String newText = response_strs[index].replace("\\n","\n");
                super.messagesAdapter.addToStart(
                        MessagesFixtures.getTextMessage(newText, "1"), true);
                if(index==0){
                    TTS_speak(newText);
                }

            }
        } else {
            Log.d("adapter error:", "can not get response info!");
        }

    }

    private void initAdapter() {
        super.messagesAdapter = new MessagesListAdapter<>(super.senderId, super.imageLoader);
        super.messagesAdapter.enableSelectionMode(this);
        super.messagesAdapter.setLoadMoreListener(this);
        super.messagesAdapter.registerViewClickListener(R.id.messageUserAvatar,
            new MessagesListAdapter.OnMessageViewClickListener<Message>() {
                @Override
                public void onMessageViewClick(View view, Message message) {
                    AppUtils.showToast(SpeechActivity.this,
                            message.getUser().getName() + " avatar click",
                            false);
                }
            });
        this.messagesList.setAdapter(super.messagesAdapter);
    }

    @Override
    public void onStartTyping() {
        Log.v("Typing listener", getString(R.string.start_typing_status));
    }

    @Override
    public void onStopTyping() {
        Log.v("Typing listener", getString(R.string.stop_typing_status));
    }

    @Override
    protected void onPause() {
        if (mSpeechManager != null) {
            mSpeechManager.destroy();
            mSpeechManager = null;
        }
        super.onPause();
        if (tts != null) {
            tts.shutdown();
        }
    }

    @Override
    protected void onResume() {
        tts = new TextToSpeech(getApplicationContext(), this);
        super.onResume();
    }


    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            if (tts.isLanguageAvailable(Locale.UK) == TextToSpeech.LANG_AVAILABLE)
                tts.setLanguage(Locale.UK);
        } else if (status == TextToSpeech.ERROR) {
            Toast.makeText(this, "Sorry! Text To Speech failed...",
                    Toast.LENGTH_LONG).show();
        }
        Bundle b = getIntent().getExtras();
        if (b != null)
            bus = b.getString("bus");
        String welcome_info = "hello, how can I help you?";
//        TTS_speak("TTS is ready, Bus ID is : " + bus);
//        TTS_speak(welcome_info);
    }

    private void TTS_speak(String speech) {
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int amStreamMusicMaxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        am.setStreamVolume(AudioManager.STREAM_MUSIC, amStreamMusicMaxVol, 0);

        Bundle bundle = new Bundle();
        bundle.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
        bundle.putInt(TextToSpeech.Engine.KEY_PARAM_VOLUME, amStreamMusicMaxVol);

        tts.speak(speech, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void checkPermission() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);//initialise locationManager
        locationListener = new LocationListener() {//initialise locationlistenser
            @Override
            public void onLocationChanged(Location location) {//method check whenever location is updated
                textView.append("\n" + location.getLatitude() + " " + location.getLongitude());//append textview with location coordinate
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {//check if the GPS is turned off
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);//send user to setting interface
                startActivity(intent);
            }
        };
        //add user permission check
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.INTERNET
                }, 10);//request code is a integer, indicator for permission
            }

        }
    }

    @Override
    //handle the request permission result
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Recognizer initialization is a time-consuming and it involves IO,
                // so we execute it in async task
                new SetupTask(this).execute();
            } else {
                finish();
            }
        }
//        switch (requestCode) {
//            case 10://same as integer above
//                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                        locationManager.requestLocationUpdates("network", 1000, 0, locationListener);
//                        //configureButton();
//                    }
//                }
//        }
    }

}

