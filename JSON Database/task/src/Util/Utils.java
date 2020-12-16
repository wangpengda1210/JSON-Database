package Util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.util.Scanner;

public class Utils {

    public static JsonObject readDatabase(String path) throws FileNotFoundException {
        File dbFile = new File(path);
        Scanner scanner = new Scanner(dbFile);
        JsonObject result = JsonParser.parseString(scanner.nextLine()).getAsJsonObject();
        scanner.close();
        return result;
    }

    public static void writeDatabase(JsonObject jsonObject, String path) throws FileNotFoundException {
        File dbFile = new File(path);
        PrintWriter printWriter = new PrintWriter(dbFile);
        printWriter.print(jsonObject.toString());
        printWriter.flush();
        printWriter.close();
    }

}
