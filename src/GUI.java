import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.Arrays;

public class GUI extends JFrame {
    private JPanel panelPrincipal;
    private JPanel panelUsuarios;
    private JPanel panelOpciones;
    private JButton altaBoton;
    private JButton bajaBoton;
    private JButton modifBoton;
    private JTextField nombre;
    private JTextField direccion;
    private JTextField telefono;
    private static final String URL = "jdbc:mariadb://localhost:3307/agenda";
    private static final String USER = "usuario1";
    private static final String PASSWORD = "superpassword";
    private DefaultTableModel modeloTabla;

    public GUI() {
        panelPrincipal = new JPanel(new BorderLayout());
        panelOpciones = new JPanel(new GridBagLayout());
        panelUsuarios = new JPanel();

        String[] columnas = {"Id", "Persona Id", "Nombre", "Dirección", "Teléfono"};

        modeloTabla = new DefaultTableModel(columnas, 0);
        JTable tabla = new JTable(modeloTabla);
        JScrollPane scroll = new JScrollPane(tabla);

        agregarUsuariosTabla();

        panelUsuarios.setLayout(new BorderLayout());
        panelUsuarios.add(scroll, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        panelOpciones.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1;
        nombre = new JTextField(20);
        panelOpciones.add(nombre, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panelOpciones.add(new JLabel("Dirección:"), gbc);
        gbc.gridx = 1;
        direccion = new JTextField(20);
        panelOpciones.add(direccion, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panelOpciones.add(new JLabel("Teléfono:"), gbc);
        gbc.gridx = 1;
        telefono = new JTextField(20);
        panelOpciones.add(telefono, gbc);

        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 1;
        altaBoton = new JButton("Alta");
        altaBoton.setEnabled(false);
        altaBoton.setFocusPainted(false);
        panelOpciones.add(altaBoton, gbc);

        //Verifica que en los campos del nombre y direccion haya al menos 3 letras
        DocumentListener listener = new DocumentListener() {
            private void check() {
                boolean nombreOk = Arrays.stream(nombre.getText().trim().split("\\s+"))
                        .anyMatch(p -> p.length() >= 3);
                boolean direccionOk = Arrays.stream(direccion.getText().trim().split("\\s+"))
                        .anyMatch(p -> p.length() >= 3);
                altaBoton.setEnabled(nombreOk && direccionOk);
            }
            public void insertUpdate(DocumentEvent e) { check(); }
            public void removeUpdate(DocumentEvent e) { check(); }
            public void changedUpdate(DocumentEvent e) { check(); }
        };

        nombre.getDocument().addDocumentListener(listener);
        direccion.getDocument().addDocumentListener(listener);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        bajaBoton = new JButton("Baja");
        bajaBoton.setFocusPainted(false);
        modifBoton = new JButton("Modificar Usuario");
        modifBoton.setFocusPainted(false);

        panelBotones.add(bajaBoton);
        panelBotones.add(modifBoton);
        panelUsuarios.setLayout(new BorderLayout());
        panelUsuarios.add(panelBotones, BorderLayout.NORTH);
        panelUsuarios.add(scroll, BorderLayout.CENTER);

        altaBoton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String nuevoUsuario = "INSERT INTO Personas (nombre, direccion) VALUES (?, ?)";
                try(Connection conexion = DriverManager.getConnection(URL, USER, PASSWORD);
                    PreparedStatement ps = conexion.prepareStatement(nuevoUsuario, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, nombre.getText());
                    ps.setString(2, direccion.getText());
                    ps.executeUpdate();

                    try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            long personaId = generatedKeys.getLong(1);

                            String sqlTel = "INSERT INTO Telefonos (personaId, telefono) VALUES (?, ?)";
                            try (PreparedStatement psTel = conexion.prepareStatement(sqlTel)) {
                                psTel.setLong(1, personaId);
                                psTel.setString(2, telefono.getText());
                                psTel.executeUpdate();
                            }
                        }
                    }

                    agregarUsuariosTabla();
                } catch (SQLException j) {
                    j.printStackTrace();
                }
                nombre.setText("");
                direccion.setText("");
                telefono.setText("");
            }
        });

        bajaBoton.addActionListener(e -> {
            int filaSeleccionada = tabla.getSelectedRow();
            if (filaSeleccionada != -1) {
                int idUsuario = (int) tabla.getValueAt(filaSeleccionada, 0); // Columna ID

                String sql = "DELETE FROM Personas WHERE id = ?";

                try (Connection conexion = DriverManager.getConnection(URL, USER, PASSWORD);
                     PreparedStatement ps = conexion.prepareStatement(sql)) {

                    ps.setInt(1, idUsuario);
                    int filas = ps.executeUpdate();

                    if (filas > 0) {
                        System.out.println("Usuario eliminado correctamente.");
                        agregarUsuariosTabla();
                    }

                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Selecciona un usuario para eliminar");
            }
        });

        modifBoton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int filaSeleccionada = tabla.getSelectedRow();
                if (filaSeleccionada != -1) {
                    JFrame ventanaModif = new JFrame();
                    JDialog x = new JDialog(ventanaModif, "Modificar Usuario", true);

                    JTextField campoNombre = new JTextField(15);
                    JTextField campoDireccion = new JTextField(15);
                    JTextField campoTelefono = new JTextField(15);

                    campoNombre.setText((String) tabla.getValueAt(filaSeleccionada, 2));
                    campoDireccion.setText((String) tabla.getValueAt(filaSeleccionada, 3));
                    campoTelefono.setText((String) tabla.getValueAt(filaSeleccionada, 4));

                    JPanel opcionesModif = new JPanel(new GridBagLayout());
                    GridBagConstraints gbc = new GridBagConstraints();
                    gbc.insets = new Insets(5, 5, 5, 5);
                    gbc.anchor = GridBagConstraints.WEST;

                    gbc.gridx = 0; gbc.gridy = 0;
                    opcionesModif.add(new JLabel("Nombre:"), gbc);
                    gbc.gridx = 1;
                    opcionesModif.add(campoNombre, gbc);

                    gbc.gridx = 0; gbc.gridy = 1;
                    opcionesModif.add(new JLabel("Dirección:"), gbc);
                    gbc.gridx = 1;
                    opcionesModif.add(campoDireccion, gbc);

                    gbc.gridx = 0; gbc.gridy = 2;
                    opcionesModif.add(new JLabel("Teléfono:"), gbc);
                    gbc.gridx = 1;
                    opcionesModif.add(campoTelefono, gbc);

                    JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER));
                    JButton guardar = new JButton("Guardar");
                    JButton cancelar = new JButton("Cancelar");
                    botones.add(guardar);
                    botones.add(cancelar);

                    x.setLayout(new BorderLayout());
                    x.add(opcionesModif, BorderLayout.CENTER);
                    x.add(botones, BorderLayout.SOUTH);

                    x.pack();
                    x.setLocationRelativeTo(null);

                    guardar.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            int idUsuario = (int) tabla.getValueAt(filaSeleccionada, 0);
                            String sql = "UPDATE Personas SET nombre=?, direccion=? WHERE id=?";
                            String sqlTel = "UPDATE Telefonos SET telefono=? WHERE personaId=?";

                            try (Connection conexion = DriverManager.getConnection(URL, USER, PASSWORD);
                                 PreparedStatement ps = conexion.prepareStatement(sql);
                                 PreparedStatement psTel = conexion.prepareStatement(sqlTel)) {

                                ps.setString(1, campoNombre.getText());
                                ps.setString(2, campoDireccion.getText());
                                ps.setInt(3, idUsuario);
                                ps.executeUpdate();

                                psTel.setString(1, campoTelefono.getText());
                                psTel.setInt(2, idUsuario);
                                psTel.executeUpdate();

                                JOptionPane.showMessageDialog(x, "Usuario actualizado correctamente");
                                agregarUsuariosTabla();
                                x.dispose();

                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                    cancelar.addActionListener(ev -> x.dispose());
                    x.setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(null, "Selecciona un usuario para eliminar");
                }
            }
        });

        setTitle("CRUD");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 500);
        setLocationRelativeTo(null);

        panelPrincipal.add(panelOpciones, BorderLayout.WEST);
        panelPrincipal.add(panelUsuarios, BorderLayout.CENTER);
        add(panelPrincipal);
    }

    //Metodo para agregar todos los datos de la base de datos a la tabla para mostrarlo en la GUI
    public void agregarUsuariosTabla(){
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        modeloTabla.setRowCount(0);

        try {
            Class.forName("org.mariadb.jdbc.Driver");

            System.out.println("Conectando a la base de datos...");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);

            System.out.println("\n=== LISTADO DE PERSONAS ===");
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM Personas");

            while (rs.next()) {
                int id = rs.getInt("id");
                String nombre = rs.getString("nombre");
                String direccion = rs.getString("direccion");

                System.out.println("ID: " + id + ", Nombre: " + nombre + ", Dirección: " + direccion);

                System.out.println("  Teléfonos:");
                Statement stmtTelefonos = conn.createStatement();
                ResultSet rsTelefonos = stmtTelefonos.executeQuery(
                        "SELECT telefono FROM Telefonos WHERE personaId = " + id);

                StringBuilder telefonosConcatenados = new StringBuilder();
                while (rsTelefonos.next()) {
                    if (telefonosConcatenados.length() > 0) telefonosConcatenados.append(", ");
                    telefonosConcatenados.append(rsTelefonos.getString("telefono"));
                }
                rsTelefonos.close();
                stmtTelefonos.close();
                modeloTabla.addRow(new Object[]{id, id, nombre, direccion, telefonosConcatenados.toString()});
            }
        } catch (SQLException se) {
            se.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
        System.out.println("\nConexión cerrada. Programa terminado.");
    }
}
