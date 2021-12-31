package sgu.Client;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import org.json.JSONException;
import org.json.JSONObject;
import sgu.Client.utils.Modal;
import sgu.Common.Command;
import sgu.Common.Message;

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
    TextArea chatInput;
    VBox chatWindow;
    ScrollPane chatScroll;

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
                                case PAIR_FAIL -> onPairFail();
                                case MESSAGE -> onReceiveMessage(data);
                            }
                        } catch (Exception e) {
                            Modal.showError(e.getMessage());
                        }
                    }
                });


            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
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
        this.chatStage = stage;

        Text usernameText = (Text) scene.lookup("#username");
        usernameText.setText(this.username);

        this.targetText = (Text) scene.lookup("#target");
        this.leaveBtn = (Button) scene.lookup("#leaveBtn");
        this.leaveBtn.setOnAction(actionEvent -> {
            Connection.send(Command.LEAVE);
        });
        this.chatInput = (TextArea) scene.lookup("#chatInput");
        this.chatInput.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                onSendMessage(this.chatInput.getText());
            }
        });
        this.chatWindow = (VBox) scene.lookup("#chatWindow");
        this.chatScroll = (ScrollPane) scene.lookup("#chatScroll");
        this.chatWindow.heightProperty().addListener(observable -> chatScroll.setVvalue(1D));
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
        this.chatInput.setDisable(true);
    }

    private void onPaired(String target) {
        this.targetText.setText(target);
        this.leaveBtn.setVisible(true);
        this.chatWindow.getChildren().clear();
        this.chatInput.setDisable(false);
    }

    private void onPairFail() {
        Modal.showError("Can not pair with user!");
    }

    private void onReceiveMessage(String data) {
        this.addServerMessage(data);
    }

    private void onSendMessage(String data) {
        Connection.send(Command.MESSAGE, data);
        this.addSelfMessage(data);
    }

    private void addMessage(String data, Pos pos, String color) {
        HBox hBox = new HBox();
        hBox.setAlignment(pos);
        hBox.setPadding(new Insets(5,5,5,5));

        TextFlow textFlow = new TextFlow(new Text(data));
        textFlow.setPadding(new Insets(15, 10, 0, 10));
        textFlow.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 20px");

        hBox.getChildren().add(textFlow);
        this.chatWindow.getChildren().add(hBox);
        this.chatInput.clear();
    }

    private void addServerMessage(String data) {
        this.addMessage(data, Pos.CENTER_LEFT, "rgb(102, 224, 248)");
    }

    private void addSelfMessage(String data) {
        this.addMessage(data, Pos.CENTER_RIGHT, "rgb(218, 218, 218)");
    }
}