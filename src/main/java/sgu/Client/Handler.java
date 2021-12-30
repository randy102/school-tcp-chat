package sgu.Client;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.json.JSONException;
import org.json.JSONObject;
import sgu.Client.utils.Modal;
import sgu.Server.libs.Command;
import sgu.Server.libs.Message;

import java.io.BufferedReader;
import java.io.IOException;

class Handler implements Runnable {
    private final BufferedReader in;
    private boolean isRegistered = false;

    Stage registerStage;
    Stage chatStage;
    Text targetText;
    Button leaveBtn;
    String username;

    public Handler(BufferedReader i, Stage registerStage) {
        this.in = i;
        this.registerStage = registerStage;
    }

    public void run() {
        try {
            while (true) {
                String input = in.readLine();
                System.out.println("Receive: " + input);
                if (input == null) break;

                JSONObject json = new JSONObject(input);
                Message message = new Message(json);
                Command command = message.cmd();
                String data = message.data();

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            switch (command) {
                                case REGISTER_SUCCESS -> onRegisterSuccess(data);
                                case USERNAME_EXISTED -> onUsernameExisted();
                                case INVITE -> onInvite(data);
                                case WAITING -> onWaiting();
                                case PAIRED -> onPaired(data);
                                case PAIR_FAIL -> onPareFail();
                            }
                        } catch (Exception e) {
                            Modal.showError(e.getMessage());
                        }
                    }
                });


            }
        } catch (IOException | JSONException e) {
            Modal.showError(e.getMessage());
        }
    }

    private void onUsernameExisted() {
        Modal.showError("Username existed!");
    }

    private void onInvite(String target) throws IOException {
        if (!isRegistered) {
            launchChatWindow();
            isRegistered = true;
        }

        ButtonType result = Modal.showConfirm("Do you want to pair with " + target + "?", "Invitation");
        if (result == ButtonType.OK) {
            Connection.send(Command.ACCEPT, target);
        } else {
            Connection.send(Command.REJECT, target);
        }

    }

    private void launchChatWindow() throws IOException {
        registerStage.close();
        FXMLLoader fxmlLoader = new FXMLLoader(Chat.class.getResource("chat.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        Stage stage = new Stage();
        stage.setTitle("Register");
        stage.setScene(scene);
        stage.show();
        stage.setOnCloseRequest(windowEvent -> {
            Connection.close();
        });
        this.chatStage = stage;

        Text usernameText = (Text) scene.lookup("#username");
        usernameText.setText(this.username);

        this.targetText = (Text) scene.lookup("#target");
        this.leaveBtn = (Button) scene.lookup("#leaveBtn");
        this.leaveBtn.setOnAction(actionEvent -> {
            Connection.send(Command.LEAVE);
        });
    }

    private void onRegisterSuccess(String username) {
        this.username = username;
    }

    private void onWaiting() throws IOException {
        if (this.chatStage == null) {
            launchChatWindow();
        }
        this.targetText.setText("Waiting...");
        this.leaveBtn.setVisible(false);
    }

    private void onPaired(String target){
        this.targetText.setText(target);
        this.leaveBtn.setVisible(true);
    }

    private void onPareFail(){
        Modal.showError("Can not pair with user!");
    }
}