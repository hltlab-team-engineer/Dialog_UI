package xyz.hiroshifuu.speechapp.models;

public class TextMessage {
    private String response_str;
    public TextMessage(String response_str){
        this.response_str = response_str;
    }

    public String getResponse_str() {
        return response_str;
    }

    public void setResponse_str(String response_str) {
        this.response_str = response_str;
    }
}
