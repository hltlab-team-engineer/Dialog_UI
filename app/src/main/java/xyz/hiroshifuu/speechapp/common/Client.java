package xyz.hiroshifuu.speechapp.common;


import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;


public class Client extends AsyncTask<String,Void,String> {

    String dstAddress;
    int dstPort;
    String dstqry;
    String response = "";
    TextView textResponse;
    PrintWriter out;
    Socket socket = null;
    InputStream inputStream;

    public Client(String addr, int port, String query) {
        dstAddress = addr;
        dstPort = port;
        dstqry = query;
    }

    @Override
    protected String doInBackground(String... strings) {
        try {
            connectWithServer();

            sendDataWithString(dstqry); //one

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(
                    1024);
            byte[] buffer = new byte[1024];
            int bytesRead;
            /*
             * notice: inputStream.read() will block if no data return
             */
            if ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
                response += byteArrayOutputStream.toString("UTF-8");
                //buffer=null;// bufer null crashes the app after 1 execution
                //if(bytesRead==-1)
                //{buffer = new byte[1024];}
            }
            //this.textResponse.setText(response);
            //String caft=textResponse.getText().toString();

        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            response = "UnknownHostException: " + e.toString();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            response = "IOException: " + e.toString();
        } finally {
            if (socket != null) {
                disConnectWithServer();
            }
        }
        return response;
    }

    private void connectWithServer() {
        try {
            if (socket == null) {
                Log.d("create new socket: ", "connectWithServer");
                socket = new Socket(dstAddress, dstPort);
                out = new PrintWriter(socket.getOutputStream());
                inputStream = socket.getInputStream();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void disConnectWithServer() {
        if (socket != null) {
            if (socket.isConnected()) {
                try {
                    inputStream.close();
                    out.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public void sendDataWithString(String message) {
        if (message != null) {
            connectWithServer();
            Log.d("send data: ", message);
            try {
                Log.d("send data: ", socket.getOutputStream().toString());
            }
            catch (Exception e){
                Log.d("can,t get output stream",e.getMessage());
            }
            out.write(message);
            out.flush();
        }
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);

        //doInBackground();
        //connectWithServer();
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
    }




}