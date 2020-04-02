package org.gik.cloud.storage.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.gik.cloud.storage.client.controller.Controller;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
        Parent root = loader.load();
        stage.setTitle("My Cloud");
        stage.setScene(new Scene(root));
        Controller controller = loader.getController();
        stage.setOnHidden(e->controller.shutDown());
        stage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
