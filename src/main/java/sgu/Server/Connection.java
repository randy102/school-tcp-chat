package sgu.Server;

import org.json.JSONException;
import org.json.JSONObject;
import sgu.Common.Command;
import sgu.Common.Message;

import java.io.*;
import java.net.Socket;

public class Connection {
    BufferedReader in;
    BufferedWriter out;
    final Socket socket;
    final String id;

    public interface MessageHandler {
        void onRequest(Message message) throws IOException;
    }

    public Connection(Socket s, String id) throws IOException {
        this.in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        this.out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
        this.socket = s;
        this.id = id;
    }

    public void send(Command command) throws IOException {
        send(new Message(command));
    }

    public void send(Command command, String data) throws IOException {
        send(new Message(command, data));
    }

    public void send(Message message) throws IOException {
        System.out.println("Sending " + message.toString() + " to " + this.id);
        this.out.write(message.toString());
        this.out.newLine();
        this.out.flush();
    }

    public void handle(MessageHandler messageHandler) throws IOException {
        System.out.println("Client " + id + " joined!");
        try {
            String input = "";
            do {
                input = in.readLine();
                if(input == null) break;
                System.out.println("Received: " + input + " from " + id);
                try {
                    JSONObject json = new JSONObject(input);
                    messageHandler.onRequest(new Message(json));
                } catch (IOException e){
                    this.send(new Message(Command.ERROR, e.getMessage()));
                }
            } while (!input.equals("bye"));
            System.out.println("Closed socket for client " + id + " " + socket.toString());

        } catch (IOException | JSONException e) {
            System.out.println(e);
        } finally {
            close();
        }
    }

    public void close() throws IOException {
        in.close();
        out.close();
        socket.close();
    }
}
