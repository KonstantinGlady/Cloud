package org.gik.cloud.storage.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ResourceBundle;

import org.gik.cloud.storage.common.MessageType;

public class Controller implements Initializable {

    @FXML
    public HBox authPanel;
    @FXML
    public HBox cloudPanel;

    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passField;
    @FXML
    public ListView<String> filesListClient;
    @FXML
    public ListView<String> filesListServer;

    private MessageService mService;
    private String userDir;
    private String leftListItem;
    private String rightListItem;

    public void sendAuth(ActionEvent event) throws Exception {
        String login = loginField.getText();
        String pass = passField.getText();
        mService.sendMessage(MessageType.AUTH, login, pass);
        setUserDir(login);
    }

    public void copyFromClient(ActionEvent event) {
    }

    public void moveFromClient(ActionEvent event) {
    }

    public void deleteOnClient(ActionEvent event) {
    }

    public void copyFromServer(ActionEvent event) throws Exception {
        mService.sendMessage(MessageType.SEND_FILE_FROM_SERVER, rightListItem);
    }

    public void moveFromServer(ActionEvent event) {
    }

    public void DeleteOnServer(ActionEvent event) {
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mService = new MessageService(this);
    }

    public void reloadUI() throws Exception {
        reloadUILocal();
        reloadUIServer();
    }

    public void reloadUIServer() throws Exception {
        mService.sendMessage(MessageType.GET_DIR, userDir);
    }

    public void reloadUILocal() throws IOException {
        filesListClient.getItems().clear();
        Files.list(Paths.get("localStorage/" +userDir))
                .map(p -> p.getFileName().toString())
                .forEach(o -> filesListClient.getItems().add(o));
    }

    public void setUserDir(String userDir) {
        this.userDir = userDir;
    }

    public String getUserDir() {
        return userDir;
    }

    public void LeftItemListClicked(MouseEvent mouseEvent) {
       leftListItem = filesListClient.getSelectionModel().getSelectedItem();
    }

    public void rightItemListClicked(MouseEvent mouseEvent) {
        rightListItem = filesListServer.getSelectionModel().getSelectedItem();
    }
}
