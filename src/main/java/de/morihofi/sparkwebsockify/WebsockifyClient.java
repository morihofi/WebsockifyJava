package de.morihofi.sparkwebsockify;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class WebsockifyClient extends WebSocketClient {

    private Socket tcpSocket;

    public WebsockifyClient(URI serverUri, Socket tcpSocket) {
        super(serverUri);
        this.tcpSocket = tcpSocket;
    }

    public static void main(String[] args) throws IOException {
        String websocketUrl = "ws://localhost:8090";

        ServerSocket serverSocket = new ServerSocket(7070);

        while (true){
            try {

                Socket socket = serverSocket.accept();
                new Thread(() -> {
                    //Handle connection in seperate Thread
                    try {
                        WebsockifyClient client = new WebsockifyClient(new URI(websocketUrl), socket);
                        client.run();
                    } catch (URISyntaxException e) {
                    }

                }).start();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {

        System.out.println("Start forwarding");

        new Thread(() -> forwardDataFromTcpToWebSocket(this.getConnection(), tcpSocket)).start();

    }

    @Override
    public void onMessage(String message) {
        try {
            System.out.println("Written String as Bytes to Socket");
            tcpSocket.getOutputStream().write(message.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onMessage(ByteBuffer message) {
        try {
            System.out.println("Written " + message.limit() + " Bytes to Socket (Websocket -> TCP)");
            if (!tcpSocket.isClosed()) {
                tcpSocket.getOutputStream().write(message.array());
            } else {
                this.getConnection().close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        try {
            if (!tcpSocket.isClosed()) {
                tcpSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Disconnected");
    }

    @Override
    public void onError(Exception ex) {

    }

    private void forwardDataFromTcpToWebSocket(WebSocket wsConn, Socket tcpSocket) {


        try {


            byte[] buffer = new byte[4096];
            int bytesRead;
            InputStream is = tcpSocket.getInputStream();
            while ((bytesRead = is.read(buffer)) != -1) {

                ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, bytesRead);
                System.out.println("Read " + byteBuffer.limit() + " Bytes from Socket (TCP -> Websocket)");
                wsConn.send(byteBuffer);
            }

            tcpSocket.close();
            wsConn.close();


        } catch (IOException e) {
        }
    }


}