/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package controllers;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;

/**
 * FXML Controller class
 *
 * @author aleja
 */
public class ProfileController implements Initializable {

    @FXML
    private ImageView imgAvatar;
    @FXML
    private TextField txtUsuario;
    @FXML
    private TextField txtEmail;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private DatePicker datePicker;
    @FXML
    private TextField txtAvatarPath;
    
    private MainController mainController;
    private SportActivityApp app;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        app = SportActivityApp.getInstance();
        User user = app.getCurrentUser();
        
        txtUsuario.setText(user.getNickName());
        txtEmail.setText(user.getEmail());
        datePicker.setValue(user.getBirthDate());
        String avatarPath = user.getAvatarPath();
        if(avatarPath != null && !avatarPath.isBlank()){
            txtAvatarPath.setText(avatarPath);
            File f = new File(avatarPath);
            if(f.exists()){
                imgAvatar.setImage(new Image(f.toURI().toString()));
            }
        }
    }    

    @FXML
    private void handleSelectAvatar(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar imagen de avatar");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = fc.showOpenDialog(txtUsuario.getScene().getWindow());
        if(file != null){
            txtAvatarPath.setText(file.getAbsolutePath());
            imgAvatar.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML
    private void handleSave(ActionEvent event) {
        String email = txtEmail.getText().trim();
        String passInput = txtPassword.getText();
        LocalDate birth = datePicker.getValue();
        String avatarPath = txtAvatarPath.getText().trim();
        
        //Validaciones
        
        //Validamos el email
        if(!User.checkEmail(email)){
            showError("Email inválido", "El formato debe ser usuario@dominio.");
            return;
        }
        
        //Si la contraseña no está vacía la validamos también
        String passToSave;
        if(passInput.isBlank()){
            
            //Mantenemos la contraseña actual si no se quiere cambiar
            passToSave = app.getCurrentUser().getPassword();
        } else{
            if(!User.checkPassword(passInput)){
                showError("Contraseña débil", "Debe tener 8-20 caracteres, incluir mayúsculas, minúsculas, dígitos y símbolos.");
                return;
            }
            passToSave = passInput;
        }
        if(birth == null || !User.isOlderThan(birth, 12)){
            showError("Edad insuficiente", "Debes tener estrictamente más de 12 años.");
            return;
        }
        
        boolean updated = app.updateCurrentUser(email, passToSave, birth, avatarPath);
        if (updated){
            new Alert(Alert.AlertType.INFORMATION, "Perfil Actualizado con éxito.").showAndWait();
            if(mainController != null){
                //mainController.updateNavState();
            }else{
                showError("Error", "No se pudo actualizar el perfil.");
            }
        }
    }
    
    private void showError(String header, String content){
        
        //Creamos un objeto de la clase alerta
        Alert alert = new Alert(Alert.AlertType.WARNING);
        
        //Rellenamos los atributos
        alert.setTitle("Validación de credenciales");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
}