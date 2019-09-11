/*
Socket Chat Application
by Ethan Tai

8/31/2019
This application is designed to set up a simple Socket connection through two phones over wifi
so that both of them can send and receive text messages. Also it is supposed to be able to measure
the time it takes to send and receive a message.

 */

package com.example.socketchatapp;
import androidx.appcompat.app.AppCompatActivity;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import java.io.*;
import java.lang.ref.WeakReference;
import java.net.*;


//TODO after connection is established change view to different view
//TODO implement chat history
//not sure how to implement this. Maybe a long string? What happens if the string gets too big?
//TODO display the ip of the device to make it easier to connect to
//TODO time function

public class MainActivity extends AppCompatActivity {
    //display is the main text window
    TextView display;

    //socket is the socket object used to establish a connection
    static Socket socket;

    //host is the String that will hold the IP of the socket to connect to
    static String host = "";

    //stop is the boolean that will tell whether or not a connection is up or not.
    //it is also used to stop a connection. see function stopServer
    static boolean stop = false;

    //out is the PrintWriter used to output text to the socket
    static PrintWriter out;

    //messageBox is the field that the user inputs the outgoing message
    EditText messageBox;

    //ipBox is the field that the user inputs the IP address
    EditText ipBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //TODO find out if this security stuff is useful or not
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.activity_main);

        //set the corresponding TextView and EditText objects to their corresponding UI objects
        display = findViewById(R.id.displayBox);
        ipBox = findViewById(R.id.ipBox);
        messageBox = findViewById(R.id.messageBox);
    }

    //function to listen for a socket connection request on button click
    public void listenServer(View view) {
        new receive(this).execute("");
    }

    //function to connect to a listening socket
    public void connectServer(View view) {
        new client(this).execute("");
    }

    //function to send a message to a listening socket and print the time it takes to receive an echo
    public void pingServer(View view){
        new ping(this).execute("");
    }

    //stop the server by setting stop to false
    public void stopServer(View view){
        stop = false;
    }

    //send the message from user input
    public void sendMessage(View view){
        if(stop){
            new send(this).execute("");
        }
    }

    //set the target ip from user input
    public void setIP() {
        host = ipBox.getText().toString();
    }

    //send message background thread
    private static class send extends AsyncTask<String, String, String>{

        //using WeakReference to be able to change the UI while still being a static class
        private WeakReference<MainActivity> activityReference;
        send(MainActivity activity1) {
            activityReference = new WeakReference<>(activity1);
        }

        @Override
        protected String doInBackground(String... params){
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return null;

            String[] progress = new String[1];

            /*
            get user input
            TODO fix this
            */
            String msgOut = activity.messageBox.getText().toString();

            //output the message to the socket
            try {
                if(msgOut.equals("")){
                    msgOut = "\n";
                }
                out.println(msgOut);
                out.flush();

                //echo message to displayBox
                progress[0] = msgOut;
                publishProgress(progress);
            }
            catch(Exception e){
                progress[0] = e.getMessage();
                publishProgress(progress);
                progress[0] = null;
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values){
            super.onProgressUpdate(values);

            // get a reference to the activity if it is still there
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;

            //update the activity's UI
            activity.display.setText(values[0]);
        }
    }

    //server thread for if you want to listen for a connection
    private static class receive extends AsyncTask<String, String, String> {

        //using WeakReference to be able to change the UI while still being a static class
        private WeakReference<MainActivity> activityReference;
        receive(MainActivity activity1) {
            activityReference = new WeakReference<>(activity1);
        }

        @Override
        protected String doInBackground(String... params) {

            //progress is the array used to pass String to the onProgress function to update the UI
            String[] progress = new String[1];
            progress[0] = "Starting.";
            publishProgress(progress);

            //trying to set up a socket connection on port 8008
            try {
                ServerSocket server = new ServerSocket(8008);
                progress[0] = "serverSocket created on port 8008.";
                publishProgress(progress);

                //wait for and accept a connection
                socket = server.accept();
                progress[0] = "Connected.";
                publishProgress(progress);

                //set stop to true to enable listening and sending messages
                stop = true;

                //set up a BufferedReader for text input from socket
                BufferedReader in = null;
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String msgIn = "";

                //set up a PrintWriter for text output to socket
                out = new PrintWriter(socket.getOutputStream(), true);

                while (stop) {
                    //read input from socket
                    try {
                        msgIn = in.readLine();
                        progress[0] = msgIn;

                    } catch (Exception e) {
                        progress[0] = e.getMessage();
                        publishProgress(progress);
                    }

                    if (msgIn == null) {
                        break; // disconnected todo not sure if this is good either
                    } else {
                        //display message
                        publishProgress(progress);
                    }
                }
                //cleanup
                progress[0] = "Ending connection.";
                publishProgress(progress);
                in.close();
                out.close();
                socket.close();

            } catch (Exception e) {
                progress[0] = e.getMessage();
                publishProgress(progress);
                stop = false;
            }
            progress[0] = null;
            stop = false;
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            // get a reference to the activity if it is still there
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;

            // modify the activity's UI
            activity.display.setText(values[0]);

        }
    }

    //client thread for if you want to initiate the connection
    private static class client extends AsyncTask<String, String, String> {
        //using WeakReference to be able to change the UI while still being a static class
        private WeakReference<MainActivity> activityReference;
        //new constructor for WeakReference
        client(MainActivity activity1) {
            activityReference = new WeakReference<>(activity1);
        }

        @Override
        protected String doInBackground(String... params) {
            // get a reference to the activity if it is still there
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return null;

            //progress is the array used to pass String to the onProgress function to update the UI
            String[] progress = new String[1];
            progress[0] = "Starting connection.";
            publishProgress(progress);

            //set the IP to the user input
            activity.setIP();

            try {
                //try to set up a socket connection on ip specified by user on port 8008
                progress[0] = "Attempting to connect";
                publishProgress(progress);
                socket = new Socket(host, 8008);
                progress[0] = "Connected.";
                publishProgress(progress);

                //set stop to true to enable listening and sending messages once the socket is
                //connected
                stop = true;

                //set up the BufferedReader for text input from socket
                BufferedReader in = null;
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String msgIn = "";

                //set up the PrintWriter for text output to socket
                out = new PrintWriter(socket.getOutputStream(), true);

                while (stop) {
                    //take in input
                    try {
                        msgIn = in.readLine();
                        progress[0] = msgIn;

                    } catch (IOException e) {
                        progress[0] = e.getMessage();
                        publishProgress(progress);
                    }
                    if (msgIn == null) {
                        stop = false; // disconnected
                    } else {
                        //display the message
                        progress[0] = msgIn;
                        publishProgress(progress);
                    }
                }
                //cleanup
                progress[0] = "Ending connection.";
                publishProgress(progress);
                in.close();
                out.close();
                socket.close();

            }
            //if the server doesn't connect for some reason, kill the thread
            catch(Exception e){
                progress[0] = "Couldn't connect to server. \n" + e.getMessage();
                publishProgress(progress);
                stop = false;
                return null;
            }
            progress[0] = null;
            stop = false;
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            // get a reference to the activity if it is still there
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;

            // modify the activity's UI
            activity.display.setText(values[0]);
        }
    }

    //TODO function to measure the time it takes to send a message and for the other end to echo it
    private static class ping extends AsyncTask<String, String, String>{
        //using WeakReference to be able to change the UI while still being a static class
        private WeakReference<MainActivity> activityReference;
        ping (MainActivity activity1) {
            activityReference = new WeakReference<>(activity1);
        }

        @Override
        protected String doInBackground(String... strings) {

            //getting the start time
            long time = System.nanoTime();

            //finding the total time that it takes to receive an echo
            time -= System.nanoTime() / 1000000;


            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            // get a reference to the activity if it is still there
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;

            // modify the activity's UI
            activity.display.setText(values[0]);
        }
    }

    //TODO function to output a new line to the display
    public void newLine(String line){

    }
}
