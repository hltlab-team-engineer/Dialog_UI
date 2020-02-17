## Description
This project is android UI for dialogue system

## Still pending issues
- QR code focus
- scrolling message (solved now- in optimized UI)

## the framework of this system


## How to create wakeup words system


## How to create customize ASR
This is a guide of using customize ASR (created by NUS NTU and AI.SG)

## How to add Emergence button at system
on this system we create 'Emergence button' by calling the service of android call
1. add permission
    '''
    <uses-permission android:name="android.permission.MANAGE_OWN_CALLS" />
    <uses-permission android:name="android.permission.READ_CALL_LOG" />
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    <uses-permission android:name="android.permission.CALL_PHONE" />
    '''
2. call the action of 'ACTION_CALL' to call emergence call
    '''
    private void intentToCall(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_CALL);
        Uri data = Uri.parse("tel:" + phoneNumber);
        intent.setData(data);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            return;
        }
        startActivity(intent);
    }
    '''

## How to connect with engine(server)

the android client is connect with engine by the library of `retrofit2`:
1. import the library of `retrofit2`
    '''
    implementation 'com.squareup.retrofit2:retrofit:2.4.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.4.0'
    '''
2.  Add permission
    '''
    <uses-permission android:name="android.permission.INTERNET" />
    '''
3. create http connection interface
    '''
    public interface HttpUtil
    {
        @GET("massage_str/{bus_id}")
        Call<TextMessage> getTextMessage(@Path(value = "bus_id", encoded = true) String bus, @Query("request_str_data") String input);
    }
    '''
4. using http connection at application (in real project, it better to using 'ExecutorService' to create Thread Pool to exec http connection)
   '''
   httpUtil = RetrofitClientInstance.getRetrofitInstance(path).create(HttpUtil.class);
   textInfo = httpUtil.getTextMessage(bus_id, input);
   '''


## Important files
File/dir|Location | details
---|---|---
D  | ./app/src/main/java/xyz/hiroshifuu/speechapp| java source files
D  | ./app/src/main/java/xyz/hiroshifuu/speechapp/activity| source files of activity
D  | ./app/src/main/java/xyz/hiroshifuu/speechapp/commons| source files include styles and http connection
D  | ./app/src/main/java/xyz/hiroshifuu/speechapp/messages| source files include format of Message list and message input
D  | ./app/src/main/res | layout and design files
D  | ./app/src/main/res/drawable | icons/symbols in layout
D  | ./app/src/main/res/layout | layout files
D  | ./app/src/main/java/xyz/hiroshifuu/speechapp/activity | All files related to different ui (speech and main-QR code)
F  | ./app/src/main/AndroidManifest.xml | permission and connections of all modules
F  | ./app/src/main/java/xyz/hiroshifuu/speechapp/activity/MainActivity.java | Opening interface (QR code)
F  | ./app/src/main/java/xyz/hiroshifuu/speechapp/activity/SpeechActivity.java | main UI file (Please change SERVER and PORT as per your requirement)
F  | ./app/src/main/java/xyz/hiroshifuu/speechapp/commons/ProperUtil.java | source code for client to send and recive data
F  | ./app/src/main/java/xyz/hiroshifuu/speechapp/adapter/MessageListAdapter| control the list view of message
F  | ./app/src/main/java/xyz/hiroshifuu/speechapp/adapter/MessageHolder| control the layout of message
F  | ./app/src/main/res/layout/active_chat_dialog.xml | layout of speech activity
F  | ./app/src/main/res/layout/item_incoming_text_message.xml | layout of request message
F  | ./app/src/main/res/layout/item_outcoming_text_message.xml | layout of response message