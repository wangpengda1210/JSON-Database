package client;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elements.RequestType;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class Main {

    @Parameter(names = {"-t"})
    String typeOfRequest;

    @Parameter(names = {"-k"})
    String key;

    @Parameter(names = {"-v"})
    String value;

    @Parameter(names = {"-in"})
    String fileName;

    private static final String QUERY_PATH = System.getProperty("user.dir") + File.separator +
            "src" + File.separator +
            "client" + File.separator +
            "data" + File.separator;

    public static void main(String[] args) {

        Main main = new Main();
        JCommander.newBuilder()
                .addObject(main)
                .build()
                .parse(args);
        main.run();

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void run() {
        String address = "127.0.0.1";
        int port = 23456;

        System.out.println("Client started!");

        try (Socket socket = new Socket(InetAddress.getByName(address), port);
             DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
             DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {

            JsonObject jsonObject = new JsonObject();

            jsonObject.addProperty("type", typeOfRequest);

            if (fileName != null) {
                if (!new File(QUERY_PATH).exists()) {
                    new File(QUERY_PATH).mkdirs();
                }

                File file = new File(QUERY_PATH + fileName);
                if (file.exists()) {
                    Scanner scanner = new Scanner(file);
                    try {
                        jsonObject = JsonParser.parseString(scanner.nextLine()).getAsJsonObject();
                    } catch (Exception e) {
                        System.out.println("File not valid");
                        return;
                    }
                } else {
                    System.out.println("File not exits");
                    return;
                }
            } else {
                if (typeOfRequest == null) {
                    typeOfRequest = "null";
                }

                if (RequestType.find(typeOfRequest.toLowerCase()) != RequestType.EXIT) {
                    jsonObject.addProperty("key", key);
                }
                if (RequestType.find(typeOfRequest.toLowerCase()) == RequestType.SET) {
                    jsonObject.addProperty("value", value);
                }
            }

            dataOutputStream.writeUTF(jsonObject.toString());
            System.out.println("Sent: " + jsonObject.toString());

            System.out.println("Received: " + dataInputStream.readUTF());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
