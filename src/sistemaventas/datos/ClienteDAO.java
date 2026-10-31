package sistemaventas.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import sistemaventas.modelo.Cliente;
import sistemaventas.modelo.Telefono;

public class ClienteDAO {

    private final TelefonoDAO telefonoDAO = new TelefonoDAO();
    private static final String SELECT_BASE = 
            "SELECT id_cliente, nombre, nit, direccion_envio FROM cliente ";

    public List<Cliente> listar() throws SQLException {
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(SELECT_BASE + "ORDER BY nombre");
             ResultSet rs = ps.executeQuery()) {
            return leer(rs);
        }
    }

    public List<Cliente> buscar(String texto) throws SQLException {
        String sql = SELECT_BASE + "WHERE nit LIKE ? OR nombre LIKE ? ORDER BY nombre";
        String patron = "%" + texto + "%";
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, patron);
            ps.setString(2, patron);
            try (ResultSet rs = ps.executeQuery()) {
                return leer(rs);
            }
        }
    }

    public boolean existeNit(String nit, String nitIgnorado) throws SQLException {
        String sql = "SELECT 1 FROM cliente WHERE nit = ?"
                + (nitIgnorado != null ? " AND nit <> ?" : "");
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nit);
            if (nitIgnorado != null) {
                ps.setString(2, nitIgnorado);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean tieneVentas(String nit) throws SQLException {
        return false;
    }

    public void insertar(Cliente c) throws SQLException {
        String sql = "INSERT INTO cliente (nombre, nit, direccion_envio) VALUES (?, ?, ?)";
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getNombre());
            ps.setString(2, c.getNit());
            ps.setString(3, vacioANull(c.getDireccionEnvio()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int idGenerado = rs.getInt(1);
                    c.setIdCliente(idGenerado);
                    for (Telefono t : c.getTelefonos()) {
                        t.setIdCliente(idGenerado);
                        telefonoDAO.insertar(t);
                    }
                }
            }
        }
    }

    public void actualizar(String nitOriginal, Cliente c) throws SQLException {
        String sql = "UPDATE cliente SET nombre = ?, nit = ?, direccion_envio = ? WHERE nit = ?";
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, c.getNombre());
            ps.setString(2, c.getNit());
            ps.setString(3, vacioANull(c.getDireccionEnvio()));
            ps.setString(4, nitOriginal);
            ps.executeUpdate();

            Cliente clienteBD = buscarPorNit(c.getNit());
            if (clienteBD != null) {
                telefonoDAO.eliminarPorCliente(clienteBD.getIdCliente());
                for (Telefono t : c.getTelefonos()) {
                    t.setIdCliente(clienteBD.getIdCliente());
                    telefonoDAO.insertar(t);
                }
            }
        }
    }

    public void eliminar(String nit) throws SQLException {
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM cliente WHERE nit = ?")) {
            ps.setString(1, nit);
            ps.executeUpdate();
        }
    }

    private Cliente buscarPorNit(String nit) throws SQLException {
        String sql = SELECT_BASE + "WHERE nit = ?";
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nit);
            try (ResultSet rs = ps.executeQuery()) {
                List<Cliente> list = leer(rs);
                return list.isEmpty() ? null : list.get(0);
            }
        }
    }

    private List<Cliente> leer(ResultSet rs) throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        while (rs.next()) {
            Cliente c = new Cliente();
            c.setIdCliente(rs.getInt("id_cliente"));
            c.setNombre(rs.getString("nombre"));
            c.setNit(rs.getString("nit"));
            c.setDireccionEnvio(nulo(rs.getString("direccion_envio")));

            c.setTelefonos(telefonoDAO.listarPorCliente(c.getIdCliente()));
            lista.add(c);
        }
        return lista;
    }

    private String vacioANull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    private String nulo(String s) {
        return s == null ? "" : s;
    }
}