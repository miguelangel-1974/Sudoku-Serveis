package com.clientFX;

import java.net.URL;
import java.util.ResourceBundle;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;

// Controlador de la vista 3 (viewRanking.fxml): llista de jugadors i botó per tornar a jugar
public class CtrlRanking implements Initializable {

    // Llista de jugadors amb els seus punts (enllaçada amb el fx:id de viewRanking.fxml)
    @FXML
    private ListView<String> listRanking;

    // JavaFX el crida quan la vista s'ha carregat. Aquí no cal configurar res.
    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }

    // Es crida quan arriba un missatge del servidor (el crida Main):
    // actualitza la llista de jugadors (el servidor la envia en ordre d'arribada)
    public void receiveMessage(JSONObject msgObj) {
        JSONArray players = msgObj.getJSONArray("players");

        // Esborrem la llista i la tornem a omplir amb "nom   punts"
        listRanking.getItems().clear();
        for (int i = 0; i < players.length(); i++) {
            JSONObject player = players.getJSONObject(i);
            listRanking.getItems().add(player.getString("name") + "   " + player.getInt("points"));
        }
    }

    // Acció del botó "Tornar a jugar" (viewRanking.fxml, onAction="#playAgain"):
    // tornem a la vista del joc. El servidor ja ha preparat la partida nova.
    @FXML
    private void playAgain() {
        UtilsViews.setView("ViewGame");
    }
}