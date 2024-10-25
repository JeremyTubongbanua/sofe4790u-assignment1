package sofe4790u.a1.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

public class Server {

    private static final int MAX_BUFFER_SIZE = 4096;
    private static int port;
    private static int filePort;
    private static final Set<ClientThread> clientHandlers = Collections.synchronizedSet(new HashSet<>());
    private static final Logger logger = Logger.getLogger(Server.class.getName());

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java a1.Server <port> <filePort>");
            return;
        }

        try {
            port = Integer.parseInt(args[0]);
            filePort = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number. Please provide valid integers.");
            return;
        }

        try (ServerSocket serverSocket = new ServerSocket(port);
             ServerSocket fileServerSocket = new ServerSocket(filePort)) {
            System.out.println("Server started on ports " + port + " and " + filePort);
            logger.info("Server listening on port: " + port + " and file port: " + filePort);

            while (true) {
                Socket socket = serverSocket.accept();
                Socket fileSocket = fileServerSocket.accept();
                logger.info("New client connected from " + socket.getInetAddress() + " on port " + socket.getPort());

                ClientThread clientHandler = new ClientThread(socket, fileSocket);
                clientHandlers.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            logger.severe("Server encountered an error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static void broadcastMessage(String message, ClientThread sender) {
        synchronized (clientHandlers) {
            for (ClientThread client : new HashSet<>(clientHandlers)) {
                if (client != sender && client.isConnected()) {
                    client.sendMessage(message);
                }
            }
        }
    }

    static void removeClient(ClientThread clientHandler) {
        clientHandlers.remove(clientHandler);
        logger.info("Client disconnected: " + clientHandler.clientName);
    }

    static class ClientThread implements Runnable {
        private Socket socket;
        private Socket fileSocket;
        private BufferedReader in;
        private PrintWriter out;
        private DataInputStream fileIn;
        private DataOutputStream fileOut;
        private String clientName;
        private boolean connected;

        public ClientThread(Socket socket, Socket fileSocket) {
            this.socket = socket;
            this.fileSocket = fileSocket;
            this.connected = true;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                fileIn = new DataInputStream(fileSocket.getInputStream());
                fileOut = new DataOutputStream(fileSocket.getOutputStream());

                clientName = in.readLine();
                logger.info(clientName + " has joined the chat.");

                String message;
                while (connected && (message = in.readLine()) != null) {
                    if (message.startsWith("/upload")) {
                        receiveFile();
                    } else {
                        Server.broadcastMessage(clientName + ": " + message, this);
                    }
                }
            } catch (IOException e) {
                logger.warning("Client " + clientName + " encountered an error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }

        private void receiveFile() {
            try {
                String fileName = fileIn.readUTF();
                long fileSize = fileIn.readLong();
                File file = new File(fileName);

                if (file.exists()) {
                    file.delete();
                }

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    byte[] buffer = new byte[MAX_BUFFER_SIZE];
                    int bytesRead;
                    long totalBytesRead = 0;

                    logger.info(clientName + " started file upload: " + fileName + " (" + fileSize + " bytes)");
                    while (totalBytesRead < fileSize && (bytesRead = fileIn.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                    }
                    logger.info(clientName + " completed file upload: " + fileName);

                } catch (IOException e) {
                    logger.warning("Error while saving file from " + clientName + ": " + e.getMessage());
                }

                broadcastFile(file);

            } catch (IOException e) {
                logger.warning("File transfer error with " + clientName + ": " + e.getMessage());
                disconnect();
            }
        }

        private void broadcastFile(File file) {
            synchronized (clientHandlers) {
                for (ClientThread client : new HashSet<>(clientHandlers)) {
                    if (client != this && client.isConnected()) {
                        client.sendMessage("FILE_TRANSFER " + file.getName() + " " + file.length());
                        client.sendFile(file);
                    }
                }
            }
            logger.info("File broadcasted to all clients: " + file.getName());
        }

        void sendMessage(String message) {
            if (connected && !socket.isClosed()) {
                out.println(message);
            }
        }

        void sendFile(File file) {
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[MAX_BUFFER_SIZE];
                int bytesRead;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                }
                fileOut.flush();
                logger.info("File sent to client: " + clientName + " (" + file.getName() + ")");

            } catch (IOException e) {
                logger.warning("Error sending file to " + clientName + ": " + e.getMessage());
                disconnect();
            }
        }

        boolean isConnected() {
            return connected && !socket.isClosed();
        }

        private void disconnect() {
            connected = false;
            Server.removeClient(this);
            try {
                if (!socket.isClosed()) {
                    socket.close();
                    logger.info("Socket closed for client: " + clientName);
                }
                if (!fileSocket.isClosed()) {
                    fileSocket.close();
                    logger.info("File socket closed for client: " + clientName);
                }
            } catch (IOException e) {
                logger.warning("Error closing sockets for " + clientName + ": " + e.getMessage());
            }
        }
    }
}
