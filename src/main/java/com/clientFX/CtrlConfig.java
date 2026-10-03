package com.clientFX;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

// Controlador de la vista 1 (viewConfig.fxml): configuració del servidor i nom del jugador
public class CtrlConfig implements Initializable {

    // Els camps següents estan enllaçats amb els elements de viewConfig.fxml
    // que tenen el mateix 'fx:id'. Són públics perquè Main els pugui llegir.

    // Camp del protocol (ws)
    @FXML
    public TextField txtProtocol;

    // Camp de l'adreça del servidor (localhost)
    @FXML
    public TextField txtHost;

    // Camp del port (3000)
    @FXML
    public TextField txtPort;

    // Camp on el jugador escriu el seu nom
    @FXML
    public TextField txtName;

    // Etiqueta on es mostren missatges ("Connecting ...", errors...)
    @FXML
    public Label txtMessage;

    // JavaFX el crida quan la vista s'ha carregat. Aquí no cal configurar res.
    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }

    // Acció del botó "Connect" (definida a viewConfig.fxml amb onAction="#connectToServer")
    @FXML
    private void connectToServer() {
        Main.connectToServer();
    }
}