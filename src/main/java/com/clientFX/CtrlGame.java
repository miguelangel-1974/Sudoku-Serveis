package com.clientFX;

import java.net.URL;
import java.util.ResourceBundle;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

// Controlador de la vista 2 (viewGame.fxml): el taulell del sudoku i la llista de jugadors
public class CtrlGame implements Initializable {

    // Etiqueta de dalt amb el nom del jugador ("Juga: Albert")
    @FXML
    private Label txtName;

    // Graella buida on posarem les 81 caselles
    @FXML
    private GridPane gridBoard;

    // Llista de jugadors amb els seus punts, a la dreta
    @FXML
    private ListView<String> listPlayers;

    // Estil de les caselles: cantonades quadrades (per defecte són arrodonides)
    private static final String STYLE = "-fx-background-radius: 0, 0, 0, 0;";

    // Estil de les caselles encertades: el mateix, amb el fons verd
    private static final String STYLE_GREEN = STYLE + " -fx-control-inner-background: lightgreen;";

    // Les 81 caselles del taulell, guardades en una taula de 9 files per 9 columnes
    // per poder-hi accedir amb cells[fila][columna]
    private TextField[][] cells = new TextField[9][9];

    // JavaFX el crida quan la vista s'ha carregat: aquí creem les caselles del taulell
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // Creem les 81 caselles i les afegim a la graella
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {

                // Cada casella és un TextField quadrat de 40x40
                TextField field = new TextField();
                field.setPrefSize(40, 40);
                field.setStyle(STYLE);

                // Quan es prem Enter a la casella, enviem el valor al servidor.
                // Fem còpies 'final' de fila i columna perquè la funció de dins
                // no pot fer servir les variables del bucle directament.
                final int r = row;
                final int c = col;
                field.setOnAction((event) -> {
                    sendMove(r, c);
                });

                // Posem la casella a la graella (primer la columna i després la fila)
                // i la guardem a la taula per poder-la modificar més endavant
                gridBoard.add(field, col, row);
                cells[row][col] = field;
            }
        }
    }

    // Mostra el nom del jugador a la part de dalt (el crida Main en connectar-se)
    public void setName(String name) {
        txtName.setText("Juga: " + name);
    }

    // Envia al servidor el valor escrit a la casella (fila, columna)
    private void sendMove(int row, int col) {
        String txt = cells[row][col].getText();

        // Només acceptem una xifra de l'1 al 9
        if (txt.length() == 1 && "123456789".contains(txt)) {
            // Preparem el missatge JSON amb la jugada i l'enviem al servidor
            JSONObject obj = new JSONObject();
            obj.put("type", "move");
            obj.put("row", row);
            obj.put("col", col);
            obj.put("value", Integer.parseInt(txt));
            Main.wsClient.safeSend(obj.toString());
        }

        // Buidem la casella: si l'encert és correcte, el servidor ens
        // enviarà el valor i es posarà en verd
        cells[row][col].setText("");
    }

    // Es crida quan arriba un missatge del servidor (el crida Main):
    // actualitza el taulell i la llista de jugadors
    public void receiveMessage(JSONObject msgObj) {
        // 'board' té els valors de les 81 caselles i 'state' l'estat de cada una
        String board = msgObj.getString("board");
        String state = msgObj.getString("state");

        // Recorrem les 81 caselles. De la posició i traiem la fila (i / 9) i la columna (i % 9)
        for (int i = 0; i < 81; i++) {
            TextField field = cells[i / 9][i % 9];
            char s = state.charAt(i);

            if (s == '1') {
                // Casella inicial del sudoku: es mostra i queda bloquejada
                field.setText(String.valueOf(board.charAt(i)));
                field.setEditable(false);
            } else if (s == '2') {
                // Casella encertada: es mostra, queda bloquejada i es posa en verd
                field.setText(String.valueOf(board.charAt(i)));
                field.setEditable(false);
                field.setStyle(STYLE_GREEN);
            } else if (!field.isEditable()) {
                // Casella lliure que abans estava bloquejada (partida nova): la netegem
                field.setText("");
                field.setEditable(true);
                field.setStyle(STYLE);
            }
        }

        // Llista de jugadors amb els seus punts: l'esborrem i la tornem a omplir
        JSONArray players = msgObj.getJSONArray("players");
        listPlayers.getItems().clear();
        for (int i = 0; i < players.length(); i++) {
            JSONObject player = players.getJSONObject(i);
            listPlayers.getItems().add(player.getString("name") + "   " + player.getInt("points"));
        }
    }
}