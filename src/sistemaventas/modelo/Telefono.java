package sistemaventas.modelo;

/**
 * Modelo para representar la entidad Teléfono asociada a un Cliente.
 * Cumple con el requerimiento de múltiples teléfonos por cliente.
 */
public class Telefono {

    private int idTelefono;
    private int idCliente;
    private String numero;
    private String tipo; // Personal, Trabajo, Casa, Otro

    // Constructor vacío
    public Telefono() {
    }

    // Constructor para nuevos registros (sin idTelefono)
    public Telefono(String numero, String tipo) {
        this.numero = numero;
        this.tipo = tipo;
    }

    // Constructor completo
    public Telefono(int idTelefono, int idCliente, String numero, String tipo) {
        this.idTelefono = idTelefono;
        this.idCliente = idCliente;
        this.numero = numero;
        this.tipo = tipo;
    }

    // Getters y Setters
    public int getIdTelefono() {
        return idTelefono;
    }

    public void setIdTelefono(int idTelefono) {
        this.idTelefono = idTelefono;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    @Override
    public String toString() {
        return tipo + ": " + numero;
    }
}