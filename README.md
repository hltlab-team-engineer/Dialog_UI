## Description
This project is android UI for dialogue system

## Still pending issues
- QR code focus
- scrolling message (solved now- in optimized UI)

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