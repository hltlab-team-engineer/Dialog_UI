package xyz.hiroshifuu.speechapp.models;

public class TextMessage {
    private String response_str;
    private String emergency_flag;
    public TextMessage(String response_str, String emergency_flag){
        this.response_str = response_str;
        this.emergency_flag = emergency_flag;
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
}
