/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package soplanificacion;

/**
 *
 * @author dugla
 */
public class Nodo {
    
    private Nodo pnext;
    private String nombre;

    public Nodo(String nombre) {
        this.pnext = null;
        this.nombre = nombre;
    }
    
    

    /**
     * @return the pnext
     */
    public Nodo getPnext() {
        return pnext;
    }

    /**
     * @param pnext the pnext to set
     */
    public void setPnext(Nodo pnext) {
        this.pnext = pnext;
    }

    /**
     * @return the nodo
     */
    public String getNodo() {
        return nombre;
    }

    /**
     * @param nodo the nodo to set
     */
    public void setNodo(String nodo) {
        this.nombre = nombre;
    }
    
    
    
    
}
