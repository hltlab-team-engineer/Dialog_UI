package xyz.hiroshifuu.speechapp.common;

public class SpeechItem {
    private String text;
    private boolean Req;

    public SpeechItem(String text, boolean Req) {
        this.text = text;
        this.Req = Req;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isReq() {
        return Req;
    }

    public void setReq(boolean req) {
        this.Req = req;
    }
}
