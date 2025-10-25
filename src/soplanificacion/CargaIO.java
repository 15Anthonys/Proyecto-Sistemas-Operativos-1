/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package soplanificacion;

/**
 *
 * @author luisg
 */
import com.google.gson.Gson; // Import de la librería
import java.io.*;

public class CargaIO {

    private static final String FILE_NAME = "carga.json";
    private static final Gson gson = new Gson(); // Instancia de Gson

    /**
     * Guarda el objeto de configuración en el archivo carga.json
     */
    public static void save(CargaProcesoConfig config) {
        try (FileWriter writer = new FileWriter(FILE_NAME)) {
            gson.toJson(config, writer); // Magia: Gson convierte el objeto a JSON
            System.out.println("Configuración de carga guardada en " + FILE_NAME);
        } catch (IOException e) {
            System.err.println("Error al guardar la configuración de carga: " + e.getMessage());
        }
    }

    /**
     * Carga el objeto de configuración desde carga.json
     * Si no existe, devuelve uno con valores por defecto.
     */
    public static CargaProcesoConfig load() {
        File f = new File(FILE_NAME);
        if (!f.exists()) {
            System.out.println("No se encontró " + FILE_NAME + ", usando valores por defecto.");
            return new CargaProcesoConfig(); // Devuelve config por defecto
        }

        try (FileReader reader = new FileReader(f)) {
            // Magia: Gson convierte el JSON a un objeto CargaProcesoConfig
            CargaProcesoConfig config = gson.fromJson(reader, CargaProcesoConfig.class);
            if (config == null) {
                return new CargaProcesoConfig();
            }
            System.out.println("Configuración de carga leída desde " + FILE_NAME);
            return config;
        } catch (Exception e) {
            System.err.println("Error al leer la configuración de carga: " + e.getMessage());
            return new CargaProcesoConfig(); // Devuelve por defecto en caso de error
        }
    }
}