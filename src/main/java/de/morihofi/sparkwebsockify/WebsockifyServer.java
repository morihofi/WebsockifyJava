package de.morihofi.sparkwebsockify;


import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class WebsockifyServer extends WebSocketServer {

    public WebsockifyServer(InetSocketAddress address) {
        super(address);
    }

    Socket vncSocket;
    WebSocket wsConn;

    OutputStream os;
    InputStream is;

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        //conn.send("Welcome to the server!"); //This method sends a message to the new client
        //broadcast("new connection: " + handshake.getResourceDescriptor()); //This method sends a message to all clients connected
        System.out.println("new connection to " + conn.getRemoteSocketAddress());

        // TCP-Verbindung zum VNC-Server herstellen
        try {
            vncSocket = new Socket("127.0.0.1", 5980);
            os = vncSocket.getOutputStream();
            is = vncSocket.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // WebSocket-Session und TCP-Socket miteinander verbinden
        wsConn = conn;

        // Starte den Thread zum Lesen der Daten vom VNC-Server und Senden an den WebSocket-Client
        new Thread(this::forwardDataFromVncToWebSocket).start();

        // Starte den Thread zum Lesen der Daten vom WebSocket-Client und Senden an den VNC-Server
        //new Thread(this::forwardDataFromWebSocketToVnc).start();

    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("closed " + conn.getRemoteSocketAddress() + " with exit code " + code + " additional info: " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("received message from " + conn.getRemoteSocketAddress() + ": " + message);

        try {
            os.write(message.getBytes(StandardCharsets.UTF_8));
            os.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        System.out.println("received ByteBuffer from " + conn.getRemoteSocketAddress());

        try {
            byte[] data = new byte[message.remaining()];
            message.get(data);
            os.write(data);
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("an error occurred on connection " + conn.getRemoteSocketAddress() + ":" + ex);
    }

    @Override
    public void onStart() {
        System.out.println("server started successfully");
    }

    private void forwardDataFromVncToWebSocket() {
        try {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                byte[] data = new byte[bytesRead];
                System.arraycopy(buffer, 0, data, 0, bytesRead);
                wsConn.send(data);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

  // private void forwardDataFromWebSocketToVnc() {
  //     try {
  //         byte[] buffer = new byte[1024];
  //         int bytesRead;

  //         while ((bytesRead = websocketSession.getRemote().getInputStream().read(buffer)) != -1) {
  //             vncSocket.getOutputStream().write(buffer, 0, bytesRead);
  //         }
  //     } catch (IOException e) {
  //         e.printStackTrace();
  //     }
  // }


    public static void main(String[] args) {
        String wsHost = "localhost";
        int wsPort = 8090;

        WebSocketServer server = new WebsockifyServer(new InetSocketAddress(wsHost, wsPort));
        server.run();
    }
}