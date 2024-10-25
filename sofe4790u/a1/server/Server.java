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
    private static final Logger logger = Logger.getLogger(Server.class.getName()); // a logger that can be used by all
                                                                                   // threads

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java a1.Server <port> <filePort>");
            return;
        }

        /*
         * 1. Parse arguments
         */
        try {
            port = Integer.parseInt(args[0]);
            filePort = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number. Please provide valid integers.");
            return;
        }

        /*
         * 2. init server socket
         */
        try (ServerSocket serverSocket = new ServerSocket(port);
                ServerSocket fileServerSocket = new ServerSocket(filePort)) {
            System.out.println("Server started on ports " + port + " and " + filePort);
            logger.info("Server listening on port: " + port + " and file port: " + filePort);

            /*
             * 3. constantly accept new clients
             */
            while (true) {
                Socket socket = serverSocket.accept();
                Socket fileSocket = fileServerSocket.accept();
                logger.info("New client connected from " + socket.getInetAddress() + " on port " + socket.getPort());

                /*
                 * 4. once a cleint accepts, create a new thread to handle the client
                 */
                ClientThread clientHandler = new ClientThread(socket, fileSocket);
                clientHandlers.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            logger.severe("Server encountered an error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Broadcasts a message to all connected clients except the sender.
     * 
     * @param message the message to broadcast
     * @param sender  the client that sent the message
     */
    static void broadcastMessage(String message, ClientThread sender) {
        synchronized (clientHandlers) {
            for (ClientThread client : new HashSet<>(clientHandlers)) {
                if (client != sender && client.isConnected()) {
                    client.sendMessage(message);
                }
            }
        }
    }

    /**
     * Broadcasts a file to all connected clients except the sender.
     * 
     * @param file the file to broadcast
     * @param sender the client that sent the file
     */
    static void broadcastFile(File file, ClientThread sender) {
        synchronized (clientHandlers) {
            for (ClientThread client : new HashSet<>(clientHandlers)) {
                if (client != sender && client.isConnected()) {
                    client.sendMessage("FILE_TRANSFER " + file.getName() + " " + file.length());
                    client.sendFile(file);
                }
            }
        }
        logger.info("File broadcasted to all clients: " + file.getName());
    }

    /**
     * Removes a client from the list of connected clients.
     * 
     * @param clientHandler the client to remove
     */
    static void removeClient(ClientThread clientHandler) {
        clientHandlers.remove(clientHandler);
        logger.info("Client disconnected: " + clientHandler.clientName);
    }

    /**
     * A thread that handles communication with a single client.
     * The server can handle multiple clients concurrently.
     */
    static class ClientThread implements Runnable {
        private Socket socket; // socket for text communication
        private Socket fileSocket; // socket for file transfer
        private BufferedReader in; // input stream for text communication
        private PrintWriter out; // output stream for text communication
        private DataInputStream fileIn; // input stream for file transfer
        private DataOutputStream fileOut; // output stream for file transfer
        private String clientName; // the name of the client
        private boolean connected; // whether the client is connected

        public ClientThread(Socket socket, Socket fileSocket) {
            this.socket = socket;
            this.fileSocket = fileSocket;
            this.connected = true;
        }

        /**
         * Main thread method that listens for incoming messages from the client and also handles file transfers and message broadcasting.
         */
        @Override
        public void run() {
            try {
                /*
                 * 1. init input and output streams
                 */
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                fileIn = new DataInputStream(fileSocket.getInputStream());
                fileOut = new DataOutputStream(fileSocket.getOutputStream());

                /*
                 * 2. read client name
                 */
                clientName = in.readLine();
                logger.info(clientName + " has joined the chat.");

                String message;
                while (connected && (message = in.readLine()) != null) {
                    /*
                     * if the client sends a message starting with "/upload", it means they want to send a file
                     * otherwise, broadcast the message to all clients
                     */
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

        /**
         * This method receives a file from the client connected to this thread.
         */
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

                Server.broadcastFile(file, this);

            } catch (IOException e) {
                logger.warning("File transfer error with " + clientName + ": " + e.getMessage());
                disconnect();
            }
        }

        /**
         * This method is for sending a message to the client connected to this thread.
         * @param message the message to send
         */
        void sendMessage(String message) {
            if (connected && !socket.isClosed()) {
                out.println(message);
            }
        }

        /**
         * This method is for sending a file to the client connected to this thread.
         * @param file the file to send
         */
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

        /**
         * This method checks if the client is still connected.
         * @return true if the client is connected, false otherwise
         * 
         */
        boolean isConnected() {
            return connected && !socket.isClosed();
        }

        /**
         * This method disconnects the client from the server.
         * It closes the sockets and removes the client from the list of connected clients.
         */
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
