/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package soplanificacion;

/**
 *
 * @author luisg
 */
public class CargaProcesoConfig {
    // Los nombres de las variables deben coincidir con el JSON
    private int instrucciones;
    private boolean esIoBound; // true para I/O, false para CPU
    private int ciclosExcepcion;
    private int ciclosResolver;

    // Constructor por defecto (necesario para Gson)
    public CargaProcesoConfig() {
        // Valores iniciales si el archivo no existe
        this.instrucciones = 100;
        this.esIoBound = false;
        this.ciclosExcepcion = 10;
        this.ciclosResolver = 5;
    }

    // Getters y Setters
    public int getInstrucciones() { return instrucciones; }
    public void setInstrucciones(int instrucciones) { this.instrucciones = instrucciones; }
    public boolean isEsIoBound() { return esIoBound; }
    public void setEsIoBound(boolean esIoBound) { this.esIoBound = esIoBound; }
    public int getCiclosExcepcion() { return ciclosExcepcion; }
    public void setCiclosExcepcion(int ciclosExcepcion) { this.ciclosExcepcion = ciclosExcepcion; }
    public int getCiclosResolver() { return ciclosResolver; }
    public void setCiclosResolver(int ciclosResolver) { this.ciclosResolver = ciclosResolver; }
    }
  

 