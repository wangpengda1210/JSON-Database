package server;

import Util.Utils;
import com.google.gson.*;
import elements.RequestType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Main {

    static JsonObject data = new JsonObject();
    static final String DATABASE_PATH = System.getProperty("user.dir") + File.separator +
            "src" + File.separator +
            "server" + File.separator +
            "data" + File.separator + "data.json";
    static boolean exit = false;

    static ReadWriteLock lock = new ReentrantReadWriteLock();
    static Lock readLock = lock.readLock();
    static Lock writeLock = lock.writeLock();

    static ExecutorService executor = Executors.newFixedThreadPool(4);

    public static void main(String[] args) throws IOException {

        String address = "127.0.0.1";
        int port = 23456;

        writeLock.lock();
        File databaseFile = new File(DATABASE_PATH);
        if (!databaseFile.exists()) {
            Utils.writeDatabase(data, DATABASE_PATH);
        }
        writeLock.unlock();


        try (ServerSocket serverSocket = new ServerSocket(port, 50,
                InetAddress.getByName(address))) {
            System.out.println("Server started!");
            while (!serverSocket.isClosed()) {
                executor.submit(() -> {
                    try (Socket socket = serverSocket.accept();
                         DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                         DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {
                        JsonObject request = JsonParser.parseString(dataInputStream.readUTF()).getAsJsonObject();
                        JsonObject resultJson = getResult(request, serverSocket);
                        dataOutputStream.writeUTF(resultJson.toString());
                    } catch (Exception e) {
                        // e.printStackTrace();
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        executor.shutdown();

    }

    private static JsonObject getResult(JsonObject request,
                                        ServerSocket serverSocket) throws IOException {

        JsonElement requestType = request.get("type");

        if (requestType.isJsonNull() || requestType.getAsString() == null) {
            return generateErrorJson("No such request type");
        }

        RequestType theRequestType = RequestType.find(requestType.getAsString());

        if (theRequestType == null) {
            return generateErrorJson("No such request type");
        }

        JsonElement key = null;
        if (theRequestType != RequestType.EXIT) {
            key = request.get("key");
            if (key.isJsonNull()) {
                return generateErrorJson("Should give a key");
            }
        }

        switch (theRequestType) {
            case GET:
                return get(key);
            case SET:
                return set(request, key);
            case DELETE:
                return delete(key);
            case EXIT:
                exit = true;
                serverSocket.close();
                return generateReturnJson(null);
            default:
                return generateErrorJson("No such request type");
        }

    }

    private static JsonObject delete(JsonElement key) throws IOException {
        writeLock.lock();
        data = Utils.readDatabase(DATABASE_PATH);
        if (key.isJsonPrimitive()) {
            String stringKey = key.getAsString();
            if (stringKey == null || stringKey.equals("")) {
                writeLock.unlock();
                return generateErrorJson("Should give a key");
            }

            JsonElement value = data.get(stringKey);
            if (value == null || value.isJsonNull()) {
                writeLock.unlock();
                return generateErrorJson("No such key");
            } else {
                data.remove(stringKey);
                Utils.writeDatabase(data, DATABASE_PATH);
                writeLock.unlock();
                return generateReturnJson(null);
            }
        } else if (key.isJsonArray()) {
            JsonArray keyArray = key.getAsJsonArray();
            JsonObject result = data.deepCopy();
            JsonObject resultDatabase = result;
            for (JsonElement keyElement : keyArray) {
                JsonElement resultElement = result.get(keyElement.getAsString());
                if (resultElement == null || resultElement.isJsonNull()) {
                    writeLock.unlock();
                    return generateErrorJson("No such key");
                } else if (keyElement.equals(keyArray.get(keyArray.size() - 1))) {
                    result.remove(keyElement.getAsString());
                    Utils.writeDatabase(resultDatabase, DATABASE_PATH);
                    writeLock.unlock();
                    return generateReturnJson(null);
                } else if (resultElement.isJsonObject()) {
                    result = resultElement.getAsJsonObject();
                } else {
                    writeLock.unlock();
                    return generateErrorJson("No such key");
                }
            }
            writeLock.unlock();
            return generateErrorJson("No such key");
        } else {
            writeLock.unlock();
            return generateErrorJson("No such key");
        }
    }

    private static JsonObject set(JsonObject request, JsonElement key) throws IOException {
        JsonElement setValueJson = request.get("value");
        if (setValueJson == null || setValueJson.isJsonNull()) {
            return generateErrorJson("Should give a value for set");
        } else if (key.isJsonPrimitive()) {
            writeLock.lock();
            data = Utils.readDatabase(DATABASE_PATH);
            data.add(key.getAsString(), setValueJson);
            Utils.writeDatabase(data, DATABASE_PATH);
            writeLock.unlock();
            return generateReturnJson(null);
        } else if (key.isJsonArray()) {
            JsonArray setArray = key.getAsJsonArray();

            writeLock.lock();
            data = Utils.readDatabase(DATABASE_PATH);
            JsonObject baseObject = data.deepCopy();
            JsonObject setObject = baseObject;
            for (JsonElement setElement : setArray) {
                String setProperty = setElement.getAsString();
                JsonElement elementToSet = setObject.get(setProperty);
                if (!elementToSet.isJsonObject()) {
                    setObject.add(setProperty, new JsonObject());
                }
                if (!setElement.equals(setArray.get(setArray.size() - 1))) {
                    setObject = setObject.get(setProperty).getAsJsonObject();
                }
            }

            setObject.add(setArray.get(setArray.size() - 1).getAsString(), setValueJson);
            data = baseObject;
            Utils.writeDatabase(data, DATABASE_PATH);
            writeLock.unlock();
        }
        return generateReturnJson(null);
    }

    private static JsonObject get(JsonElement key) throws IOException {
        readLock.lock();
        data = Utils.readDatabase(DATABASE_PATH);
        if (key.isJsonPrimitive()) {
            String stringKey = key.getAsString();
            if (stringKey == null || stringKey.equals("")) {
                readLock.unlock();
                return generateErrorJson("Should give a key");
            }

            JsonElement value = data.get(stringKey);
            if (value == null || value.isJsonNull()) {
                readLock.unlock();
                return generateErrorJson("No such key");
            } else {
                readLock.unlock();
                return generateReturnJson(value.toString());
            }
        } else if (key.isJsonArray()) {
            JsonArray keyArray = key.getAsJsonArray();
            JsonObject result = data.deepCopy();
            for (JsonElement keyElement : keyArray) {
                JsonElement resultElement = result.get(keyElement.getAsString());
                if (resultElement == null || resultElement.isJsonNull()) {
                    readLock.unlock();
                    return generateErrorJson("No such key");
                } else if (resultElement.isJsonObject()) {
                    result = resultElement.getAsJsonObject();
                } else {
                    if (keyElement.equals(keyArray.get(keyArray.size() - 1))) {
                        readLock.unlock();
                        if (resultElement.isJsonPrimitive()) {
                            return generateReturnJson(resultElement.getAsString());
                        } else {
                            return generateReturnJson(resultElement.toString());
                        }
                    } else {
                        readLock.unlock();
                        return generateErrorJson("No such key");
                    }
                }
            }
            readLock.unlock();
            return generateReturnJson(result.toString());
        } else {
            readLock.unlock();
            return generateErrorJson("No such key");
        }
    }

    private static JsonObject generateErrorJson(String reason) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("response", "ERROR");
        jsonObject.addProperty("reason", reason);
        return jsonObject;
    }

    private static JsonObject generateReturnJson(String value) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("response", "OK");
        if (value != null) {
            try {
                jsonObject.add("value", JsonParser.parseString(value));
            } catch (JsonSyntaxException e) {
                jsonObject.addProperty("value", value);
            }
        }
        return jsonObject;
    }

}
