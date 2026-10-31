package sistemaventas.vista;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.table.DefaultTableModel;
import sistemaventas.datos.ClienteDAO;
import sistemaventas.datos.TelefonoDAO;
import sistemaventas.modelo.Cliente;
import sistemaventas.modelo.Telefono;

/**
 * Catálogo de clientes como ventana interna del MDI.
 * Soporta múltiples teléfonos por cliente (Relación 1:N).
 */
public class FrmClientes extends JInternalFrame {

    private final ClienteDAO dao = new ClienteDAO();
    private final TelefonoDAO telefonoDAO = new TelefonoDAO();
    private final List<Cliente> mostrados = new ArrayList<>();

    private final JTextField txtBuscar = new JTextField(20);
    private final JButton btnBuscar = new JButton("Buscar");
    private final JButton btnTodos = new JButton("Mostrar todos");
    private final JButton btnNuevo = new JButton("Nuevo");
    private final JButton btnEditar = new JButton("Editar");
    private final JButton btnEliminar = new JButton("Eliminar");

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"NIT", "Nombre", "Teléfonos", "Dirección de envío"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modeloTabla);

    public FrmClientes() {
        super("Clientes", true, true, true, true);
        setSize(850, 480);
        construirPantalla();
        cargarTabla("");
        addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            public void internalFrameActivated(InternalFrameEvent e) {
                cargarTabla(txtBuscar.getText().trim());
            }
        });
    }

    private void construirPantalla() {
        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        norte.setBorder(BorderFactory.createTitledBorder("Buscar por NIT o nombre"));
        norte.add(txtBuscar);
        norte.add(btnBuscar);
        norte.add(btnTodos);

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel sur = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        sur.add(btnNuevo);
        sur.add(btnEditar);
        sur.add(btnEliminar);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(norte, BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(tabla), BorderLayout.CENTER);
        getContentPane().add(sur, BorderLayout.SOUTH);

        btnBuscar.addActionListener(e -> cargarTabla(txtBuscar.getText().trim()));
        txtBuscar.addActionListener(e -> cargarTabla(txtBuscar.getText().trim()));
        btnTodos.addActionListener(e -> {
            txtBuscar.setText("");
            cargarTabla("");
        });
        btnNuevo.addActionListener(e -> nuevoCliente());
        btnEditar.addActionListener(e -> editarCliente());
        btnEliminar.addActionListener(e -> eliminarCliente());
    }

    private void cargarTabla(String filtro) {
        modeloTabla.setRowCount(0);
        mostrados.clear();
        try {
            List<Cliente> clientes = filtro.isEmpty() ? dao.listar() : dao.buscar(filtro);
            for (Cliente c : clientes) {
                mostrados.add(c);
                
                // Formatear la lista de teléfonos como texto separado por comas
                StringBuilder telStr = new StringBuilder();
                for (Telefono t : c.getTelefonos()) {
                    if (telStr.length() > 0) telStr.append(", ");
                    telStr.append(t.getTipo()).append(": ").append(t.getNumero());
                }

                modeloTabla.addRow(new Object[]{
                    c.getNit(), c.getNombre(), telStr.toString(), c.getDireccionEnvio()});
            }
        } catch (SQLException ex) {
            errorBD("No se pudieron cargar los clientes.", ex);
        }
    }

    private void nuevoCliente() {
        JTextField nit = new JTextField(15);
        JTextField nombre = new JTextField(15);
        JTextField direccion = new JTextField(15);

        DefaultListModel<Telefono> modelTelefonos = new DefaultListModel<>();
        JList<Telefono> listTelefonos = new JList<>(modelTelefonos);
        
        while (pedirDatos("Nuevo cliente", nit, nombre, direccion, modelTelefonos, listTelefonos)) {
            try {
                String error = validar(nit.getText().trim(), nombre.getText().trim(), null);
                if (error != null) {
                    aviso(error);
                    continue;
                }
                Cliente nuevo = new Cliente(nombre.getText().trim(), nit.getText().trim(), direccion.getText().trim());
                for (int i = 0; i < modelTelefonos.size(); i++) {
                    nuevo.agregarTelefono(modelTelefonos.get(i));
                }

                dao.insertar(nuevo);
                cargarTabla(txtBuscar.getText().trim());
                return;
            } catch (SQLException ex) {
                errorBD("No se pudo guardar el cliente.", ex);
                return;
            }
        }
    }

    private void editarCliente() {
        Cliente c = seleccionado();
        if (c == null) return;

        String nitOriginal = c.getNit();
        JTextField nit = new JTextField(c.getNit(), 15);
        JTextField nombre = new JTextField(c.getNombre(), 15);
        JTextField direccion = new JTextField(c.getDireccionEnvio(), 15);

        DefaultListModel<Telefono> modelTelefonos = new DefaultListModel<>();
        for (Telefono t : c.getTelefonos()) {
            modelTelefonos.addElement(t);
        }
        JList<Telefono> listTelefonos = new JList<>(modelTelefonos);

        while (pedirDatos("Editar cliente", nit, nombre, direccion, modelTelefonos, listTelefonos)) {
            try {
                String error = validar(nit.getText().trim(), nombre.getText().trim(), nitOriginal);
                if (error != null) {
                    aviso(error);
                    continue;
                }
                c.setNit(nit.getText().trim());
                c.setNombre(nombre.getText().trim());
                c.setDireccionEnvio(direccion.getText().trim());
                
                List<Telefono> listaNuevosTels = new ArrayList<>();
                for (int i = 0; i < modelTelefonos.size(); i++) {
                    listaNuevosTels.add(modelTelefonos.get(i));
                }
                c.setTelefonos(listaNuevosTels);

                dao.actualizar(nitOriginal, c);
                cargarTabla(txtBuscar.getText().trim());
                return;
            } catch (SQLException ex) {
                errorBD("No se pudo actualizar el cliente.", ex);
                return;
            }
        }
    }

    private void eliminarCliente() {
        Cliente c = seleccionado();
        if (c == null) return;

        try {
            int respuesta = JOptionPane.showConfirmDialog(this,
                    "¿Eliminar al cliente " + c.getNombre() + " (NIT " + c.getNit() + ")?",
                    "Eliminar cliente", JOptionPane.YES_NO_OPTION);
            if (respuesta == JOptionPane.YES_OPTION) {
                dao.eliminar(c.getNit());
                cargarTabla(txtBuscar.getText().trim());
            }
        } catch (SQLException ex) {
            errorBD("No se pudo eliminar el cliente.", ex);
        }
    }

    private boolean pedirDatos(String titulo, JTextField nit, JTextField nombre, JTextField direccion,
                               DefaultListModel<Telefono> modelTelefonos, JList<Telefono> listTelefonos) {
        JPanel panelPrincipal = new JPanel(new BorderLayout(8, 8));
        
        JPanel datosCliente = new JPanel(new GridLayout(3, 2, 5, 5));
        datosCliente.add(new JLabel("NIT:"));
        datosCliente.add(nit);
        datosCliente.add(new JLabel("Nombre:"));
        datosCliente.add(nombre);
        datosCliente.add(new JLabel("Dirección:"));
        datosCliente.add(direccion);

        // Panel interactivo para agregar/quitar múltiples teléfonos
        JPanel panelTel = new JPanel(new BorderLayout(5, 5));
        panelTel.setBorder(BorderFactory.createTitledBorder("Teléfonos del cliente"));

        JPanel pFormTel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField txtNumTel = new JTextField(8);
        JComboBox<String> cbTipo = new JComboBox<>(new String[]{"PERSONAL", "TRABAJO", "CASA", "OTRO"});
        JButton btnAddTel = new JButton("+");
        JButton btnRemoveTel = new JButton("-");

        pFormTel.add(new JLabel("Núm:"));
        pFormTel.add(txtNumTel);
        pFormTel.add(cbTipo);
        pFormTel.add(btnAddTel);
        pFormTel.add(btnRemoveTel);

        JScrollPane scrollList = new JScrollPane(listTelefonos);
        scrollList.setPreferredSize(new Dimension(300, 80));

        btnAddTel.addActionListener(e -> {
            String num = txtNumTel.getText().trim();
            if (!num.isEmpty()) {
                modelTelefonos.addElement(new Telefono(num, cbTipo.getSelectedItem().toString()));
                txtNumTel.setText("");
            }
        });

        btnRemoveTel.addActionListener(e -> {
            int sel = listTelefonos.getSelectedIndex();
            if (sel >= 0) {
                modelTelefonos.remove(sel);
            }
        });

        panelTel.add(pFormTel, BorderLayout.NORTH);
        panelTel.add(scrollList, BorderLayout.CENTER);

        panelPrincipal.add(datosCliente, BorderLayout.NORTH);
        panelPrincipal.add(panelTel, BorderLayout.CENTER);

        return JOptionPane.showConfirmDialog(this, panelPrincipal, titulo,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private String validar(String nit, String nombre, String nitOriginal) throws SQLException {
        if (nit.isEmpty()) return "El NIT es obligatorio.";
        if (nombre.isEmpty()) return "El nombre es obligatorio.";
        if (dao.existeNit(nit, nitOriginal)) return "Ya existe un cliente con ese NIT.";
        return null;
    }

    private Cliente seleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= mostrados.size()) {
            aviso("Selecciona un cliente de la tabla.");
            return null;
        }
        return mostrados.get(fila);
    }

    private void aviso(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Aviso", JOptionPane.INFORMATION_MESSAGE);
    }

    private void errorBD(String mensaje, SQLException ex) {
        JOptionPane.showMessageDialog(this, mensaje + "\n" + ex.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
    }
}