package sgu.Client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Chat extends Application {
    @Override
    public void start(Stage stage) throws IOException, InterruptedException {
        Connection connection = new Connection(stage);
        connection.start();

        FXMLLoader fxmlLoader = new FXMLLoader(Chat.class.getResource("register.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Register");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop(){
        System.out.println("Chat is closing");
        Connection.close();
    }

    public static void main(String[] args) {
        launch();
    }
}