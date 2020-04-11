package org.gik.cloud.storage.client.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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
    public ListView<String> fileListClient;
    @FXML
    public ListView<String> fileListServer;

    private static Controller instance;
    private MessageService mService;
    private String userDir;
    private String leftListItem;
    private String rightListItem;

    public static final String LOCAL_STORAGE = "localStorage/";

    public Controller() {
    }

    public static Controller getInstance() {
        if (instance == null) {
            instance = new Controller();
        }
        return instance;
    }

    public static void warningWindow() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Authentication is failed");
        alert.setContentText("Wrong login or password. Try again!");
        alert.showAndWait();
    }

    public void sendAuth(ActionEvent event) throws Exception {
        String login = loginField.getText();
        String pass = passField.getText();
        mService.sendMessage(MessageType.AUTH, login, pass);
        setUserDir(login);
    }

    public void copyFromClient(ActionEvent event) throws Exception {
        mService.sendMessage(MessageType.COPY_FILE_TO_SERVER, leftListItem);
    }

    public void moveFromClient(ActionEvent event) throws Exception {
        mService.sendMessage(MessageType.MOVE_FILE_TO_SERVER, leftListItem);
    }

    public void deleteOnClient(ActionEvent event) throws IOException {
        String file = fileListClient.getSelectionModel().getSelectedItem();
        deleteFile(file);
    }

    public void deleteFile(String file) throws IOException {
        Path path = Paths.get(LOCAL_STORAGE + userDir + "/" + file);
        Files.delete(path);
        fileListClient.getItems().remove(file);
    }

    public void copyFromServer(ActionEvent event) throws Exception {
        mService.sendMessage(MessageType.COPY_FILE_FROM_SERVER, rightListItem);
    }

    public void moveFromServer(ActionEvent event) throws Exception {
        mService.sendMessage(MessageType.MOVE_FILE_FROM_SERVER, rightListItem);
    }

    public void deleteOnServer(ActionEvent event) throws Exception {
        mService.sendMessage(MessageType.DELETE, rightListItem);
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;
        mService = new MessageService();

    }

    public void reloadUI() throws Exception {
        reloadUILocal();
        reloadUIServer();
    }

    public void reloadUIServer() throws Exception {
        fileListServer.getItems().clear();
        mService.sendMessage(MessageType.GET_DIR, userDir);
    }

    public void reloadUILocal() throws Exception {
        fileListClient.getItems().clear();
        Files.list(Paths.get(LOCAL_STORAGE + userDir))
                .map(p -> p.getFileName().toString())
                .forEach(o -> fileListClient.getItems().add(o));
    }

    public void setUserDir(String userDir) {
        this.userDir = userDir;
    }

    public String getUserDir() {
        return (LOCAL_STORAGE + userDir + "/");
    }

    public void LeftItemListClicked(MouseEvent mouseEvent) {
        leftListItem = fileListClient.getSelectionModel().getSelectedItem();
    }

    public void rightItemListClicked(MouseEvent mouseEvent) {
        rightListItem = fileListServer.getSelectionModel().getSelectedItem();
    }

    public void RefreshList(ActionEvent event) throws Exception {
        reloadUI();
    }

    public void shutDown() {
        mService.close();
    }

    public void clearServerList() {
        Platform.runLater(() -> fileListServer.getItems().clear());
    }
}
