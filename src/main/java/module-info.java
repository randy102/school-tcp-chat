module sgu.networkingfinalclient {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires java.json;

    opens sgu.Client to javafx.fxml;
    exports sgu.Client;
}