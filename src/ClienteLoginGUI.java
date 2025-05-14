import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class ClienteLoginGUI extends JFrame {
    private JTextField campoUsuario;
    private JPasswordField campoClave;
    private JButton botonLogin, botonRegistro;
    private JLabel estado;
    

    public ClienteLoginGUI() {
        setTitle("🌟 Bienvenido al Chat");
        setSize(350, 250);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        //UIManager.put("Component.arc", 10); // redondeo general
        UIManager.put("TextComponent.arc", 5);       // Campos de texto ligeramente redondeados
        UIManager.put("Button.arc", 20);             // Botones más redondos


        JPanel panelPrincipal = new JPanel();
        panelPrincipal.setBackground(new Color(250, 250, 255));
        panelPrincipal.setLayout(new BoxLayout(panelPrincipal, BoxLayout.Y_AXIS));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        JLabel titulo = new JLabel("Iniciar sesión o registrarse", JLabel.CENTER);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        titulo.setForeground(new Color(33, 33, 33)); //nuevo


        campoUsuario = new JTextField(15);
        campoUsuario.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        campoUsuario.setAlignmentX(Component.CENTER_ALIGNMENT);
        campoUsuario.putClientProperty("JComponent.roundRect", true); //bordes redondeados
        //campoUsuario.putClientProperty("JComponent.arc", 5);
        JLabel labelUsuario = new JLabel("Usuario");
        labelUsuario.setAlignmentX(Component.CENTER_ALIGNMENT);
        labelUsuario.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        labelUsuario.setForeground(new Color(70, 70, 70));


        campoClave = new JPasswordField(15);
        campoClave.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        campoClave.setAlignmentX(Component.CENTER_ALIGNMENT);
        campoClave.putClientProperty("JComponent.roundRect", true); //bordes redondeados
        //campoClave.putClientProperty("JComponent.arc", 5);
        JLabel labelClave = new JLabel("Contraseña");
        labelClave.setAlignmentX(Component.CENTER_ALIGNMENT);
        labelClave.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        labelClave.setForeground(new Color(70, 70, 70));


        botonLogin = new JButton("🔓 Iniciar Sesión");
        botonLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        botonLogin.putClientProperty("JButton.arc", 20); //nuevo
        botonLogin.setBackground(new Color(33, 150, 243)); //nuevo
        botonLogin.setForeground(Color.WHITE); //nuevo
        botonLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        botonRegistro = new JButton("📝 Registrarse");        
        botonRegistro.setAlignmentX(Component.CENTER_ALIGNMENT);
        botonRegistro.putClientProperty("JButton.arc", 20); //nuevo
        botonRegistro.setBackground(new Color(100, 181, 246)); //nuevo
        botonRegistro.setForeground(Color.WHITE); //nuevo
        botonRegistro.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));



        estado = new JLabel(" ", JLabel.CENTER);
        estado.setForeground(Color.RED);
        estado.setAlignmentX(Component.CENTER_ALIGNMENT);

        panelPrincipal.add(titulo);
        panelPrincipal.add(Box.createVerticalStrut(10));
        
        JPanel grupoUsuario = new JPanel();
        grupoUsuario.setLayout(new BoxLayout(grupoUsuario, BoxLayout.Y_AXIS));
        grupoUsuario.setBackground(panelPrincipal.getBackground());
        grupoUsuario.setAlignmentX(Component.CENTER_ALIGNMENT);  // Alineado centrado
        grupoUsuario.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));  // Ocupa todo el ancho

        labelUsuario.setAlignmentX(Component.LEFT_ALIGNMENT);
        campoUsuario.setAlignmentX(Component.LEFT_ALIGNMENT);
        campoUsuario.setPreferredSize(new Dimension(200, 30));  // Ancho fijo decente

        grupoUsuario.add(labelUsuario);
        grupoUsuario.add(Box.createVerticalStrut(2));
        grupoUsuario.add(campoUsuario);
        panelPrincipal.add(grupoUsuario);

        panelPrincipal.add(Box.createVerticalStrut(10));
        JPanel grupoClave = new JPanel();
        grupoClave.setLayout(new BoxLayout(grupoClave, BoxLayout.Y_AXIS));
        grupoClave.setBackground(panelPrincipal.getBackground());
        grupoClave.setAlignmentX(Component.CENTER_ALIGNMENT);
        grupoClave.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        labelClave.setAlignmentX(Component.LEFT_ALIGNMENT);
        campoClave.setAlignmentX(Component.LEFT_ALIGNMENT);
        campoClave.setPreferredSize(new Dimension(200, 30));

        grupoClave.add(labelClave);
        grupoClave.add(Box.createVerticalStrut(2));
        grupoClave.add(campoClave);
        panelPrincipal.add(grupoClave);

        panelPrincipal.add(Box.createVerticalStrut(10)); // Espacio antes de botones
        panelPrincipal.add(botonLogin);
        panelPrincipal.add(Box.createVerticalStrut(5)); // Espacio entre botones
        panelPrincipal.add(botonRegistro);
        panelPrincipal.add(Box.createVerticalStrut(10)); // Espacio antes del estado
        panelPrincipal.add(estado);


        add(panelPrincipal);

        botonLogin.addActionListener(e -> enviarDatos("LOGIN"));
        botonRegistro.addActionListener(e -> enviarDatos("REGISTER"));

        setVisible(true);
    }


    private void enviarDatos(String tipo) {
        String usuario = campoUsuario.getText();
        String clave = new String(campoClave.getPassword());

        try {
            Socket socket = new Socket("localhost", 1234);
            PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            salida.println(tipo + ":" + usuario + ":" + clave);
            String respuesta = entrada.readLine();

            switch (respuesta) {
                case "LOGIN_OK":
                case "REGISTER_OK":
                    estado.setText("✅ Bienvenido, " + usuario);
                    dispose(); // Cierra la ventana de login
                    new ClienteChatGUI(usuario, socket); // Abre el chat
                    break;
                case "LOGIN_FAIL":
                    estado.setText("❌ Usuario o contraseña incorrectos.");
                    socket.close();
                    break;
                case "REGISTER_FAIL":
                    estado.setText("⚠️ Usuario ya existe.");
                    socket.close();
                    break;
                default:
                    estado.setText("❌ Error desconocido.");
                    socket.close();
            }
        } catch (IOException e) {
            estado.setText("❌ Error de conexión.");
        }
    }


    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            
            // Personalización global específica:
            UIManager.put("TextComponent.arc", 5);
            UIManager.put("Button.arc", 20);

            System.out.println("LookAndFeel actual: " + UIManager.getLookAndFeel().getName());

        } catch (Exception ex) {
            System.err.println("No se pudo aplicar FlatLaf.");
        }

        SwingUtilities.invokeLater(ClienteLoginGUI::new);
    }
}
