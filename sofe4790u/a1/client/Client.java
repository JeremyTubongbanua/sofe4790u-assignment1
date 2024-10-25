package sofe4790u.a1.client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final int MAX_BUFFER_SIZE = 4096;
    private static Socket socket;
    private static Socket fileSocket;

    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: java a1.Client <host> <port> <filePort> <client_name>");
            return;
        }

        String serverAddress, clientName;
        int port, filePort;

        try {
            serverAddress = args[0];
            port = Integer.parseInt(args[1]);
            filePort = Integer.parseInt(args[2]);
            clientName = args[3];
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number. Please provide valid integers.");
            return;
        }

        try {
            socket = new Socket(serverAddress, port);
            fileSocket = new Socket(serverAddress, filePort);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            DataOutputStream fileOut = new DataOutputStream(fileSocket.getOutputStream());

            out.println(clientName);

            Scanner scanner = new Scanner(System.in);

            new Thread(() -> {
                try {
                    String message;
                    while (socket.isConnected() && (message = in.readLine()) != null) {
                        if (message.startsWith("FILE_TRANSFER")) {
                            String[] parts = message.split(" ");
                            String fileName = parts[1];
                            long fileSize = Long.parseLong(parts[2]);
                            receiveFile(fileName, fileSize);
                        } else {
                            System.out.println(message);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            while (true) {
                String input = scanner.nextLine();
                if (input.startsWith("/upload")) {
                    String[] parts = input.split(" ", 2);
                    if (parts.length < 2) {
                        System.out.println("Usage: /upload <file_path>");
                        continue;
                    }
                    String filePath = parts[1];
                    File file = new File(filePath);
                    if (file.exists()) {
                        out.println("/upload");
                        sendFile(file, fileOut);
                    } else {
                        System.out.println("File not found: " + filePath);
                    }
                } else {
                    out.println(input);
                }
            }
        } catch (IOException e) {
            System.out.println("Could not connect to the server. Please ensure the server is running and try again.");
            e.printStackTrace();
        }
    }

    private static void receiveFile(String fileName, long fileSize) {
        try {
            File file = new File(fileName);
            if (file.exists()) {
                file.delete();
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                DataInputStream dataIn = new DataInputStream(fileSocket.getInputStream());
                byte[] buffer = new byte[MAX_BUFFER_SIZE];
                int bytesRead;
                long totalBytesRead = 0;

                while (totalBytesRead < fileSize && (bytesRead = dataIn.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }
            }
            System.out.println("File received: " + fileName);

        } catch (IOException e) {
            System.out.println("Error during file transfer: " + e.getMessage());
        }
    }

    private static void reconnectFileSocket() {
        try {
            fileSocket = new Socket(socket.getInetAddress(), fileSocket.getPort());
        } catch (IOException e) {
            System.out.println("Could not reconnect file socket: " + e.getMessage());
        }
    }

    private static void sendFile(File file, DataOutputStream fileOut) {
        try {
            fileOut.writeUTF(file.getName());
            fileOut.writeLong(file.length());

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[MAX_BUFFER_SIZE];
                int bytesRead;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                }
            }
            fileOut.flush(); // Ensure all data is flushed
            System.out.println("File sent: " + file.getName());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}