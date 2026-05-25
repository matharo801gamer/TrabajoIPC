/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
package controllers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.ArrayList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import utils.Poi;
import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.GeoUtils;
import upv.ipc.sportlib.MapProjection;
import upv.ipc.sportlib.MapRegion;
import upv.ipc.sportlib.TrackPoint;

/**
 * FXML Controller class
 *
 * @author The_T
 */
public class MapController implements Initializable {

    @FXML
    private ScrollPane mapScrollPane;
    @FXML
    private Group zoomGroup;
    @FXML
    private Pane mapPane;
    @FXML
    private ImageView mapView;
    @FXML
    private AreaChart<Number, Number> elevationChart;
    @FXML
    private NumberAxis yAxisAlt;
    @FXML
    private NumberAxis xAxisDist;
    @FXML
    private ListView<Poi> mapListView;

    private Activity currentActivity;
    private MapProjection currentProjection;
    private double currentZoom = 1.0;
    private ContextMenu mapContextMenu;
    private boolean insertionMode = false;
    
    private Circle hoverMarker;
    private List<Double> pointsDistance;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        clearView();
        MenuItem miText = new MenuItem("Añadir texto");
        MenuItem miCircle = new MenuItem("Añadir círculo");
        mapContextMenu = new ContextMenu(miText, miCircle);
        mapPane.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                // Clic derecho -> Mostrar menú contextual para añadir anotaciones
                onMapRightClick(e.getX(), e.getY());
            }
        });
        hoverMarker = new Circle(8.0, Color.ORANGE);
        hoverMarker.setStroke(Color.DARKORANGE);
        hoverMarker.setStrokeWidth(2.0);
        hoverMarker.setVisible(false); // Empieza invisible
        
        elevationChart.setOnMouseMoved(e -> handleChartHover(e));
        elevationChart.setOnMouseExited(e -> hoverMarker.setVisible(false));
    }

    /**
     * Muestra una actividad deportiva en el mapa y en el gráfico de altitud.
     * Este método será invocado cuando el usuario seleccione una actividad.
     */
    public void displayActivity(Activity activity) {
        if (activity == null) {
            clearView();
            return;
        }

        this.currentActivity = activity;
        pointsDistance = new ArrayList<>();
        mapPane.getChildren().retainAll(mapView);
        elevationChart.getData().clear();
        MapRegion region = activity.getSuggestedMap();
        if (region == null) {
            showAlert("Aviso", "No se encontró ningún mapa que cubra esta actividad.");
            return;
        }
        File imgFile = new File(region.getImagePath());
        if (!imgFile.exists()) {
            showAlert("Error", "No se encuentra la imagen del mapa en: " + imgFile.getAbsolutePath());
            return;
        }
        Image mapImage = new Image(imgFile.toURI().toString());
        mapView.setImage(mapImage);
        mapPane.setPrefSize(mapImage.getWidth(), mapImage.getHeight());
        mapPane.setMinSize(mapImage.getWidth(), mapImage.getHeight());
        mapPane.setMaxSize(mapImage.getWidth(), mapImage.getHeight());
        currentProjection = new MapProjection(region, mapImage.getWidth(), mapImage.getHeight());
        
        List<TrackPoint> points = activity.getTrackPoints();
        XYChart.Series<Number, Number> elevationSeries = new XYChart.Series<>();
        elevationSeries.setName("Altitud");
        double accumDistKm = 0.0;
        TrackPoint prevPoint = null;
        for (int i = 0; i < points.size(); i++) {
            TrackPoint tp = points.get(i);

            Point2D screenPos = currentProjection.project(tp);
            if (prevPoint != null) {
                Point2D prevScreenPos = currentProjection.project(prevPoint);
                double segmentDist = GeoUtils.distance(prevPoint, tp);
                long seconds = java.time.Duration.between(prevPoint.getTime(), tp.getTime()).toSeconds();
                double segmentSpeedKmh = 0.0;
                
                if (seconds > 0) {
                    segmentSpeedKmh = (segmentDist / seconds) * 3.6;
                }

                Color segmentColor;
                double avgSpeed = activity.getAverageSpeed();
                if (segmentSpeedKmh < 0.75 * avgSpeed) {
                    segmentColor = Color.RED;        // Tramo Lento (menos del 75%)
                } else if (segmentSpeedKmh > 1.25 * avgSpeed) {
                    segmentColor = Color.LIMEGREEN;  // Tramo Rápido (más del 125%)
                } else {
                    segmentColor = Color.GOLD;       // Tramo Medio
                }

                Line segmentLine = new Line(
                    prevScreenPos.getX(), prevScreenPos.getY(), 
                    screenPos.getX(), screenPos.getY()
                );
                segmentLine.setStroke(segmentColor);
                segmentLine.setStrokeWidth(4.0);
                segmentLine.setStrokeLineCap(StrokeLineCap.ROUND);
                
                mapPane.getChildren().add(segmentLine);
            }

            if (i == 0) {
                addMarker(screenPos, Color.GREEN, 8.0);
            } else if (i == points.size() - 1) {
                addMarker(screenPos, Color.RED, 8.0);
            }

            if (prevPoint != null) {
                accumDistKm += GeoUtils.distance(prevPoint, tp) / 1000.0;
            }
            
            pointsDistance.add(accumDistKm);

            elevationSeries.getData().add(new XYChart.Data<>(accumDistKm, tp.getElevation()));
            prevPoint = tp;
        }

        elevationChart.getData().add(elevationSeries);
        
        mapPane.getChildren().add(hoverMarker);

        currentZoom = 1.0;
        applyZoom();
    }

    private void addMarker(Point2D point, Color color, double radius) {
        Circle marker = new Circle(point.getX(), point.getY(), radius, color);
        mapPane.getChildren().add(marker);
    }

    public void clearView() {
        mapView.setImage(null);
        mapPane.getChildren().retainAll(mapView);
        elevationChart.getData().clear();
        currentActivity = null;
        currentProjection = null;
    }

    // =========================================================
    // MANEJADORES DE ZOOM
    // =========================================================
    @FXML
    private void handleZoomIn() {
        currentZoom *= 1.2;
        applyZoom();
    }

    @FXML
    private void handleZoomOut() {
        currentZoom /= 1.2;
        if (currentZoom < 0.3) {
            currentZoom = 0.3;
        }
        applyZoom();
    }

    private void applyZoom() {
        zoomGroup.setScaleX(currentZoom);
        zoomGroup.setScaleY(currentZoom);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleRegisterMap(ActionEvent event) throws IOException {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File(".")); // Empezamos en el directorio del proyecto

        File imgFile = fc.showOpenDialog(zoomGroup.getScene().getWindow());

        // FIX 3: showOpenDialog() devuelve null si el usuario cancela la selección
        if (imgFile != null) {
            System.out.println("Mapa seleccionado: " + imgFile.getCanonicalPath());
            buildMap(imgFile); // Reconstruimos la vista con la nueva imagen
            // mapListView no existe en mapPanel.fxml, por lo que causaba un NullPointerException
        }
    }

    // =========================================================
    // CONSTRUCCIÓN DEL MAPA
    // =========================================================

    /**
     * Carga una imagen y construye la jerarquía de nodos del mapa.
     *
     * Este método puede llamarse varias veces (p. ej. al cambiar el mapa),
     * ya que sustituye completamente el contenido del ScrollPane.
     *
     * @param imgFile fichero de imagen a cargar como fondo del mapa
     */
    private void buildMap(File imgFile) {
        // Comprobación defensiva: si el fichero no existe mostramos un aviso
        if (!imgFile.exists()) {
            mapScrollPane.setContent(
                    new Label("Imagen no encontrada: " + imgFile.getPath()));
            return;
        }

        // Cargamos la imagen y obtenemos sus dimensiones reales en píxeles
        Image img = new Image(imgFile.toURI().toString());
        double W = img.getWidth();
        double H = img.getHeight();

        // ── mapPane: lienzo del mapa ───────────────────────────────────
        // Usamos un Pane (y no un Group) para poder posicionar los nodos
        // hijos con coordenadas absolutas (setLayoutX / setLayoutY).
        mapPane = new Pane();
        mapPane.setPrefSize(W, H); // tamaño preferido = tamaño de la imagen
        mapPane.setMinSize(W, H); // impedimos que el layout lo encoja
        mapPane.setMaxSize(W, H); // impedimos que el layout lo agrande

        // Añadimos la imagen como fondo del Pane
        ImageView iv = new ImageView(img);
        iv.setFitWidth(W);
        iv.setFitHeight(H);
        mapPane.getChildren().add(iv);

        // ── Manejador de clics sobre el mapa ──────────────────────────
        // Gestionamos el clic derecho (menú contextual) y el clic izquierdo
        // en modo inserción (FIX 2).
        mapPane.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                // Clic derecho → mostrar menú contextual
                onMapRightClick(e.getX(), e.getY());

            } else if (e.getButton() == MouseButton.PRIMARY && insertionMode) {
                // FIX 2: clic izquierdo en modo inserción → añadir POI y desactivar modo
                insertionMode = false;
                mapPane.setStyle(""); // Restauramos el cursor normal
                addPoi(e.getX(), e.getY());
            }
        });

        // ── Jerarquía de Groups para el zoom ──────────────────────────
        // contentGroup es el nodo raíz que recibe el ScrollPane.
        // zoomGroup es el que se escala; anidar un Group dentro de otro
        // evita que el ScrollPane reajuste su contenido durante el escalado.
        zoomGroup = new Group();
        Group contentGroup = new Group();
        zoomGroup.getChildren().add(mapPane);
        contentGroup.getChildren().add(zoomGroup);

        // Aplicamos el zoom actual (valor actual del slider)
        double zoom = currentZoom;
        zoomGroup.setScaleX(zoom);
        zoomGroup.setScaleY(zoom);

        // Asignamos el contentGroup como contenido del ScrollPane
        mapScrollPane.setContent(contentGroup);

    }

    // =========================================================
    // MENÚ CONTEXTUAL (clic derecho sobre el mapa)
    // =========================================================

    /**
     * Muestra el menú contextual reutilizable en la posición del clic.
     *
     * Las acciones de los MenuItem se actualizan con las coordenadas
     * del clic actual antes de mostrar el menú.
     *
     * @param x coordenada X del clic en el sistema local del mapPane
     * @param y coordenada Y del clic en el sistema local del mapPane
     */
    private void onMapRightClick(double x, double y) {
        // FIX 6: cerramos el menú si ya estaba visible (evita instancias flotantes)
        mapContextMenu.hide();

        // Actualizamos las acciones de los items con las coordenadas actuales.
        // Usamos variables final para que el lambda pueda capturarlas.
        double clickX = x;
        double clickY = y;
        mapContextMenu.getItems().get(0).setOnAction(e -> addPoi(clickX, clickY));
        mapContextMenu.getItems().get(1).setOnAction(e -> addCircle(clickX, clickY));

        // Mostramos el menú en coordenadas de pantalla
        mapContextMenu.show(
                mapPane.getScene().getWindow(),
                mapPane.localToScreen(x, y).getX(),
                mapPane.localToScreen(x, y).getY());
    }

    private void addPoi(double x, double y) {
        Dialog<Poi> poiDialog = new Dialog<>();
        poiDialog.setTitle("Nuevo POI");
        poiDialog.setHeaderText("Introduce una etiqueta de texto en este punto:");
        ButtonType okButton = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        poiDialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);
        TextField nameField = new TextField();
        nameField.setPromptText("Nombre o etiqueta del POI");
        VBox vbox = new VBox(10, new Label("Etiqueta:"), nameField);
        poiDialog.getDialogPane().setContent(vbox);
        poiDialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButton) {
                return new Poi(nameField.getText().trim(), x, y);
            }
            return null;
        });
        Optional<Poi> result = poiDialog.showAndWait();
        if (result.isPresent()) {
            Poi poi = result.get();

            // Dibujar la etiqueta de texto sobre el mapa en las coordenadas indicadas
            Text text = new Text(poi.getCode());
            text.setX(x);
            text.setY(y - 5); // Desplazar ligeramente hacia arriba del punto exacto
            text.setFill(Color.DARKRED);
            text.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

            mapPane.getChildren().add(text);
        }
    }

    // =========================================================
    // DIÁLOGO "ACERCA DE"
    // =========================================================

    /**
     * Muestra un diálogo informativo con datos de la asignatura.
     *
     * Nota: accedemos al Stage del diálogo para poder personalizar
     * su icono, ya que Alert no expone directamente esa propiedad.
     *
     * @param event evento de acción del menú
     */
    @FXML
    private void about(ActionEvent event) {
        Alert mensaje = new Alert(Alert.AlertType.INFORMATION);

        // Personalizamos el icono de la ventana del diálogo
        Stage dialogStage = (Stage) mensaje.getDialogPane().getScene().getWindow();
        dialogStage.getIcons().add(
                new Image(getClass().getResourceAsStream("/resources/logo.png")));

        mensaje.setTitle("Acerca de");
        mensaje.setHeaderText("IPC - 2026");
        mensaje.showAndWait(); // Bloquea hasta que el usuario cierra el diálogo
    }

    // =========================================================
    // CAMBIAR EL MAPA (selector de fichero)
    // =========================================================

    /**
     * Abre un selector de fichero para que el usuario elija una imagen
     * diferente como mapa y reconstruye toda la vista.
     *
     * @param event evento de acción del menú
     * @throws IOException si hay un problema al obtener la ruta canónica
     */
    @FXML
    private void cambiarMapa(ActionEvent event) throws IOException {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File(".")); // Empezamos en el directorio del proyecto

        File imgFile = fc.showOpenDialog(zoomGroup.getScene().getWindow());

        // FIX 3: showOpenDialog() devuelve null si el usuario cancela la selección
        if (imgFile != null) {
            System.out.println("Mapa seleccionado: " + imgFile.getCanonicalPath());
            buildMap(imgFile); // Reconstruimos la vista con la nueva imagen
            mapListView.getItems().clear(); // Borramos los datos del mapa anterior
        }
    }

    // =========================================================
    // AÑADIR UN CÍRCULO AL MAPA
    // =========================================================

    /**
     * Dibuja un círculo rojo de radio 10 px en la posición indicada.
     *
     * Ejemplo sencillo de cómo añadir formas vectoriales (Shape) sobre el mapa.
     * Los alumnos pueden extenderlo para:
     * - Elegir color dinámicamente.
     * - Asociar información al círculo (tooltip, popup, etc.).
     * - Permitir moverlo con arrastrar y soltar (drag and drop).
     *
     * @param x coordenada X en el sistema local del mapPane
     * @param y coordenada Y en el sistema local del mapPane
     */
    private void addCircle(double x, double y) {
        Circle circle = new Circle(10, Color.RED); // radio = 10 px, color = rojo
        circle.setCenterX(x);
        circle.setCenterY(y);
        mapPane.getChildren().add(circle); // Se añade sobre el mapa como cualquier nodo
    }

    
    private void handleChartHover(MouseEvent event) {
        // Comprobación de seguridad
        if (currentActivity == null || currentProjection == null || pointsDistance == null || pointsDistance.isEmpty()) {
            return;
        }
        try {
            // 1. Obtener el eje X del gráfico de forma dinámica
            NumberAxis xAxis = (NumberAxis) elevationChart.getXAxis();
            // 2. Traducir la coordenada X del ratón a coordenadas locales del eje
            double localX = xAxis.sceneToLocal(event.getSceneX(), event.getSceneY()).getX();
            // 3. Traducir los píxeles al valor real del eje (Distancia en Km)
            double hoveredDistKm = xAxis.getValueForDisplay(localX).doubleValue();
            // 4. Validar que el ratón esté dentro de los límites del track
            double maxDist = pointsDistance.get(pointsDistance.size() - 1);
            if (hoveredDistKm < 0 || hoveredDistKm > maxDist) {
                hoverMarker.setVisible(false);
                return;
            }
            // 5. Buscar el índice del punto GPS más cercano a esa distancia
            int closestIndex = findClosestPointIndex(hoveredDistKm);
            if (closestIndex >= 0 && closestIndex < currentActivity.getTrackPoints().size()) {
                TrackPoint tp = currentActivity.getTrackPoints().get(closestIndex);
                // 6. Proyectar las coordenadas GPS de ese punto en píxeles del mapa
                Point2D screenPos = currentProjection.project(tp);
                // 7. Colocar el marcador y hacerlo visible
                hoverMarker.setCenterX(screenPos.getX());
                hoverMarker.setCenterY(screenPos.getY());
                hoverMarker.setVisible(true);
            }
        } catch (Exception ex) {
            // En caso de que el ratón pase por zonas fuera de los ejes (ej. márgenes)
            hoverMarker.setVisible(false);
        }
    }
    private int findClosestPointIndex(double targetDist) {
        int closestIdx = 0;
        double minDiff = Double.MAX_VALUE;
        // Búsqueda lineal súper rápida
        for (int i = 0; i < pointsDistance.size(); i++) {
            double diff = Math.abs(pointsDistance.get(i) - targetDist);
            if (diff < minDiff) {
                minDiff = diff;
                closestIdx = i;
            }
        }
        return closestIdx;
    }
    
}
