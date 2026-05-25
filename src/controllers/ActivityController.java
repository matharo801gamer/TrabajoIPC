/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package controllers;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.SportActivityApp;
import java.util.ArrayList;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;

/**
 * FXML Controller class
 */
public class ActivityController implements Initializable {

    /**
     * Initializes the controller class.
     */
    @FXML private ListView<Activity> activityList;
    @FXML private Button btnImport, btnDelete;
    @FXML private VBox statsBox;

    @FXML private Label lblActivityName, lblDistancia, lblDuracion, lblVelocidad; 
    @FXML private Label lblAltMin, lblAltMax, lblRitmo, lblDesnPos, lblDesnNeg;
    @FXML private Label lblNumActs, lblTotalLoss, lblTotalGain, lblTotalTime, lblTotalDist;

    @FXML private ComboBox<String> monthSelector;
    @FXML
    private VBox acumuladoBox;
    @FXML
    private Label mothSelector;
    @FXML
    private StackPane mapContainer;
    private MapController mapPanelController;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        SportActivityApp app = SportActivityApp.getInstance();

        // Ocultar stats
        statsBox.setVisible(true);
        statsBox.setManaged(true);

        // Cargar actividad
        List<Activity> acts = app.getUserActivities();
        activityList.getItems().setAll(acts);

        // Celda
        activityList.setCellFactory(lv -> new ListCell<Activity>() {
            @Override
            protected void updateItem(Activity item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String fecha = item.getStartTime()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    String dist = String.format("%.2f km", item.getTotalDistance() / 1000.0);
                    setText(item.getName() + "\n" + fecha + "  ·  " + dist);
                }
            }
        });
        
        // Deshabilitar boton eliminar
        btnDelete.setDisable(true);
        btnDelete.setOnAction(this::handleDelete);
        
        // Listener para la actividad
        activityList.getSelectionModel().selectedItemProperty()
            .addListener((obs, old, selected) -> {
                btnDelete.setDisable(selected == null);
                if (selected != null) {
                    statsBox.setVisible(true);
                    statsBox.setManaged(true);
                    mostrarEstadisticas(selected);
                    
                    if (mapPanelController != null) {
                        mapPanelController.displayActivity(selected);
                    }
                }
            });        
        cargarSelectorMeses(acts);
        
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/views/components/mapPanel.fxml")
            );
            javafx.scene.Parent mapNode = loader.load();
            this.mapPanelController = loader.getController();
            mapContainer.getChildren().add(mapNode);
        } catch (java.io.IOException e) {
            System.err.println("Error al cargar el mapa dinámicamente: " + e.getMessage());
            e.printStackTrace();
        }
        
    }

    @FXML
    private void handleImport(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar fichero GPX");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("GPX Files", "*.gpx")
        );
        File file = fc.showOpenDialog(btnImport.getScene().getWindow());

        if (file != null) {
            try {
                SportActivityApp app = SportActivityApp.getInstance();
                Activity nueva = app.importActivity(file);
                if (nueva != null) {
                    activityList.getItems().add(0, nueva);
                    activityList.getSelectionModel().select(nueva);
                    cargarSelectorMeses(activityList.getItems().stream().collect(Collectors.toList()));
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Actividad importada");
                    alert.setHeaderText(null);
                    alert.setContentText("\"" + nueva.getName() + "\" importada correctamente.");
                    alert.showAndWait();
                }   
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("No se pudo importar el fichero GPX: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        Activity selected = activityList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Eliminar actividad");
            confirm.setHeaderText(null);
            confirm.setContentText("¿Seguro que quieres eliminar \"" + selected.getName() + "\"?");
            
            java.util.Optional<javafx.scene.control.ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
                SportActivityApp app = SportActivityApp.getInstance();
                app.removeActivity(selected);
                activityList.getItems().remove(selected);
                statsBox.setVisible(false);
                statsBox.setManaged(false);
                cargarSelectorMeses(activityList.getItems());
            }
        }
    }

    private void mostrarEstadisticas(Activity act) {
        // Nombre
        lblActivityName.setText(act.getName());
    
        // Distancia
        lblDistancia.setText(String.format("%.2f km", act.getTotalDistance() / 1000.0));
    
        // Duracion
        Duration dur = act.getDuration();
        long h = dur.toHours();
        long m = dur.toMinutesPart();
        long s = dur.toSecondsPart();
        lblDuracion.setText(String.format("%02d:%02d:%02d", h, m, s));
    
        // Velocidad
        lblVelocidad.setText(String.format("%.1f km/h", act.getAverageSpeed()));
        
        // RItmo
        lblRitmo.setText(formatPace(act.getAveragePace()));
    
        // Desnivel
        lblDesnPos.setText(String.format("+ %.0f m", act.getElevationGain()));
        lblDesnNeg.setText(String.format("- %.0f m", act.getElevationLoss()));
        lblAltMin.setText(String.format("%.0f m", act.getMinElevation()));
        lblAltMax.setText(String.format("%.0f m", act.getMaxElevation()));
    }

    private String formatPace(double paceMinPerKm) {
        int min = (int) paceMinPerKm;
        int sec = (int) Math.round((paceMinPerKm - min) * 60);
        return min + "'" + String.format("%02d", sec) + "\" /km";
    }

    private void cargarSelectorMeses(List<Activity> acts) {
        List<String> meses = new ArrayList<>();
        for (Activity a : acts) {
            String mes = a.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            if (!meses.contains(mes)) {
                meses.add(mes);
            }
        }
        
        for (int i = 0; i < meses.size() - 1; i++) {
            for (int j = i + 1; j < meses.size(); j++) {
                if (meses.get(i).compareTo(meses.get(j)) < 0) {
                    String temp = meses.get(i);
                    meses.set(i, meses.get(j));
                    meses.set(j, temp);
                }
            }
        }

        monthSelector.getItems().setAll(meses);
        if (!meses.isEmpty()) {
            monthSelector.setValue(meses.get(0));
            calcularAcumulado(meses.get(0), acts);
        }

        monthSelector.setOnAction(e ->
            calcularAcumulado(monthSelector.getValue(), acts));
    }

    private void calcularAcumulado(String mesAnyo, List<Activity> todasLasActs) {
        List<Activity> delMes = new ArrayList<>();
        for (Activity a : todasLasActs) {
            String mes = a.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            if (mes.equals(mesAnyo)) {
                delMes.add(a);
            }
        }

        double distTotal = 0;
        Duration durTotal = Duration.ZERO;
        double ascTotal = 0;
        double descTotal = 0;

        for (Activity a : delMes) {
            distTotal += a.getTotalDistance();
            durTotal = durTotal.plus(a.getDuration());
            ascTotal += a.getElevationGain();
            descTotal += a.getElevationLoss();
        }

        distTotal = distTotal / 1000.0;
        
        // Actualizar labels
        lblNumActs.setText(delMes.size() + " actividades");
        lblTotalDist.setText(String.format("%.2f km", distTotal));
        lblTotalTime.setText(formatDuration(durTotal));
        lblTotalGain.setText(String.format("+ %.0f m", ascTotal));
        lblTotalLoss.setText(String.format("- %.0f m", descTotal));
    }

    private String formatDuration(Duration dur) {
        long h = dur.toHours();
        long m = dur.toMinutesPart();
        long s = dur.toSecondsPart();
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}