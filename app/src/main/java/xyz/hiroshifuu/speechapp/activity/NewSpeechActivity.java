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
import android.os.Handler;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import xyz.hiroshifuu.speechapp.common.Client;
import xyz.hiroshifuu.speechapp.adapter.CustomAdapter;
import xyz.hiroshifuu.speechapp.common.PermissionHandler;
import xyz.hiroshifuu.speechapp.R;
import xyz.hiroshifuu.speechapp.common.ProperUtil;
import xyz.hiroshifuu.speechapp.common.SpeechItem;
import xyz.hiroshifuu.speechapp.common.SpeechRecognizerManager;

public class NewSpeechActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int SERVERPORT = 5588;
    //    private static final String SERVER_IP = "3.1.160.222";
    private static final String SERVER_IP = "3.0.6.160";
    //private TextView status_tv;
//    private TextView result_tv;
    private EditText result_tv2;
    //    private TextView result_server_tv;
    private Button start_listen_btn;
    Client myClient = null;
    private SpeechRecognizerManager mSpeechManager;
    private TextToSpeech tts;
    //    private String bus = "No bus";
    private String bus = "95";
    public String qryresp, res;
    private Button location; //Press to send location to server
    private TextView textView; //Show location in textview
    private LocationManager locationManager; //instance to access location services
    private LocationListener locationListener;//listen for location changes
    private ListView listView;
    private ArrayList<SpeechItem> listItems;

    final Handler handler = new Handler();

    //new added
    // RecyclerView recyclerView;
    // MessageAdapter messageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.speech);
        findViews();
        setClickListeners();

        tts = new TextToSpeech(getApplicationContext(), this);
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
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        }


        listView = findViewById(R.id.list);
        listItems = new ArrayList<SpeechItem>();
        SpeechItem item;
        item = new SpeechItem("Hello, How can I assist you? Do you have queries related to? \n - Bus route \n -Surrounding places such as shopping malls, hospitals \n -Other rules and regulations", false, false);
        listItems.add(item);
        ArrayAdapter ad = new CustomAdapter(listItems, getApplicationContext());
        listView.setAdapter(ad);
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

    private void findViews() {
        //status_tv = findViewById(R.id.status_tv);

        //-----------------------------
        // recyclerView = findViewById(R.id.conversation);

        //new added above
//        result_tv = findViewById(R.id.result_tv);
        result_tv2 = findViewById(R.id.result_tv2);

        result_tv2.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage(result_tv2.getText().toString());
                    handled = true;
                    result_tv2.setText("");

                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(result_tv2.getWindowToken(), 0);
                }
                return handled;
            }
        });

//        result_server_tv = findViewById(R.id.result_server_tv);
        start_listen_btn = findViewById(R.id.start_listen_btn);
        //location = findViewById(R.id.location);
        // textView = findViewById(R.id.textView);//location results
    }

    private void setClickListeners() {
        final Activity that = this;
        start_listen_btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (PermissionHandler.checkPermission(that, PermissionHandler.RECORD_AUDIO)) {
                    switch (v.getId()) {
                        case R.id.start_listen_btn:
                            if (mSpeechManager == null) {
                                SetSpeechListener();
                            } else if (!mSpeechManager.ismIsListening()) {
                                mSpeechManager.destroy();
                                SetSpeechListener();
                            }
                            //status_tv.setText(getString(R.string.you_may_speak));
                            start_listen_btn.setClickable(false);
                            start_listen_btn.getBackground().setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
                            break;
                    }
                } else {
                    PermissionHandler.askForPermission(PermissionHandler.RECORD_AUDIO, that);
                }
            }
        });
    }

    private void sendMessage(String text) {

        listItems.add(new SpeechItem(text, true, false));
        ((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();
        try {
            myClient = new Client(SERVER_IP, SERVERPORT, text);
            qryresp = myClient.execute().get();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        catch (ExecutionException e)
        {
            e.printStackTrace();
        }
        TTS_speak(qryresp);
        listItems.add(new SpeechItem(qryresp, false, false));
        ((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();

        /* (To be used for google map)
       SpeechItem item = new SpeechItem("https://www.google.com/maps/dir/?api=1&origin=Sembwang&destination=Clementi&travelmode=bus", false, true);
       listItems.add(item);
        */

        final ScrollView scrollview = (findViewById(R.id.scrollview));
        scrollview.postDelayed(new Runnable() {
            @Override
            public void run() {
                scrollview.fullScroll(ScrollView.FOCUS_DOWN);
            }
        },100);
    }

    private void SetSpeechListener() {
        mSpeechManager = new SpeechRecognizerManager(this, new SpeechRecognizerManager.onResultsReady() {
            @Override
            public void onResults(ArrayList<String> results) {
                if (results != null && results.size() > 0) {
                    res = results.get(0);
                    sendMessage(res);
                } else {
                    //status_tv.setText(getString(R.string.no_results_found));
                }
                //status_tv.setText(getString(R.string.destroied));
                mSpeechManager.destroy();
                mSpeechManager = null;
                start_listen_btn.setClickable(true);
                start_listen_btn.getBackground().setColorFilter(null);
            }
        });
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
        TTS_speak("TTS is ready, Bus ID is : " + bus);
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
}

