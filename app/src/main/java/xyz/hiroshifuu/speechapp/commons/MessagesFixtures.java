package xyz.hiroshifuu.speechapp.commons;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.UUID;

public class MessagesFixtures {
    private MessagesFixtures() {
        throw new AssertionError();
    }

    static SecureRandom rnd = new SecureRandom();

    public static Message getImageMessage(String imageInfo, String userID) {
        Message message = new Message(getRandomId(), getUser(userID), null);
        message.setImage(new Message.Image(imageInfo));
        return message;
    }

    public static Message getTextMessage(String text, String userID) {
        return new Message(getRandomId(), getUser(userID), text);
    }

    public static Message getTextMessage(String text, String userID, String link) {
        return new Message(getRandomId(), getUser(userID), text, link);
    }

    static final ArrayList<String> names = new ArrayList<String>() {
        {
            add("user");
            add("bus");
        }
    };

    static final ArrayList<String> avatars = new ArrayList<String>() {
        {
//            add("user");
//            add("bus");
            add("https://www.flaticon.com/free-icon/school-bus-front_15923#term=bus&page=1&position=4");
            add("https://www.flaticon.com/free-icon/school-bus-front_15923#term=bus&page=1&position=4");
        }
    };

    private static User getUser(String userID) {
        boolean even = false;
        if (userID.equals("0")){
            even = true;
        }
        return new User(
                userID,
                even ? names.get(0) : names.get(1),
                even ? avatars.get(0) : avatars.get(1),
                true);
    }

    static String getRandomId() {
        return Long.toString(UUID.randomUUID().getLeastSignificantBits());
    }
}
