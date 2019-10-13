package xyz.hiroshifuu.speechapp.models;

/**
 * For implementing by real user model
 */
public interface IUser {

    /**
     * Returns the user's id
     *
     * @return the user's id
     * */
    String getId();

    /**
     * Returns the user's name
     *
     * @return the user's name
     * */
    String getName();

    /**
     * Returns the user's avatar image url
     *
     * @return the user's avatar image url
     * */
    String getAvatar();
}