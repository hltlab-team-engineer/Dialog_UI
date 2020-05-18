package xyz.hiroshifuu.speechapp.models;

public class TextMessage {
    private String response_str;
    private String emergency_flag;
    private  String url_flag;
    public TextMessage(String response_str, String emergency_flag, String url_flag){
        this.response_str = response_str;
        this.emergency_flag = emergency_flag;
        this.url_flag = url_flag;
    }

    public String getEmergency_flag() {
        return emergency_flag;
    }

    public void setEmergency_flag(String emergency_flag) {
        this.emergency_flag = emergency_flag;
    }

    public String getResponse_str() {
        return response_str;
    }

    public void setResponse_str(String response_str) {
        this.response_str = response_str;
    }

    public String getUrl_flag() {
        return url_flag;
    }

    public void setUrl_flag(String url_flag) {
        this.url_flag = url_flag;
    }

}
