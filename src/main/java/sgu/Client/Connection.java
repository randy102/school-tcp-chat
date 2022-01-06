package sgu.Client;


import javafx.scene.control.Alert;
import javafx.stage.Stage;
import sgu.Common.Command;
import sgu.Common.Message;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Connection {
    private static String host = "localhost";
    private static int port = 1234;
    private static Socket socket;

    private static BufferedWriter out;
    private static BufferedReader in;

    private Stage registerStage;

    public Connection(Stage registerStage){
        this.registerStage = registerStage;
    }

    public static void send(Command command)  {
        send(new Message(command));
    }

    public static void send(Command command, String data) {
        send(new Message(command, data));
    }

    public static void send(Message message) {
        try{
            System.out.println("Send: " + message);
            out.write(message.toString());
            out.newLine();
            out.flush();
        } catch (Exception e){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    public static void close(){
        try {
            send(Command.CLOSE);
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() throws IOException, InterruptedException {
        socket = new Socket(host, port);
        System.out.println("Client connected");
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        Thread thread = new Thread(new Handler(in, registerStage));
        thread.start();
    }
}