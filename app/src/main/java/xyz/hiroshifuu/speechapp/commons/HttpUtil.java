package xyz.hiroshifuu.speechapp.commons;

import retrofit2.Call;
import retrofit2.http.GET;


import retrofit2.http.Query;
import xyz.hiroshifuu.speechapp.models.TextMessage;

public interface HttpUtil {
    @GET("/massage_str")
    Call<TextMessage> getTextMessage(@Query("request_str_data") String input);
}
