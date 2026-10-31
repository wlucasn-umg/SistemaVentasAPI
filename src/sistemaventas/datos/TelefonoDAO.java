package sistemaventas.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import sistemaventas.modelo.Telefono;

public class TelefonoDAO {

    public List<Telefono> listarPorCliente(int idCliente) throws SQLException {
        List<Telefono> lista = new ArrayList<>();
        String sql = "SELECT id_telefono, id_cliente, numero, tipo FROM telefono WHERE id_cliente = ?";
        
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Telefono(
                            rs.getInt("id_telefono"),
                            rs.getInt("id_cliente"),
                            rs.getString("numero"),
                            rs.getString("tipo")));
                }
            }
        }
        return lista;
    }

    public void insertar(Telefono t) throws SQLException {
        String sql = "INSERT INTO telefono (id_cliente, numero, tipo) VALUES (?, ?, ?)";
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, t.getIdCliente());
            ps.setString(2, t.getNumero());
            ps.setString(3, t.getTipo());
            ps.executeUpdate();
        }
    }

    public void eliminarPorCliente(int idCliente) throws SQLException {
        String sql = "DELETE FROM telefono WHERE id_cliente = ?";
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            ps.executeUpdate();
        }
    }
}