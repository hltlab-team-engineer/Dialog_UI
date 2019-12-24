package xyz.hiroshifuu.speechapp.models;

public class LocationMessage {
    private String response_location;

    public LocationMessage(String response_location){
        this.response_location = response_location;
    }

    public String getResponse_location(){
        return response_location;
    }

    public void setResponse_location(String response_location){
        this.response_location = response_location;
    }

}
