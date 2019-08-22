package xyz.hiroshifuu.speechapp.commons;

import retrofit2.Call;
import retrofit2.http.GET;

import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import xyz.hiroshifuu.speechapp.models.TextMessage;

public interface HttpUtil {
    @GET("/massage_str/{bus_id}")
    Call<TextMessage> getTextMessage(@Path(value = "bus_id", encoded = true) String bus, @Query("request_str_data") String input);

    @GET("/location/{bus_id}")
    Call<TextMessage> getTextMessageLoc(@Path(value = "bus_id", encoded = true) String bus, @Query("request_loc_data") String input);
}
