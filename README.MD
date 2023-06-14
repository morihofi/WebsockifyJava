# Websockify Server in Java

[![No Maintenance Intended](http://unmaintained.tech/badge.svg)](http://unmaintained.tech/)

This is a proof of concept implementation of a Websockify server in Java. The server acts as a bridge between WebSockets and TCP connections, allowing for bidirectional communication between WebSocket clients and TCP servers.

## ✨ Features

- WebSocket to TCP bridge: 
  - The server accepts WebSocket connections and forwards the received messages to a specified TCP server. 
- TCP to WebSocket bridge: 
  - The server listens for incoming TCP connections and relays the received data to the WebSocket clients.

## ✅ Software tested
- noVNC (HTML5 VNC Client, works) 

## 📜 Getting Started

To use this Websockify server, follow the steps below:

1. Clone the repository: `git clone https://github.com/morihofi/WebsockifyJava`
2. Open it in your favorite IDE

### Tunnel TCP to WebSocket
```java
public static void main(String[] args) {
        //Websocket server settings
        String wsHost = "localhost";
        int wsPort = 8090;

        WebSocketServer server = new WebsockifyServer(new InetSocketAddress(wsHost, wsPort), "192.168.178.1", 80);
        server.run();
    }
```
This tunnels traffic from `192.168.178.1:80/TCP` to a Websocket running on Port `8090` on your machine


### Tunnel WebSocket to TCP
```java
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
```
This connects to `ws://localhost:8090` and opens a TCP Socket on your machine on port `7070`