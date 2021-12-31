package sgu.Client;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import sgu.Common.Command;

public class RegisterController {
    @FXML
    protected TextField nameInput;

    @FXML
    protected void onRegisterClick() {
        Connection.send(Command.REGISTER, nameInput.getText());
    }
}