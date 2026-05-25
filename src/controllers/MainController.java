package controllers;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import upv.ipc.sportlib.SportActivityApp;

public class MainController implements Initializable {

    @FXML
    private StackPane contentArea;
    
    private SportActivityApp app;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        app = SportActivityApp.getInstance();
        showActivities(null);
    }    

    @FXML
    private void showActivities(ActionEvent event) {
        loadView("/views/components/activityPanel.fxml");
    }

    @FXML
    private void showProfile(ActionEvent event) {
        loadView("/views/Profile.fxml");
    }

    @FXML
    private void showSessions(ActionEvent event) {
        loadView("/views/components/sessionPanel.fxml");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        app.logout();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 700));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadView(String fxml) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxml));
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
