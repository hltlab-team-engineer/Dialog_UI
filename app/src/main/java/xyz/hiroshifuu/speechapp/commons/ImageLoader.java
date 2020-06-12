package xyz.hiroshifuu.speechapp.commons;

import android.support.annotation.Nullable;
import android.widget.ImageView;

/**
 * Callback for implementing images loading in message list
 */
public interface ImageLoader {

    void loadImage(ImageView imageView, @Nullable String url, @Nullable Object payload);

}

