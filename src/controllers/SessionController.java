package controllers;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import upv.ipc.sportlib.Session;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.User;

public class SessionController implements Initializable {

    @FXML private TableView<Session> sessionsTable;
    @FXML private TableColumn<Session, String> colInicio;
    @FXML private TableColumn<Session, String> colFin;
    @FXML private TableColumn<Session, String> colDuracion;
    @FXML private TableColumn<Session, String> colImportadas;
    @FXML private TableColumn<Session, String> colVistas;
    @FXML private TableColumn<Session, String> colAnotaciones;

    @FXML private Label lblTotalSesiones;
    @FXML private Label lblTotalImportadas;
    @FXML private Label lblTotalVistas;
    @FXML private Label lblTotalAnotaciones;

    private SportActivityApp app;
    private DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        app = SportActivityApp.getInstance();

        // Configurar el contenido de las columnas
        colInicio.setCellValueFactory(c -> {
            LocalDateTime dt = c.getValue().getStartTime();
            return new SimpleStringProperty(dt != null ? dt.format(dtf) : "");
        });

        colFin.setCellValueFactory(c -> {
            LocalDateTime dt = c.getValue().getEndTime();
            return new SimpleStringProperty(dt != null ? dt.format(dtf) : "");
        });

        colDuracion.setCellValueFactory(c -> {
            Duration d = c.getValue().getDuration();
            if (d == null) return new SimpleStringProperty("");
            long s = d.getSeconds();
            return new SimpleStringProperty(String.format("%dh %02dm %02ds", s / 3600, (s % 3600) / 60, (s % 60)));
        });

        colImportadas.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getImportedActivities())));
        colVistas.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getViewedActivities())));
        colAnotaciones.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getAnnotationsCreated())));

        cargarSesionesBackground();
    }
    
    private void cargarSesionesBackground() {
        // Se usa una Task para realizar el trabajo pesado en segundo plano
        Task<List<Session>> task = new Task<List<Session>>() {
            @Override
            protected List<Session> call() throws Exception {
                User user = app.getCurrentUser();
                return app.getSessionsByUser(user);
            }
        };

        // Cuando la tarea termina, se actualiza el GUI en el hilo principal
        task.setOnSucceeded(e -> {
            List<Session> sesiones = task.getValue();
            if (sesiones != null) {
                ObservableList<Session> obsList = FXCollections.observableArrayList(sesiones);
                sessionsTable.setItems(obsList);
                calcularTotales(sesiones);
            }
        });

        // Lanzar la tarea en un hilo secundario
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private void calcularTotales(List<Session> sesiones) {
        int tSesiones = sesiones.size();
        int tImportadas = 0;
        int tVistas = 0;
        int tAnotaciones = 0;

        for (Session s : sesiones) {
            tImportadas += s.getImportedActivities();
            tVistas += s.getViewedActivities();
            tAnotaciones += s.getAnnotationsCreated();
        }

        lblTotalSesiones.setText(String.valueOf(tSesiones));
        lblTotalImportadas.setText(String.valueOf(tImportadas));
        lblTotalVistas.setText(String.valueOf(tVistas));
        lblTotalAnotaciones.setText(String.valueOf(tAnotaciones));
    }
}
