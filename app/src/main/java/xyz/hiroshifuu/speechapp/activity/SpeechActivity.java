package xyz.hiroshifuu.speechapp.activity;


import android.Manifest;
//import android.support.v7.widget.RecyclerView;
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
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;

import xyz.hiroshifuu.speechapp.commons.PermissionHandler;
import xyz.hiroshifuu.speechapp.commons.SpeechRecognizerManager;
import xyz.hiroshifuu.speechapp.messages.MessageInput;
import xyz.hiroshifuu.speechapp.messages.MessagesList;
import xyz.hiroshifuu.speechapp.messages.MessagesListAdapter;

import xyz.hiroshifuu.speechapp.commons.AppUtils;
import xyz.hiroshifuu.speechapp.commons.Client;
import xyz.hiroshifuu.speechapp.commons.Message;
import xyz.hiroshifuu.speechapp.R;
import xyz.hiroshifuu.speechapp.commons.ProperUtil;
import xyz.hiroshifuu.speechapp.commons.MessagesFixtures;

public class SpeechActivity extends DemoMessagesActivity
        implements MessageInput.InputListener,
        MessageInput.TypingListener,
        TextToSpeech.OnInitListener {

    private Properties my_property;

    private static final int SERVERPORT = 5588;
    private static final String SERVER_IP = "3.0.6.160";

    Client myClient = null;

    private TextToSpeech tts;
    private String bus = "NO BUS";
    public String qryresp, res;
    private Button location; //Press to send location to server
    private TextView textView; //Show location in textview
    private LocationManager locationManager; //instance to access location services
    private LocationListener locationListener;//listen for location changes

    private MessagesList messagesList;
    private SpeechRecognizerManager mSpeechManager;
    private MessageInput input;
    private Activity that;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.avtive_chat_dialog);

        my_property = ProperUtil.getPropertiesURL(getApplicationContext());

        tts = new TextToSpeech(getApplicationContext(), this);

        this.messagesList = (MessagesList) this.findViewById(R.id.messagesList2);
        initAdapter();

        input = (MessageInput) this.findViewById(R.id.input2);
        input.setInputListener(this);
        input.setTypingListener(this);

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

        checkPermission();
        that = this;
    }

    @Override
    public boolean onSubmit(CharSequence input, String userID) {
        super.messagesAdapter.addToStart(
                MessagesFixtures.getTextMessage(input.toString(), userID), true);
        return true;
    }


    private String SetSpeechListener() {
        res = "";
        mSpeechManager = new SpeechRecognizerManager(this, new SpeechRecognizerManager.onResultsReady() {
            @Override
            public void onResults(ArrayList<String> results) {
                if (results != null && results.size() > 0) {
                    res = results.get(0);
                    Log.d("res info00 : ", res);
                    sendSoundInfo(res);
                } else {
                    //status_tv.setText(getString(R.string.no_results_found));
                }
                mSpeechManager.destroy();
                mSpeechManager = null;
                input.attachmentButton.setClickable(true);
                input.attachmentButton.getBackground().setColorFilter(null);

            }
        });
        return res;
    }

    private void sendSoundInfo(String info){
        super.messagesAdapter.addToStart(
                MessagesFixtures.getTextMessage(info, "0"), true);
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
//        TTS_speak("TTS is ready, Bus ID is : " + bus);
    }

    private void TTS_speak(String speech) {
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int amStreamMusicMaxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        am.setStreamVolume(AudioManager.STREAM_MUSIC, amStreamMusicMaxVol,0 );

        Bundle bundle = new Bundle();
        bundle.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
        bundle.putInt(TextToSpeech.Engine.KEY_PARAM_VOLUME, amStreamMusicMaxVol);

        tts.speak(speech, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void checkPermission(){
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
        switch (requestCode) {
            case 10://same as integer above
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        locationManager.requestLocationUpdates("network", 1000, 0, locationListener);
                        //configureButton();
                    }
                }
        }
    }

//    private void sendMessage(String text) {
//
//        listItems.add(new SpeechItem(text, true, false));
//        ((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();
//        try {
//            myClient = new Client(SERVER_IP, SERVERPORT, text);
//            qryresp = myClient.execute().get();
//        }
//        catch (InterruptedException e)
//        {
//            e.printStackTrace();
//        }
//        catch (ExecutionException e)
//        {
//            e.printStackTrace();
//        }
//        TTS_speak(qryresp);
//        listItems.add(new SpeechItem(qryresp, false, false));
//        ((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();
//
//        /* (To be used for google map)
//       SpeechItem item = new SpeechItem("https://www.google.com/maps/dir/?api=1&origin=Sembwang&destination=Clementi&travelmode=bus", false, true);
//       listItems.add(item);
//        */
//
//        final ScrollView scrollview = (findViewById(R.id.scrollview));
//        scrollview.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                scrollview.fullScroll(ScrollView.FOCUS_DOWN);
//            }
//        },100);
//    }


    private void configureButton() {
        location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String loc = textView.getText().toString();
                Client myClient = new Client(SERVER_IP, SERVERPORT, loc);//send location to sever
                myClient.execute();
            }
        });
    }
}

