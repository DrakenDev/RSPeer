package com.nex.script;

import com.google.gson.GsonBuilder;
import org.rspeer.script.Script;
import org.rspeer.ui.Log;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class Settings {

    static String settFile;
    static String email;

    public static void initialize(String email){
        settFile = Script.getDataDirectory().toString() + "/" + email.replace("@", "_").replace(".", "_") + ".json";
        Settings.email = email;
        ReLoadSettings();
    }

    static HashMap<String, Object> settings;
    public static String getString(String name, String defaultValue){
        if(settings == null) ReLoadSettings();
        if(settings != null) return (String)settings.getOrDefault(name, defaultValue);
        return defaultValue;
    }
    public static  Integer getInt(String name, Integer defaultValue){
        if(settings == null) ReLoadSettings();
        if(settings != null) return (Integer)settings.getOrDefault(name, defaultValue);
        return defaultValue;
    }
    public static  Double getDouble(String name, Double defaultValue){
        if(settings == null) ReLoadSettings();
        if(settings != null) return (Double)settings.getOrDefault(name, defaultValue);
        return defaultValue;
    }
    public static  Object getObject(String name, Object defaultValue){
        if(settings == null) ReLoadSettings();
        if(settings != null) return settings.getOrDefault(name, defaultValue);
        return defaultValue;
    }
    public static  Object setValue(String name, Object value){
        if(settings == null) ReLoadSettings();
        if(settings != null) return settings.put(name, value);
        return value;
    }

    public static void SaveSettings(){
        if(settings == null) return;
        try {
            save(Paths.get(settFile), settings);
        }catch (Exception e) {
            Log.severe(e);
        }
    }
    public static void ReLoadSettings(){
        if(email == null)
            return;
        try {
            settings = new HashMap<>();
            settings = load(Paths.get(settFile), settings.getClass());
        }catch (Exception e) {
            Log.severe(e);
        }
    }
    public static void DeleteSettings(){
        try {
            if (email != null)
                Files.delete(new File(settFile).toPath());
        }catch (IOException ex){ Log.severe(ex); }
    }

    public static <T> T load(Path path, Class<? extends T> type) throws IOException, IllegalAccessException, InstantiationException {
        if (Files.exists(path)) {
            return new GsonBuilder().create().fromJson(
                    new FileReader(path.toString()), type
            );
        }
        return type.newInstance();
    }

    public static <T> void save(Path path, T instance) throws IOException {
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
        Files.write(path, new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create().toJson(instance).getBytes()
        );
    }
}
