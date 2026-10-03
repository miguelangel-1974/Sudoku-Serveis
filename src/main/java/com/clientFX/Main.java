package com.clientFX;

import org.json.JSONObject;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

// Classe principal del client. Extén 'Application', la classe base d'una app JavaFX.
// S'encarrega de crear la finestra, carregar les tres vistes, connectar-se al servidor
// i repartir els missatges que arriben als controladors.
public class Main extends Application {

    // Connexió amb el servidor (la classe UtilsWS ens ajuda a gestionar-la)
    public static UtilsWS wsClient;

    // Controladors de les tres vistes. Són públics i estàtics perquè
    // els altres arxius els puguin fer servir.
    public static CtrlConfig ctrlConfig;
    public static CtrlGame ctrlGame;
    public static CtrlRanking ctrlRanking;

    // Punt d'entrada del programa: arrenca JavaFX, que després crida 'start'
    public static void main(String[] args) {
        launch(args);
    }

    // JavaFX crida aquest mètode automàticament quan l'aplicació s'inicia
    @Override
    public void start(Stage stage) throws Exception {

        // Afegim les tres vistes. La primera és la que es veu al començar.
        // Cada vista té un nom i l'arxiu .fxml del qual es carrega.
        UtilsViews.addView(getClass(), "ViewConfig", "/assets/viewConfig.fxml");
        UtilsViews.addView(getClass(), "ViewGame", "/assets/viewGame.fxml");
        UtilsViews.addView(getClass(), "ViewRanking", "/assets/viewRanking.fxml");

        // Guardem el controlador de cada vista per poder-los cridar més endavant
        ctrlConfig = (CtrlConfig) UtilsViews.getController("ViewConfig");
        ctrlGame = (CtrlGame) UtilsViews.getController("ViewGame");
        ctrlRanking = (CtrlRanking) UtilsViews.getController("ViewRanking");

        // Creem l'escena (el contingut de la finestra) de 600 d'ample per 450 d'alt
        Scene scene = new Scene(UtilsViews.parentContainer, 600, 450);

        // Posem l'escena a la finestra i la mostrem
        stage.setScene(scene);
        stage.setTitle("Sudoku");
        stage.show();
    }

    // JavaFX crida aquest mètode quan es tanca la finestra:
    // tanquem la connexió amb el servidor i aturem el programa
    @Override
    public void stop() {
        if (wsClient != null) {
            wsClient.forceExit();
        }
        System.exit(0);
    }

    // Executa una acció passats uns mil·lisegons (la fem servir per mostrar missatges un moment)
    public static void pauseDuring(long milliseconds, Runnable action) {
        PauseTransition pause = new PauseTransition(Duration.millis(milliseconds));
        pause.setOnFinished(event -> Platform.runLater(action));
        pause.play();
    }

    // Es crida quan l'usuari prem el botó "Connect" de la primera vista
    public static void connectToServer() {

        // Si no ha escrit el nom, no ens connectem i li avisem
        if (ctrlConfig.txtName.getText().isBlank()) {
            ctrlConfig.txtMessage.setTextFill(Color.RED);
            ctrlConfig.txtMessage.setText("Escriu un nom");
            return;
        }

        ctrlConfig.txtMessage.setTextFill(Color.BLACK);
        ctrlConfig.txtMessage.setText("Connecting ...");

        // Esperem 1,5 segons perquè es vegi el missatge "Connecting ..." i després ens connectem
        pauseDuring(1500, () -> {

            // Construïm l'adreça del servidor amb el que ha escrit l'usuari (ws://localhost:3000)
            String protocol = ctrlConfig.txtProtocol.getText();
            String host = ctrlConfig.txtHost.getText();
            String port = ctrlConfig.txtPort.getText();
            wsClient = UtilsWS.getSharedInstance(protocol + "://" + host + ":" + port);

            // Indiquem què fem quan arriba un missatge o hi ha un error.
            // Platform.runLater fa que el codi s'executi al fil de la interfície,
            // perquè JavaFX només deixa canviar la pantalla des d'aquest fil.
            wsClient.onMessage((response) -> { Platform.runLater(() -> { wsMessage(response); }); });
            wsClient.onError((response) -> { Platform.runLater(() -> { wsError(response); }); });
        });
    }

    // Es crida cada vegada que arriba un missatge del servidor
    private static void wsMessage(String response) {
        // Convertim el text rebut en un objecte JSON i mirem de quin tipus és
        JSONObject msgObj = new JSONObject(response);
        String type = msgObj.getString("type");

        // Si encara estem a la primera vista, aquest és el primer missatge: vol dir que
        // ja estem connectats. Aleshores diem el nostre nom al servidor i passem a la vista del joc.
        if ("ViewConfig".equals(UtilsViews.getActiveView())) {
            String name = ctrlConfig.txtName.getText();

            JSONObject join = new JSONObject();
            join.put("type", "join");
            join.put("name", name);
            wsClient.safeSend(join.toString());

            ctrlGame.setName(name);
            UtilsViews.setView("ViewGame");
        }

        // Passem el missatge als controladors perquè actualitzin el taulell i les llistes de jugadors
        ctrlGame.receiveMessage(msgObj);
        ctrlRanking.receiveMessage(msgObj);

        // Si el sudoku s'ha acabat, mostrem la vista amb la llista de jugadors
        if (type.equals("end")) {
            UtilsViews.setView("ViewRanking");
        }
    }

    // Es crida quan hi ha un error de connexió
    private static void wsError(String response) {
        // Si el servidor no està engegat, ho mostrem a la primera vista durant un moment
        String connectionRefused = "Connection refused";
        if (response.indexOf(connectionRefused) != -1) {
            ctrlConfig.txtMessage.setTextFill(Color.RED);
            ctrlConfig.txtMessage.setText(connectionRefused);
            pauseDuring(1500, () -> {
                ctrlConfig.txtMessage.setText("");
            });
        }
    }
}