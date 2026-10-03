package com.server;

import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONObject;

// Servidor del Sudoku. Guarda el taulell, la solució i els punts dels jugadors.
// Hereta de WebSocketServer, com a la teoria, i per això ha de sobreescriure
// els mètodes onOpen, onClose, onMessage, onError i onStart.
public class Main extends WebSocketServer {

    // Port on escolta el servidor (els clients es connecten a aquest port)
    public static final int PORT = 3000;

    // Sudoku inicial: 81 xifres (9 files de 9). El 0 vol dir casella buida.
    // Està escrit en trossos de 9 xifres només perquè es llegeixi millor.
    private static final String PUZZLE =
        "530070000" + "600195000" + "098000060" +
        "800060003" + "400803001" + "700020006" +
        "060000280" + "000419005" + "000080079";

    // Solució completa del sudoku. Serveix per saber si el valor
    // que posa un jugador a una casella és correcte o no.
    private static final String SOLUTION =
        "534678912" + "672195348" + "198342567" +
        "859761423" + "426853791" + "713924856" +
        "961537284" + "287419635" + "345286179";

    // Valors actuals del taulell (81 caràcters, una casella cada un).
    // Per saber la posició d'una casella: fila * 9 + columna.
    private char[] board;

    // Estat de cada casella: '0' lliure, '1' inicial del sudoku, '2' encertada per un jugador.
    // El client fa servir això per saber quines caselles ha de bloquejar i pintar de verd.
    private char[] state;

    // Nom de cada jugador connectat. LinkedHashMap manté l'ordre en què s'han unit.
    // Només s'usa dins dels mètodes 'synchronized', per això no cal una versió concurrent.
    private LinkedHashMap<WebSocket, String> names = new LinkedHashMap<>();

    // Punts de cada jugador, segons el seu nom.
    // Es guarda a part perquè, si un jugador torna a entrar amb el mateix nom, conserva els punts.
    private ConcurrentHashMap<String, Integer> points = new ConcurrentHashMap<>();

    // Constructor: li diem a quina adreça i port ha d'escoltar i preparem la primera partida
    public Main(InetSocketAddress address) {
        super(address);
        newGame();
    }

    // Prepara una partida nova: taulell inicial i caselles inicials bloquejades
    private void newGame() {
        board = PUZZLE.toCharArray();
        state = new char[81];
        for (int i = 0; i < 81; i++) {
            if (board[i] == '0') {
                // Casella buida: el jugador la pot omplir
                state[i] = '0';
            } else {
                // Casella que ja ve donada: queda bloquejada
                state[i] = '1';
            }
        }
    }

    // Crea el missatge JSON amb el taulell i la llista de jugadors (en ordre d'arribada).
    // 'type' pot ser "state" (partida en marxa) o "end" (sudoku acabat).
    // És el missatge que reben els clients cada vegada que alguna cosa canvia.
    private JSONObject buildState(String type) {
        JSONObject obj = new JSONObject();
        obj.put("type", type);
        obj.put("board", new String(board));
        obj.put("state", new String(state));

        // Els jugadors surten en l'ordre en què s'han anat unint
        JSONArray players = new JSONArray();
        for (String name : names.values()) {
            JSONObject player = new JSONObject();
            player.put("name", name);
            player.put("points", points.get(name));
            players.put(player);
        }
        obj.put("players", players);

        return obj;
    }

    // Envia un missatge a tots els clients connectats.
    // getConnections() és un mètode de WebSocketServer que retorna tots els sockets oberts.
    private void sendToAll(JSONObject obj) {
        for (WebSocket client : getConnections()) {
            client.send(obj.toString());
        }
    }

    // Es crida quan un client es connecta: li enviem l'estat actual.
    // Així el client rep un primer missatge i sap que la connexió funciona.
    // 'synchronized' fa que només un fil a la vegada executi aquests mètodes,
    // així dos jugadors no poden modificar el taulell al mateix temps.
    @Override
    public synchronized void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Client connectat");
        conn.send(buildState("state").toString());
    }

    // Es crida quan un client tanca la connexió: l'esborrem de la llista i avisem la resta
    @Override
    public synchronized void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String name = names.remove(conn);
        System.out.println("Client desconnectat: " + name);
        sendToAll(buildState("state"));
    }

    // Es crida quan arriba un missatge d'un client. N'hi ha de dos tipus:
    // "join" (un jugador diu el seu nom) i "move" (un jugador posa un valor)
    @Override
    public synchronized void onMessage(WebSocket conn, String message) {
        // Convertim el text rebut en un objecte JSON i mirem de quin tipus és
        JSONObject obj = new JSONObject(message);
        String type = obj.getString("type");

        if (type.equals("join")) {
            // Un jugador nou diu el seu nom: el guardem i, si és nou, li donem 0 punts
            String name = obj.getString("name");
            names.put(conn, name);
            points.putIfAbsent(name, 0);

            // Avisem tothom perquè vegin el nou jugador a la llista
            sendToAll(buildState("state"));

        } else if (type.equals("move")) {
            // Un jugador posa un valor en una casella.
            // Primer mirem qui és; si no ha enviat el nom, ignorem el missatge.
            String name = names.get(conn);
            if (name == null) {
                return;
            }

            // Posició de la casella a la cadena de 81 caràcters, i valor posat pel jugador
            int index = obj.getInt("row") * 9 + obj.getInt("col");
            char value = (char) ('0' + obj.getInt("value"));

            // Si la casella ja està bloquejada, no fem res
            if (state[index] != '0') {
                return;
            }

            // Comparem el valor amb el de la solució
            if (value == SOLUTION.charAt(index)) {
                // Encert: la casella es bloqueja i el jugador suma 2 punts
                board[index] = value;
                state[index] = '2';
                points.put(name, points.get(name) + 2);
            } else {
                // Error: el jugador resta 1 punt
                points.put(name, points.get(name) - 1);
            }

            // Mirem si queda alguna casella lliure
            boolean acabat = true;
            for (int i = 0; i < 81; i++) {
                if (state[i] == '0') {
                    acabat = false;
                }
            }

            if (acabat) {
                // Avisem que s'ha acabat, i preparem una partida nova
                sendToAll(buildState("end"));
                newGame();
                sendToAll(buildState("state"));
            } else {
                // La partida continua: enviem el taulell actualitzat a tothom
                sendToAll(buildState("state"));
            }
        }
    }

    // Es crida quan hi ha un error: el mostrem per consola
    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    // Es crida quan el servidor ja ha arrencat
    @Override
    public void onStart() {
        System.out.println("Servidor engegat al port " + getPort());
    }

    // Punt d'entrada del servidor: el crea i l'arrenca
    public static void main(String[] args) {
        Main server = new Main(new InetSocketAddress(PORT));
        server.start();
    }
}