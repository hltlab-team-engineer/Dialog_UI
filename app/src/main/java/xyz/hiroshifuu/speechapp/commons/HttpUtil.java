package xyz.hiroshifuu.speechapp.commons;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HttpUtil {

    public void request(String serviceUrl){
//        Properties proper = ProperUtil.getPropertiesURL(getApplicationContext());
//        String serviceUrl = proper.getProperty("serverUrl");
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(serviceUrl) // setting server Url
                .addConverterFactory(GsonConverterFactory.create()) //setting json proxy
                .build();
    }
}
