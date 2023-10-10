package server;

import org.json.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Server {
    private static String serverAddress;
    private static int serverPort;
    private static String serverDirectory;

    public static void main(String[] args) throws IOException {

        // load config.json
        loadConfig();

        // wait for connection
        try {
            ServerSocket serverSocket = new ServerSocket(serverPort);
            System.out.println("Server is listening on " + serverAddress + ":" + serverPort);

            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("accept address from: " + client.getInetAddress() + ":" + client.getPort());
                handleRequest(client);
            }
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }

    private static void loadConfig() {
        try {
            BufferedReader configReader = new BufferedReader(new FileReader("config.json"));
            StringBuilder configJson = new StringBuilder();
            String line;
            while ((line = configReader.readLine()) != null) {
                configJson.append(line);
            }
            configReader.close();

            JSONObject config = new JSONObject(configJson.toString());
            serverAddress = config.getString("serverAddress");
            serverPort = config.getInt("serverPort");
            serverDirectory = config.getString("serverDirectory");
        } catch (IOException | JSONException ex) {
            System.err.println("Error loading config.json: " + ex);
            System.exit(1);
        }
    }

    private static void handleRequest(Socket connectToClient) {
        try {
            BufferedReader requestReader = new BufferedReader(new InputStreamReader(connectToClient.getInputStream()));
            PrintWriter responseWriter = new PrintWriter(connectToClient.getOutputStream(), true);

            String requestLine = requestReader.readLine();
            if (requestLine != null && requestLine.startsWith("GET")) {
                System.out.println("request: " + requestLine);
                String[] requestParts = requestLine.split(" ");
                String requestedPath = requestParts[1];
                String filePath = serverDirectory + requestedPath;
                // response to client
                responseToClient(connectToClient, responseWriter, filePath);
            } else {
                System.out.println("unknown request: " + requestLine);
            }
            // close.
            responseWriter.close();
            requestReader.close();
            connectToClient.close();
            System.out.println("process over.");

        } catch (IOException ex) {
            System.err.println(ex);
        }
    }

    private static void responseToClient(Socket connectToClient, PrintWriter responseWriter, String fileName) throws IOException {
        File file = new File(fileName);

        if (file.exists() && file.isFile()) {
            System.out.println("file " + fileName + " found");
            // set Content-Type
            String contentType = getContentType(fileName);
            // send head in chars
            responseWriter.println("HTTP/1.1 200 OK");
            responseWriter.println("Content-Type: " + contentType);
            responseWriter.println("Content-Length: " + file.length());
            responseWriter.println();

            // Send the file content in bytes
            try (BufferedInputStream fileStream = new BufferedInputStream(new FileInputStream(file))) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fileStream.read(buffer)) != -1) {
                    connectToClient.getOutputStream().write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // File not found, send a 404 response
            System.out.println("file " + fileName + " not found");
            responseWriter.println("HTTP/1.1 404 Not Found");
            responseWriter.println();
        }
    }

    private static String getContentType(String fileName) {
        if (fileName.endsWith(".html")) {
            return "text/html";
        } else if (fileName.endsWith(".jpg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".ico")) {
            return "image/x-icon";
        } else if (fileName.endsWith(".mp4")) {
            return "video/mp4";
        } else if (fileName.endsWith(".mkv")) {
            return "video/x-matroska"; // MIME类型为MKV
        } else {
            // 默认返回二进制流类型
            return "application/octet-stream";
        }
    }

}
