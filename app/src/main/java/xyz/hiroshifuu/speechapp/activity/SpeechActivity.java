package xyz.hiroshifuu.speechapp.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.stfalcon.chatkit.commons.models.IMessage;
import com.stfalcon.chatkit.commons.models.MessageContentType;

import org.tensorflow.lite.Interpreter;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import im.delight.android.location.SimpleLocation;
import io.github.ponnamkarthik.richlinkpreview.RichLinkView;
import io.github.ponnamkarthik.richlinkpreview.ViewListener;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import xyz.hiroshifuu.speechapp.commons.FloatTextView;
import xyz.hiroshifuu.speechapp.commons.HttpUtil;
import xyz.hiroshifuu.speechapp.commons.PermissionHandler;
import xyz.hiroshifuu.speechapp.commons.RecognizeCommands;
import xyz.hiroshifuu.speechapp.commons.SpeechRecognizerManager;
import xyz.hiroshifuu.speechapp.messages.MessageInput;
import xyz.hiroshifuu.speechapp.messages.MessagesList;
import xyz.hiroshifuu.speechapp.messages.MessagesListAdapter;

import xyz.hiroshifuu.speechapp.commons.AppUtils;
import xyz.hiroshifuu.speechapp.commons.Message;
import xyz.hiroshifuu.speechapp.R;
import xyz.hiroshifuu.speechapp.commons.ProperUtil;
import xyz.hiroshifuu.speechapp.commons.MessagesFixtures;
import xyz.hiroshifuu.speechapp.models.LocationMessage;
import xyz.hiroshifuu.speechapp.models.TextMessage;
import xyz.hiroshifuu.speechapp.utils.RetrofitClientInstance;

public class SpeechActivity extends DemoMessagesActivity
        implements MessageInput.InputListener,
        MessageInput.TypingListener,
        TextToSpeech.OnInitListener {

    private Properties my_property;

    private TextToSpeech tts;
    private static String bus = "NO_BUS";
    public String res;
    private TextView textView; //Show location in textview
    private LocationManager locationManager; //instance to access location services
    private LocationListener locationListener;//listen for location changes

    private MessagesList messagesList;
    private SpeechRecognizerManager mSpeechManager;
    private MessageInput input;
    private Activity that;

    private static HttpUtil httpUtil;

    private ImageButton callPhone;
    private int requestCode;
    private String[] permissions;
    private int[] grantResults;

    private static final int SAMPLE_RATE = 16000;
    private static final int SAMPLE_DURATION_MS = 800;
    private static final int RECORDING_LENGTH = (int) (SAMPLE_RATE * SAMPLE_DURATION_MS / 1000);
    private static final long AVERAGE_WINDOW_DURATION_MS = 1000;
    private static final float DETECTION_THRESHOLD = 0.50f;
    private static final int SUPPRESSION_MS = 500;
    private static final int MINIMUM_COUNT = 3;
    private static final long MINIMUM_TIME_BETWEEN_SAMPLES_MS = 30;
    private static final String LABEL_FILENAME = "file:///android_asset/conv_labels.txt";
    private static final String MODEL_FILENAME = "file:///android_asset/retrained_graph.tflite";
    private Interpreter tfLite;

    // UI elements.
    private final boolean wakeupflag = false;
    private static final int REQUEST_RECORD_AUDIO = 13;
    private static final String LOG_TAG = SpeechActivity.class.getSimpleName();

    // Working variables.
    short[] recordingBuffer = new short[RECORDING_LENGTH];
    int recordingOffset = 0;
    boolean shouldContinue = true;
    private Thread recordingThread;
    boolean shouldContinueRecognition = true;
    private Thread recognitionThread;
    private final ReentrantLock recordingBufferLock = new ReentrantLock();

    private List<String> labels = new ArrayList<String>();
    private List<String> displayedLabels = new ArrayList<>();
    private RecognizeCommands recognizeCommands = null;

    private final Timer scrollTimer = new Timer();
    //    private TimerTask scrollTask;
    private FloatTextView tv_scoll;
    private String scrollText = "welcome to bus!";

    private SimpleLocation location;
    private double latitude = -999;
    private double longitude = -999;

//    private static String TEST_LINK = "https://www.google.com/maps/dir/Sembawang/National+University+of+Singapore,+21+Lower+Kent+Ridge+Rd,+Singapore+119077/@1.3706059,103.727092,12z/data=!3m1!4b1!4m14!4m13!1m5!1m1!1s0x31da13622c24a83d:0x500f7acaedaa6d0!2m2!1d103.8184954!2d1.4491107!1m5!1m1!1s0x31da1a56784202d9:0x488d08d6c1f88d6b!2m2!1d103.7763939!2d1.2966426!3e3";
//    public static String TEST_LINK = "https://www.google.com/maps/dir/?api=1&origin=Sembwang&destination=Clementi&travelmode=transit";
    private Handler scrollHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            ExecutorService scrollExecutor = Executors.newCachedThreadPool();
            ResponseLocationMessage response_Message = new ResponseLocationMessage("input", bus);
            Future<LocationMessage> result = scrollExecutor.submit(response_Message);

            LocationMessage locationMessage = null;
            String textMessage = "";

            try {
                locationMessage = result.get();
                textMessage = locationMessage.getResponse_location();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (textMessage != "") {
                scrollText = textMessage;
                Log.d(LOG_TAG, textMessage);
                refreshUI(textMessage);

            } else {
                Log.d("adapter error:", "can not get location info!");
            }
            super.handleMessage(msg);
        }

        public void refreshUI(String scrollTexts) {
            Log.d(LOG_TAG, scrollTexts);
            tv_scoll.initScrollTextView(getWindowManager(), scrollTexts, 4);
            Log.d(LOG_TAG, "scrollText" + "1");
            tv_scoll.stopScroll();
            tv_scoll.starScroll();
        }
    };


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

        String welcome_info = "hello, I am your autonomus bus agent. How can I help you? What kind of information do you want to know?";
//        super.messagesAdapter.addToStart(
//                MessagesFixtures.getTextMessage(welcome_info, "1", TEST_LINK), true);
        super.messagesAdapter.addToStart(
                MessagesFixtures.getTextMessage(welcome_info, "1"), true);

        TTS_speak("Hi how can I help?");
//
//        String welcome_info1 = "To ask location: How can I go to changi airport? ";
//        super.messagesAdapter.addToStart(
//                MessagesFixtures.getTextMessage(welcome_info1, "1"), true);
//
//        String welcome_info2 = "To ask help in emergency: Please help there is emergency..";
//        super.messagesAdapter.addToStart(
//                MessagesFixtures.getTextMessage(welcome_info2, "1"), true);
//
//        String welcome_info3 = "To ask general questions: What is LTA Singapore?";
//        super.messagesAdapter.addToStart(
//                MessagesFixtures.getTextMessage(welcome_info3, "1"), true);

//        super.messagesAdapter.addToStart(
//                MessagesFixtures.getImageMessage("http://104.43.15.41:5001/Dialog_Engine/imgs/map.png", "1"), true);
//                //MessagesFixtures.getImageMessage("https://drive.google.com/drive/folders/1ZT5bZ1UBL83QfOTvwJNH5WGsFu9NFSnD", "1"), true);

        //  public String getImageUrl() {
        //       return image == null ? null : image.url;
        // }


        RichLinkView richLinkView;
        // protected void onCreate (Bundle savedInstanceState) {
        //super.onCreate(savedInstanceState);
//            setContentView(R.layout.list_item_map2);
        // ...
        //
//        richLinkView = (RichLinkView) findViewById(R.id.richLinkView);
//
//        //  Picasso.get().load(metaData.imageurl).into(imgPreview)
//
//        richLinkView.setLink("https://www.google.com/maps/dir/Sembawang/National+University+of+Singapore,+21+Lower+Kent+Ridge+Rd,+Singapore+119077/@1.3706059,103.727092,12z/data=!3m1!4b1!4m14!4m13!1m5!1m1!1s0x31da13622c24a83d:0x500f7acaedaa6d0!2m2!1d103.8184954!2d1.4491107!1m5!1m1!1s0x31da1a56784202d9:0x488d08d6c1f88d6b!2m2!1d103.7763939!2d1.2966426!3e3", new ViewListener() {
//
//            @Override
//            public void onSuccess(boolean status) {
//
//            }
//
//            @Override
//            public void onError(Exception e) {
//
//            }
//        });


        input = (MessageInput) this.findViewById(R.id.input2);
        input.setInputListener(this);
        input.setTypingListener(this);

        tv_scoll = (FloatTextView) findViewById(R.id.tv_menuname);

        tv_scoll.initScrollTextView(getWindowManager(), scrollText, 1);
//        tv_scoll.setText("welcome to bus!");
        tv_scoll.starScroll();

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

        callPhone = findViewById(R.id.call_phone);

        callPhone.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            public void onClick(View arg0) {
                Log.d("press call phone", "pressed");
                if (!hasPermission()) {
                    Log.d("press call phone", "can not call ");
                    int curApiVersion = Build.VERSION.SDK_INT;
                    if (curApiVersion >= Build.VERSION_CODES.M) {
                        requestPermissions(
                                new String[]{Manifest.permission.CALL_PHONE},
                                0x11);
//                        intentToCall("85443713");
                    } else {
                        intentToCall(my_property.getProperty("emergenceCall"));
                    }
                } else {
                    intentToCall(my_property.getProperty("emergenceCall"));
                }
            }
        });
        checkPermission();

        // Load the labels for the model, but only display those that don't start
        // with an underscore.
        if (wakeupflag == true) {

            String actualLabelFilename = LABEL_FILENAME.split("file:///android_asset/", -1)[1];
            Log.i(LOG_TAG, "Reading labels from: " + actualLabelFilename);
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(getAssets().open(actualLabelFilename)));
                String line;
                while ((line = br.readLine()) != null) {
                    Log.d(LOG_TAG, line);
                    labels.add(line);
                    if (line.charAt(0) != '_') {
                        displayedLabels.add(line.substring(0, 1).toUpperCase() + line.substring(1));
                    }
                }
                br.close();
            } catch (IOException e) {
                throw new RuntimeException("Problem reading label file!", e);
            }

            // Set up an object to smooth recognition results to increase accuracy.
            recognizeCommands =
                    new RecognizeCommands(
                            labels,
                            AVERAGE_WINDOW_DURATION_MS,
                            DETECTION_THRESHOLD,
                            SUPPRESSION_MS,
                            MINIMUM_COUNT,
                            MINIMUM_TIME_BETWEEN_SAMPLES_MS);

            String actualModelFilename = MODEL_FILENAME.split("file:///android_asset/", -1)[1];
            Log.d(LOG_TAG, actualModelFilename);
            try {
                tfLite = new Interpreter(loadModelFile(this.getAssets(), actualModelFilename));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            //
            tfLite.resizeInput(0, new int[]{RECORDING_LENGTH, 1});
            tfLite.resizeInput(1, new int[]{1});

            // Start the recording and recognition threads.
            requestMicrophonePermission();
            startRecording();
            startRecognition();
        }

        scrollTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                android.os.Message message = new android.os.Message();
                message.what = 1;
                scrollHandler.sendMessage(message);
            }
        }, 20000, 1000000);

        location = new SimpleLocation(this);

        // if we can't access the location yet
        if (!location.hasLocationEnabled()) {
            // ask the user to enable location access
            SimpleLocation.openSettings(this);
        }

        location.setListener(new SimpleLocation.Listener() {

            public void onPositionChanged() {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                Log.d("location update", latitude + " " + longitude);
            }

        });
        location.beginUpdates();
        that = this;
    }

//    private String getLOcation(){
//        // construct a new instance of SimpleLocation
//        return latitude + "-" + longitude;
//    }

    private boolean hasPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    /**
     * Memory-map the model file in Assets.
     */
    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void requestMicrophonePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                    new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
        }
    }

    public synchronized void startRecording() {
        if (recordingThread != null) {
            return;
        }
        shouldContinue = true;
        recordingThread =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                record();
                            }
                        });
        recordingThread.start();
    }

    public synchronized void stopRecording() {
        if (recordingThread == null) {
            return;
        }
        shouldContinue = false;
        recordingThread = null;
    }

    private void record() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

        // Estimate the buffer size we'll need for this device.
        int bufferSize =
                AudioRecord.getMinBufferSize(
                        SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }
        short[] audioBuffer = new short[bufferSize / 2];

        AudioRecord record =
                new AudioRecord(
                        MediaRecorder.AudioSource.DEFAULT,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize);

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Record can't initialize!");
            return;
        }

        record.startRecording();

        Log.v(LOG_TAG, "Start recording");

        // Loop, gathering audio data and copying it to a round-robin buffer.
        while (shouldContinue) {
            int numberRead = record.read(audioBuffer, 0, audioBuffer.length);
            int maxLength = recordingBuffer.length;
            int newRecordingOffset = recordingOffset + numberRead;
            int secondCopyLength = Math.max(0, newRecordingOffset - maxLength);
            int firstCopyLength = numberRead - secondCopyLength;
            // We store off all the data for the recognition thread to access. The ML
            // thread will copy out of this buffer into its own, while holding the
            // lock, so this should be thread safe.
            recordingBufferLock.lock();
            try {
                System.arraycopy(audioBuffer, 0, recordingBuffer, recordingOffset, firstCopyLength);
                System.arraycopy(audioBuffer, firstCopyLength, recordingBuffer, 0, secondCopyLength);
                recordingOffset = newRecordingOffset % maxLength;
            } finally {
                recordingBufferLock.unlock();
            }
        }

        record.stop();
        record.release();
    }

    public synchronized void startRecognition() {
        if (recognitionThread != null) {
            return;
        }
        shouldContinueRecognition = true;
        recognitionThread =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                recognize();
                            }
                        });
        recognitionThread.start();
    }

    public synchronized void stopRecognition() {
        if (recognitionThread == null) {
            return;
        }
        shouldContinueRecognition = false;
        recognitionThread = null;
    }

    private void recognize() {

        Log.v(LOG_TAG, "Start recognition");

        short[] inputBuffer = new short[RECORDING_LENGTH];
        float[][] floatInputBuffer = new float[RECORDING_LENGTH][1];
        float[][] outputScores = new float[1][labels.size()];
        int[] sampleRateList = new int[]{SAMPLE_RATE};

        // Loop, grabbing recorded data and running the recognition model on it.
        while (shouldContinueRecognition) {
            long startTime = new Date().getTime();
            // The recording thread places data in this round-robin buffer, so lock to
            // make sure there's no writing happening and then copy it to our own
            // local version.
            recordingBufferLock.lock();
            try {
                int maxLength = recordingBuffer.length;
                int firstCopyLength = maxLength - recordingOffset;
                int secondCopyLength = recordingOffset;
                System.arraycopy(recordingBuffer, recordingOffset, inputBuffer, 0, firstCopyLength);
                System.arraycopy(recordingBuffer, 0, inputBuffer, firstCopyLength, secondCopyLength);
            } finally {
                recordingBufferLock.unlock();
            }

            // We need to feed in float values between -1.0f and 1.0f, so divide the
            // signed 16-bit inputs.
            for (int i = 0; i < RECORDING_LENGTH; ++i) {
                floatInputBuffer[i][0] = inputBuffer[i] / 32767.0f;
            }

            Object[] inputArray = {floatInputBuffer, sampleRateList};
            Map<Integer, Object> outputMap = new HashMap<>();
            outputMap.put(0, outputScores);

            // Run the model.
            tfLite.runForMultipleInputsOutputs(inputArray, outputMap);

            // Use the smoother to figure out if we've had a real recognition event.
            long currentTime = System.currentTimeMillis();
            final RecognizeCommands.RecognitionResult result =
                    recognizeCommands.processLatestResults(outputScores[0], currentTime);
            if (!result.foundCommand.startsWith("_") && result.score > Float.valueOf(my_property.getProperty("wakeupAccuracy"))) {
                runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {

                                input.attachmentButton.performClick();

                            }
                        });
            }

            try {
                // We don't need to run too frequently, so snooze for a bit.
                Thread.sleep(MINIMUM_TIME_BETWEEN_SAMPLES_MS);
            } catch (InterruptedException e) {
                // Ignore
            }
        }

        Log.v(LOG_TAG, "End recognition");
    }

    private void intentToCall(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_CALL);
        Uri data = Uri.parse("tel:" + phoneNumber);
        intent.setData(data);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        startActivity(intent);
    }

    @Override
    public boolean onSubmit(final CharSequence input, final String userID) throws IOException {
        super.messagesAdapter.addToStart(
                MessagesFixtures.getTextMessage(input.toString(), userID), true);
//        Log.d(LOG_TAG, bus);
//        String[] inputs = {bus, input.toString()};
//        ResponseString responseString = new ResponseString();
//        responseString.execute(inputs);
        bus = "NO_BUS";
        Log.d(LOG_TAG, bus);
        ExecutorService executor = Executors.newCachedThreadPool();
        ResponseMessage response_Message = new ResponseMessage(input.toString(), bus);
        Future<TextMessage> result = executor.submit(response_Message);

        TextMessage textMessage = null;
        String response_str = "";
        String emergency_flag = "0";
        String url_flag = "0";
        Log.d("BEFORE", url_flag);
        try {
            textMessage = result.get();
            response_str = textMessage.getResponse_str();
            emergency_flag = textMessage.getEmergency_flag();
            url_flag = textMessage.getUrl_flag();
            Log.d(LOG_TAG, url_flag);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d("AFTER", url_flag);
        if (response_str != "") {
            String[] response_strs = response_str.split("::\\\\n");
            for (int index = 0; index < response_strs.length; index++) {
                String newText = response_strs[index].replace("\\n", "\n");

                super.messagesAdapter.addToStart(
                        MessagesFixtures.getTextMessage(newText, "1"), true);

                if (index == 0) {
                    TTS_speak(newText);
                }
//                if (index == response_strs.length -1)
//                {
//                    super.messagesAdapter.addToStart(
//                            MessagesFixtures.getTextMessage(TEST_LINK, "1"), true);
//                }
            }


            if (!url_flag.equals("0")) {
                Log.d(LOG_TAG, "+++>>" + url_flag);
//                super.messagesAdapter.addToStart(
//                        MessagesFixtures.getImageMessage(url_flag, "1"), true);
                        super.messagesAdapter.addToStart(
                MessagesFixtures.getTextMessage("", "1", url_flag), true);
            }
            Log.d("url_log","else part");

        } else {
            Log.d("url error:", "can not get response info!");
        }
        if (emergency_flag.equals("1")) {
            if (response_str != "") {
                Log.d("url_log","indide emergeny not response");
                String[] response_strs = response_str.split("::\\\\n");
                for (int index = 0; index < response_strs.length; index++) {
                    String newText = response_strs[index].replace("\\n", "\n");
                    if (index == 0) {
                        TTS_speak(newText);
                    }
                }

            } else {
                Log.d("adapter error:", "can not get response info!");
            }
            this.callPhone.performClick();
        }
        return true;
    }

    /**
     * replace Callable by AsyncTask, make samaung can work
     */
//    private class ResponseString extends AsyncTask<String, Void, String> {
//        @Override
//        protected String doInBackground(String... params) {
//            try {
//                Log.d(LOG_TAG, params[0] + " " + params[1]);
//                Call<TextMessage> textInfo = httpUtil.getTextMessage(params[0], params[1]);
//                if(textInfo.equals(null)){
//                    Log.d(LOG_TAG, "textInfo is null");
//                }
//                Response<TextMessage> execute = textInfo.execute();
//                if(execute.equals(null)){
//                    Log.d(LOG_TAG, "execute is null");
//                }else{
//                    Log.d(LOG_TAG, execute.toString());
//                }
//                TextMessage message = execute.body();
//
//                if(!message.equals(null)){
//                    Log.d(LOG_TAG, message.getResponse_str());
//                    return  message.getResponse_str();
//                }else{
//                    return null;
//                }
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(String response_str) {
////            String response_str = result;
//            if (response_str != "" || response_str.equals(null) ) {
//                Log.d(LOG_TAG, response_str);
//                String[] response_strs = response_str.split("::\\\\n");
//                for (int index = 0; index < response_strs.length; index++) {
//                    String newText = response_strs[index].replace("\\n", "\n");
//                    messagesAdapter.addToStart(
//                            MessagesFixtures.getTextMessage(newText, "1"), true);
//                    if (index == 0) {
//                        TTS_speak(newText);
//                    }
//                }
//
//            } else {
//                Log.d("adapter error:", "can not get response info!");
//            }
//        }
//    }

    class ResponseMessage implements Callable<TextMessage> {
        private String response_str;
        private String input;
        private String bus_id;

        ResponseMessage(String input, String bus_id) {
            this.input = input;
            this.bus_id = bus_id;
        }

        @Override
        public TextMessage call() {
            Call<TextMessage> textInfo = null;
//            response_str = "";
            TextMessage textMessage = null;
            try {
                textInfo = httpUtil.getTextMessage(bus_id, input);

//                TextMessage text = textInfo.execute().body();
//                Log.d(LOG_TAG, text.toString());
                textMessage = textInfo.execute().body();
                response_str = textMessage.getResponse_str();
                Log.d(LOG_TAG, response_str);

            } catch (IOException e) {
                e.printStackTrace();
            }
            return textMessage;
        }
    }

    class ResponseLocationMessage implements Callable<LocationMessage> {
        private String response_str;
        private String input;
        private String bus_id;

        ResponseLocationMessage(String input, String bus_id) {
            this.input = input;
            this.bus_id = bus_id;
        }

        @Override
        public LocationMessage call() {
            Call<LocationMessage> textInfo = null;
            LocationMessage locationMessage = null;
            try {
                Log.d("location call", latitude + " " + longitude);
                //textInfo = httpUtil.getTextMessageLoc(bus, latitude, longitude, input);
                String lat = Double.toString(latitude);
                String lon = Double.toString(longitude);
                textInfo = httpUtil.getTextMessageLoc(bus, lat + "," + lon);
                locationMessage = textInfo.execute().body();
//                textInfo.execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return locationMessage;
        }
    }

    private String SetSpeechListener() {
        // stop wakeup record
        if (wakeupflag == true) {
            stopRecording();
            stopRecognition();
        }

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
                    //status_tv.setText(getString(R.string.no_results_found));
                }
                mSpeechManager.destroy();
                mSpeechManager = null;
                input.attachmentButton.setClickable(true);
                input.attachmentButton.getBackground().setColorFilter(null);

            }

        });
        // start wakeup record
        if (wakeupflag) {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    startRecording();
                    startRecognition();
                }
            }, 10000);
        }

        Log.d("after sound", "after sound");

        return res;
    }

    private void sendSoundInfo2(String info) {
        Log.d("sound input", info);
        super.messagesAdapter.addToStart(
                MessagesFixtures.getTextMessage(info, "0"), true);
        sendSoundInfo(info);
    }

    private void sendSoundInfo(String info) {

//        String[] inputs = {input.toString(), bus};
//        ResponseString responseString = new ResponseString();
//        responseString.execute(inputs);
        bus = "NO_BUS";
        ExecutorService executor = Executors.newCachedThreadPool();
        ResponseMessage response_Message = new ResponseMessage(info, bus);
        Future<TextMessage> result = executor.submit(response_Message);

        TextMessage textMessage = null;
        String response_str = "";
        String emergency_flag = "0";
        String url_flag = "0";
        try {
            textMessage = result.get();
            response_str = textMessage.getResponse_str();
            emergency_flag = textMessage.getEmergency_flag();
            url_flag = textMessage.getUrl_flag();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (response_str != "") {
            Log.d("textMessage", response_str);
            String[] response_strs = response_str.split("::\\\\n");
            for (int index = 0; index < response_strs.length; index++) {
                String newText = response_strs[index].replace("\\n", "\n");
                super.messagesAdapter.addToStart(
                        MessagesFixtures.getTextMessage(newText, "1"), true);
                if (index == 0) {
                    TTS_speak(newText);
                }
            }
//            if (url_flag != "0") {
//                super.messagesAdapter.addToStart(MessagesFixtures.getTextMessage(TEST_LINK, "1"), true);
//            }
            if (!url_flag.equals("0")) {
                Log.d(LOG_TAG, "+++>>" + url_flag);
//                super.messagesAdapter.addToStart(
//                        MessagesFixtures.getImageMessage(url_flag, "1"), true);
                super.messagesAdapter.addToStart(
                        MessagesFixtures.getTextMessage("", "1", url_flag), true);
            }
//            super.messagesAdapter.addToStart(
//                    MessagesFixtures.getTextMessage("", "1", url_flag), true);
        } else {
            Log.d("adapter error:", "can not get response info!");
        }
        if (emergency_flag.equals("1")) {
            this.callPhone.performClick();
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
        if (httpUtil == null) {
            String base_url = my_property.getProperty("serverUrl");
            String port = my_property.getProperty("port");
            String path = base_url + ":" + port + "/";
            Log.d("path", path);
            httpUtil = RetrofitClientInstance.getRetrofitInstance(path).create(HttpUtil.class);
        }
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
        String welcome_info = "hello, how can I help you";
//        TTS_speak("TTS is ready, Bus ID is : " + bus);
        TTS_speak(welcome_info);
    }

    private void TTS_speak(String speech) {
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int amStreamMusicVol = am.getStreamVolume(AudioManager.STREAM_RING);
       // am.setStreamVolume(AudioManager.STREAM_MUSIC, amStreamMusicMaxVol, 0);

        Bundle bundle = new Bundle();
        bundle.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
        bundle.putInt(TextToSpeech.Engine.KEY_PARAM_VOLUME, amStreamMusicVol);

        tts.speak(speech, TextToSpeech.QUEUE_FLUSH, null, null);
    }

//    private void TTS_speak(String speech) {
//        setVolumeControlStream(AudioManager.STREAM_MUSIC);
//        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//        int amStreamMusicVol = am.getStreamVolume(AudioManager.STREAM_RING);
////        int amStreamMusicMaxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
////        am.setStreamVolume(AudioManager.STREAM_MUSIC, amStreamMusicMaxVol, 0);
//
//        Bundle bundle = new Bundle();
//        bundle.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
//        bundle.putInt(TextToSpeech.Engine.KEY_PARAM_VOLUME, amStreamMusicVol);
//
//        tts.speak(speech, TextToSpeech.QUEUE_FLUSH, null, null);
//    }

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

}

