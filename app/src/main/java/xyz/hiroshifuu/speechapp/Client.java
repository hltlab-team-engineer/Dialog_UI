package xyz.hiroshifuu.speechapp;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.media.AudioManager;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.sql.PreparedStatement;

/*public class Client extends AsyncTask<Void, Void, Void> {


	String dstAddress;
	String RES;
	int dstPort;
	String dstqry;
	String response = "";
	String res;
	String textResponse;
	PreparedStatement pst;
	BufferedReader in;
	Socket socket = null;
	PrintWriter out;


	Client(String addr, int port, String qry, String textResponse) {
		dstAddress = addr;
		dstPort = port;
		dstqry=qry;
		//Log.d("Query",dstqry);
		this.textResponse=textResponse;

	}


	@Override
	protected Void doInBackground(Void... arg0)  {

		connectWithServer();
		sendDataWithString(dstqry);
		response=receiveDataFromServer();
		Log.w("ABC",response);
		//disConnectWithServer();
		return null;
	}


	private void connectWithServer() {
		try {

			if (socket == null) {
				socket = new Socket(dstAddress, dstPort);
				out = new PrintWriter(socket.getOutputStream());
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void disConnectWithServer() {
		if (socket != null) {
			if (socket.isConnected()) {
				try {
					in.close();
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
			out.write(message);
			out.flush();
		}
	}

	public String receiveDataFromServer() {
		try {
			String message = "";
			int charsRead = 0;
			char[] buffer = new char[1024];

			while (true)
			{
				if((charsRead=in.read(buffer))<=0)
				{
					break;
				}
				message += new String(buffer).substring(0, charsRead);
			}
			String aaa="hello maulik";
			return message;

		/*	while ((charsRead = in.read(buffer)) != -1) {
				message += new String(buffer).substring(0, charsRead);

			} //enden here

			//disConnectWithServer(); // disconnect server

		} catch (IOException e) {
			return "Error receiving response:  " + e.getMessage();
		}
	}*/ //not here

/*	public String receiveDataFromServer() throws IOException {

			//input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			//String read = in.readLine();


			String message = "";
			int charsRead = 0;
			char[] buffer = new char[1024];

			while ((charsRead = in.read(buffer)) != -1) {
				message += new String(buffer).substring(0, charsRead);
				res=message;
			}
            return res;
			//disConnectWithServer(); // disconnect server

	}//here comment ended before



	@Override
	protected void onPostExecute(Void result) {
		textResponse=res;
		Log.i("RESPOSE OnpostExecution",res);
	}

}*/

public class Client extends AsyncTask<String,Void,String> {

    String dstAddress;
    int dstPort;
    String dstqry;
    String response = "";
    TextView textResponse;
    PrintWriter out;
    Socket socket = null;
    InputStream inputStream;

    Client(String addr, int port, String query) {
        dstAddress = addr;
        dstPort = port;
        dstqry = query;
    }

    @Override
    protected String doInBackground(String... strings) {
        try {
            connectWithServer();

            sendDataWithString(dstqry); //one
            //String cint=this.textResponse.getText().toString();

			/*
			int pos=0;
			ByteBuffer buff=ByteBuffer.allocate(1024);
			while (pos<buffer.length)
			{
				int n=inputStream.read(buffer,pos,buffer.length-pos);
				if(n<0)
				{

					buff=ByteBuffer.wrap(buffer,0,pos);
					break;
				}
				pos+=n;
				buff.clear();
			}
						this.textResponse.setText(buff.toString());

*/
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