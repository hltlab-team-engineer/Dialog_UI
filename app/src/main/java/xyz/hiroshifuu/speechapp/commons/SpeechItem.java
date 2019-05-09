package xyz.hiroshifuu.speechapp.commons;

public class SpeechItem {
    private String text;
    private boolean Req;
    private boolean Map;

    public SpeechItem(String text, boolean Req, boolean Map) {
        this.text = text;
        this.Req = Req;
        this.Map = Map;
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

    public boolean isMap() {
        return Map;
    }

    public void setMap(boolean map) {
        this.Map = map;
    }
}
