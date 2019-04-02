package xyz.hiroshifuu.speechapp.models;

public class TextMessage {
    private String textINfo;
    public TextMessage(String textINfo){
        this.textINfo = textINfo;
    }

    public String getTextINfo() {
        return textINfo;
    }

    public void setTextINfo(String textINfo) {
        this.textINfo = textINfo;
    }
}
