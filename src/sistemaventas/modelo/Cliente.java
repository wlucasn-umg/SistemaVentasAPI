package sistemaventas.modelo;

import java.util.ArrayList;
import java.util.List;

public class Cliente {

    private int idCliente;
    private String nombre;
    private String nit;
    private String direccionEnvio;
    private List<Telefono> telefonos;

    public Cliente() {
        this.telefonos = new ArrayList<>();
    }

    public Cliente(String nombre, String nit, String direccionEnvio) {
        this.nombre = nombre;
        this.nit = nit;
        this.direccionEnvio = direccionEnvio;
        this.telefonos = new ArrayList<>();
    }

    // CONSTRUCTOR COMPATIBLE DE 4 PARÁMETROS (Evita errores de compilación)
    public Cliente(String nombre, String telefono, String nit, String direccionEnvio) {
        this.nombre = nombre;
        this.nit = nit;
        this.direccionEnvio = direccionEnvio;
        this.telefonos = new ArrayList<>();
        if (telefono != null && !telefono.trim().isEmpty()) {
            this.telefonos.add(new Telefono(telefono.trim(), "PERSONAL"));
        }
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit;
    }

    public String getDireccionEnvio() {
        return direccionEnvio;
    }

    public void setDireccionEnvio(String direccionEnvio) {
        this.direccionEnvio = direccionEnvio;
    }

    public List<Telefono> getTelefonos() {
        return telefonos;
    }

    public void setTelefonos(List<Telefono> telefonos) {
        this.telefonos = telefonos;
    }

    public void agregarTelefono(Telefono t) {
        this.telefonos.add(t);
    }

    // MÉTODO DE COMPATIBILIDAD (Obtiene el primer número como texto)
    public String getTelefono() {
        if (telefonos != null && !telefonos.isEmpty()) {
            return telefonos.get(0).getNumero();
        }
        return "";
    }
}