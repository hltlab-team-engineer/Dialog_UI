package xyz.hiroshifuu.speechapp.service;

import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.speech.RecognizerIntent;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import xyz.hiroshifuu.speechapp.utils.Log;
import xyz.hiroshifuu.speechapp.R;
import xyz.hiroshifuu.speechapp.commons.ProperUtil;
import xyz.hiroshifuu.speechapp.utils.AudioRecorder;
import xyz.hiroshifuu.speechapp.utils.ChunkedWebRecSessionBuilder;
import xyz.hiroshifuu.speechapp.utils.EncodedAudioRecorder;
import xyz.hiroshifuu.speechapp.utils.Extras;
import xyz.hiroshifuu.speechapp.utils.PreferenceUtils;

public class WebSocketRecognitionService extends AbstractRecognitionService{
    // When does the chunk sending start and what is its interval
    private static final int TASK_DELAY_SEND = 100;
    private static final int TASK_INTERVAL_SEND = 200;
    // Limit to the number of hypotheses that the service will return
    // TODO: make configurable
    private static final int MAX_HYPOTHESES = 100;
    // Pretty-print results
    // TODO: make configurable
    private static final boolean PRETTY_PRINT = true;

    private static final String EOS = "EOS";
    private static final String TAG = "speech tag: ";

    private static final String PROTOCOL = "";

    private static final String context_prefix = "?content-type=audio/x-raw,+layout=(string)interleaved,+rate=(int)8000,+format=(string)S16LE,+channels=(int)1";
    private static final String token_prefix = "?content-type=&accessToken=";
    private static final String model = "&model=eng_closetalk";

    private Properties my_property;

    private static final int MSG_RESULT = 1;
    private static final int MSG_ERROR = 2;

    private volatile Looper mSendLooper;
    private volatile Handler mSendHandler;

    private MyHandler mMyHandler;

    private Runnable mSendRunnable;

    private WebSocket mWebSocket;

    private String mUrl;

    private boolean mIsEosSent;

    private int mNumBytesSent;

    //    private static final String parse_token = "?content-type=audio/x-raw+layout=(string)interleaved+rate=(int)16000+format=(string)S16LE+channels=(int)1?token=";
//    private static final String parse_token = "?content-type=audio/x-raw+layout=(string)interleaved+rate=(int)16000+format=(string)S16LE+channels=(int)1?token=";
    private static final String parse_token = "?token=";
    private static final String flag = "&";

    @Override
    protected void configure(Intent recognizerIntent) throws IOException {
        //getExtras():{android.speech.extra.PARTIAL_RESULTS=true, android.speech.extra.LANGUAGE_MODEL=free_form, calling_package=xyz.hiroshifuu.speechapp}
        //ChunkedWebRecSessionBuilder builder = new ChunkedWebRecSessionBuilder(this, getExtras(), null);


        boolean isUnlimitedDuration = getExtras().getBoolean(Extras.EXTRA_UNLIMITED_DURATION, false)
                || getExtras().getBoolean(Extras.EXTRA_DICTATION_MODE, false);
        //isUnlimitedDuration:false
        configureHandler(isUnlimitedDuration,
                getExtras().getBoolean(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false));
    }

    @Override
    protected void connect() {
        my_property = ProperUtil.getPropertiesURL(getApplicationContext());
        String asr_host = my_property.getProperty("ASRHost");
        String asr_port = my_property.getProperty("ASRPort");
        String asr_path = my_property.getProperty("ARSPath");
        String token = my_property.getProperty("token");
        String mUrl = asr_host +  asr_port + asr_path + context_prefix + token_prefix + token+ model;

        Log.i(TAG, mUrl);
        startSocket(mUrl);
    }

    @Override
    protected void disconnect() {
        if (mSendHandler != null) mSendHandler.removeCallbacks(mSendRunnable);
        if (mSendLooper != null) {
            mSendLooper.quit();
            mSendLooper = null;
        }

        if (mWebSocket != null && mWebSocket.isOpen()) {
            mWebSocket.end(); // TODO: or close?
            mWebSocket = null;
        }
        Log.i("Number of bytes sent: " + mNumBytesSent);
    }

    @Override
    protected String getEncoderType() {
        return PreferenceUtils.getPrefString(getSharedPreferences(), getResources(),
                R.string.keyImeAudioFormat, R.string.defaultAudioFormat);
    }

    @Override
    protected boolean isAudioCues() {
        return PreferenceUtils.getPrefBoolean(getSharedPreferences(), getResources(), R.string.keyImeAudioCues, R.bool.defaultImeAudioCues);
    }

    protected void configureHandler(boolean isUnlimitedDuration, boolean isPartialResults) {
        mMyHandler = new MyHandler(this, isUnlimitedDuration, isPartialResults);//用于从网络获取信息，怕网络延迟，导致页面假死
    }

    private void handleResult(String text) {
        // when connect to the websocket, websocket will send a message to show that the connection is successful.
        // this message will send to handler but this message doesn't contain results so the chatbot will not show this message
        // and stop the speech recognition
        // so the new recording result will be ignored
        if(text.contains("result")){
        Message msg = new Message();
        msg.what = MSG_RESULT;//msg.what =1 为start listening
        msg.obj = text;

        mMyHandler.sendMessage(msg);}
    }

    private void handleException(Exception error) {
        Message msg = new Message();
        msg.what = MSG_ERROR;
        msg.obj = error;
        mMyHandler.sendMessage(msg);
    }

    /**
     * Opens the socket and starts recording/sending.
     *
     * @param url Webservice URL
     */
    void startSocket(String url) {
        mIsEosSent = false;
        Log.i("WebSocket","Connect to websocket");
        AsyncHttpClient.getDefaultInstance().websocket(url, PROTOCOL, (ex, webSocket) -> {
            mWebSocket = webSocket;
            if (ex != null) {
                Log.e("WebsocketRecognition","Exception:"+ex);
                handleException(ex);
                return;
            }
            //连接成功后，websocket会发送一个{"status":200,"message":"ready"}
            webSocket.setStringCallback(s -> {
                Log.i("WebsocketRecognition","Receive Message "+s);
                handleResult(s);
            });

            webSocket.setClosedCallback(ex1 -> {
                if (ex1 == null) {
                    Log.e(TAG,"ClosedCallback");
                    handleFinish(mIsEosSent);
                } else {
                    Log.e(TAG, "ClosedCallback: "+ ex1);
                    handleException(ex1);
                }
            });

            webSocket.setEndCallback(ex12 -> {
                if (ex12 == null) {
                    Log.e("EndCallback");
                    handleFinish(mIsEosSent);
                } else {
                    Log.e("EndCallback: ", ex12);
                    handleException(ex12);
                }
            });

            startSending(webSocket);
        });
    }


    private void startSending(final WebSocket webSocket) {

        Log.i("Websocket","start sending");
        mNumBytesSent = 0;
        HandlerThread thread = new HandlerThread("WsSendHandlerThread", Process.THREAD_PRIORITY_BACKGROUND);//创建一个循环
        thread.start();//启动循环
        mSendLooper = thread.getLooper();
        mSendHandler = new Handler(mSendLooper);

        // Send chunks to the server
        mSendRunnable = new Runnable() {//开启一个新的线程
            public void run() {
                if (webSocket != null && webSocket.isOpen()) {

                    AudioRecorder recorder = getRecorder();//Row Recorder
                    if (recorder == null || recorder.getState() != AudioRecorder.State.RECORDING) {
                        Log.i("Sending: EOS (recorder == null)");
                        webSocket.send(EOS);
                        mIsEosSent = true;
                    }
                    else {
                        Log.i("Websocket","Get State"+recorder.getState()+"");
                        byte[] buffer = recorder.consumeRecordingAndTruncate();
                        //System.out.println(Arrays.toString(buffer));
                        if (recorder instanceof EncodedAudioRecorder) {
                            Log.i("Web","there");
                            send(webSocket, ((EncodedAudioRecorder) recorder).consumeRecordingEncAndTruncate());
                        } else {
                            send(webSocket, buffer);
                        }
                        if (buffer.length > 0) {
                            onBufferReceived(buffer);
                        }
                        //this:xyz.hiroshifuu.speechapp.service.WebSocketRecognitionService
                        //TASK_INTERVAL_SEND:200ms
                        boolean success = mSendHandler.postDelayed(this, TASK_INTERVAL_SEND);

                        if (!success) {
                            Log.i("mSendHandler.postDelayed returned false");
                        }
                    }
                }
            }
        };

        mSendHandler.postDelayed(mSendRunnable, TASK_DELAY_SEND);
    }

    void send(WebSocket webSocket, byte[] buffer) {
        if (buffer != null && buffer.length > 0) {
            webSocket.send(buffer);
            mNumBytesSent += buffer.length;
            Log.i("Sent bytes: " + buffer.length);
        }
    }

    private static class MyHandler extends Handler {
        private final WeakReference<WebSocketRecognitionService> mRef;
        private final boolean mIsUnlimitedDuration;
        private final boolean mIsPartialResults;

        public MyHandler(WebSocketRecognitionService c, boolean isUnlimitedDuration, boolean isPartialResults) {
            mRef = new WeakReference<>(c);
            mIsUnlimitedDuration = isUnlimitedDuration;
            mIsPartialResults = isPartialResults;
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            WebSocketRecognitionService outerClass = mRef.get();
            if (outerClass != null) {
                if (msg.what == MSG_ERROR) {
                    Exception e = (Exception) msg.obj;
                    if (e instanceof TimeoutException) {
                        outerClass.onError(android.speech.SpeechRecognizer.ERROR_NETWORK_TIMEOUT);
                    } else {
                        outerClass.onError(android.speech.SpeechRecognizer.ERROR_NETWORK);
                    }
                } else if (msg.what == MSG_RESULT) {
                    try {
                        WebSocketResponse response = new WebSocketResponse((String) msg.obj);//send reply to the WebsocketResponse
                        int statusCode = response.getStatus();

                        if (statusCode == WebSocketResponse.STATUS_SUCCESS && response.isResult()) {
                            WebSocketResponse.Result responseResult = response.parseResult();
                            if (responseResult.isFinal()) {
                                ArrayList<String> hypotheses = responseResult.getHypotheses(MAX_HYPOTHESES, PRETTY_PRINT);
                                if (hypotheses.isEmpty()) {
                                    Log.i("Empty final result (" + hypotheses + "), stopping");
                                    outerClass.onError(android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT);
                                } else {
                                    // We stop listening unless the caller explicitly asks us to carry on,
                                    // by setting EXTRA_UNLIMITED_DURATION=true
                                    if (mIsUnlimitedDuration) {
                                        outerClass.onPartialResults(toResultsBundle(hypotheses, true));
                                    } else {
                                        outerClass.mIsEosSent = true;
                                        outerClass.onEndOfSpeech();
                                        outerClass.onResults(toResultsBundle(hypotheses, true));
                                    }
                                }
                            } else {
                                // We fire this only if the caller wanted partial results
                                if (mIsPartialResults) {
                                    ArrayList<String> hypotheses = responseResult.getHypotheses(MAX_HYPOTHESES, PRETTY_PRINT);
                                    if (hypotheses.isEmpty()) {
                                        Log.i("Empty non-final result (" + hypotheses + "), ignoring");
                                    } else {
                                        outerClass.onPartialResults(toResultsBundle(hypotheses, false));
                                    }
                                }
                            }
                        } else if (statusCode == WebSocketResponse.STATUS_SUCCESS) {
                            // TODO: adaptation_state currently not handled
                        } else if (statusCode == WebSocketResponse.STATUS_ABORTED) {
                            outerClass.onError(android.speech.SpeechRecognizer.ERROR_SERVER);
                        } else if (statusCode == WebSocketResponse.STATUS_NOT_AVAILABLE) {
                            outerClass.onError(android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY);
                        } else if (statusCode == WebSocketResponse.STATUS_NO_SPEECH) {
                            outerClass.onError(android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT);
                        } else if (statusCode == WebSocketResponse.STATUS_NO_VALID_FRAMES) {
                            outerClass.onError(android.speech.SpeechRecognizer.ERROR_NO_MATCH);
                        } else {
                            // Server sent unsupported status code, client should be updated
                            outerClass.onError(android.speech.SpeechRecognizer.ERROR_CLIENT);
                        }
                    } catch (WebSocketResponse.WebSocketResponseException e) {
                        // This results from a syntactically incorrect server response object
                        Log.e((String) msg.obj, e);
                        outerClass.onError(android.speech.SpeechRecognizer.ERROR_SERVER);
                    }
                }
            }
        }
    }

}
