import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;

public class ClienteChatGUI extends JFrame {
    private JTextArea areaChat;
    private JTextField campoMensaje;
    private JButton botonEnviar;
    private JList<String> listaUsuarios;
    private DefaultListModel<String> modeloUsuarios;
    private JLabel etiquetaDestinatario;

    private PrintWriter salida;
    private BufferedReader entrada;
    private final String nombreUsuario;
    private final HashMap<String, StringBuilder> historiales = new HashMap<>();

    private String usuarioActualChat = null;


    public ClienteChatGUI(String nombreUsuario, Socket socket) {
        this.nombreUsuario = nombreUsuario;

        setTitle("🌈 Chat - " + nombreUsuario);
        setSize(600, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        Font fuente = new Font("Segoe UI Emoji", Font.PLAIN, 14);
        Color colorFondo = new Color(255, 248, 240);
        Color colorBoton = new Color(255, 204, 229);
        Color colorArea = new Color(255, 255, 255);

        // Panel usuarios
        modeloUsuarios = new DefaultListModel<>();
        listaUsuarios = new JList<>(modeloUsuarios);
        listaUsuarios.setFont(fuente);
        listaUsuarios.setBackground(new Color(240, 255, 255));
        listaUsuarios.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String seleccionado = listaUsuarios.getSelectedValue();
                if (seleccionado != null) {
                    usuarioActualChat = seleccionado;
                    etiquetaDestinatario.setText(usuarioActualChat);
                    areaChat.setText(historiales.getOrDefault(usuarioActualChat, new StringBuilder()).toString());
                }
            }
        });

        JPanel panelUsuarios = new JPanel(new BorderLayout());
        panelUsuarios.setBorder(BorderFactory.createTitledBorder("👥 Conectados"));
        panelUsuarios.add(new JScrollPane(listaUsuarios), BorderLayout.CENTER);
        panelUsuarios.setPreferredSize(new Dimension(150, 0));

        // Área de chat
        areaChat = new JTextArea();
        areaChat.setEditable(false);
        areaChat.setFont(fuente);
        areaChat.setBackground(colorArea);
        JScrollPane scrollChat = new JScrollPane(areaChat);
        scrollChat.setBorder(BorderFactory.createTitledBorder("💬 Chat"));

        etiquetaDestinatario = new JLabel("💬 Selecciona un usuario para chatear", JLabel.CENTER);
        etiquetaDestinatario.setFont(new Font("Segoe UI Emoji", Font.BOLD, 16));
        etiquetaDestinatario.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Entrada de mensaje
        campoMensaje = new JTextField();
        campoMensaje.setFont(fuente);
        campoMensaje.setBackground(Color.WHITE);
        campoMensaje.addActionListener(e -> enviarMensaje());

        botonEnviar = new JButton("Enviar ✉️");
        botonEnviar.setFont(fuente);
        botonEnviar.setBackground(colorBoton);
        botonEnviar.addActionListener(e -> enviarMensaje());

        JPanel panelEntrada = new JPanel(new BorderLayout());
        panelEntrada.setBackground(colorFondo);
        panelEntrada.add(campoMensaje, BorderLayout.CENTER);
        panelEntrada.add(botonEnviar, BorderLayout.EAST);

        add(panelUsuarios, BorderLayout.WEST);
        add(etiquetaDestinatario, BorderLayout.NORTH);
        add(scrollChat, BorderLayout.CENTER);
        add(panelEntrada, BorderLayout.SOUTH);

        getContentPane().setBackground(colorFondo);

        try {
            salida = new PrintWriter(socket.getOutputStream(), true);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error de conexión.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // Hilo para recibir mensajes
        new Thread(() -> {
            try {
                String input;
                while ((input = entrada.readLine()) != null) {
                    if (input.startsWith("FROM:")) {
                        String[] partes = input.split(":", 3);
                        String remitente = partes[1];
                        String texto = remitente + ": " + partes[2] + "\n";

                        historiales.computeIfAbsent(remitente, k -> new StringBuilder()).append(texto);

                        if (remitente.equals(usuarioActualChat)) {
                            areaChat.append(texto);
                        }

                    } else if (input.startsWith("USERS:")) {
                        String[] usuarios = input.substring(6).split(",");
                        SwingUtilities.invokeLater(() -> {
                            modeloUsuarios.clear();
                            for (String usuario : usuarios) {
                                if (!usuario.isBlank() && !usuario.equals(nombreUsuario)) {
                                    modeloUsuarios.addElement(usuario);
                                }
                            }
                        });

                    } else if (input.startsWith("ERROR:")) {
                        areaChat.append("⚠️ " + input + "\n");
                    }
                }

            } catch (IOException e) {
                areaChat.append("❌ Conexión perdida.\n");
            }
        }).start();

        //Solicita la lista de usuarios
        salida.println("GET_USERS");

        setVisible(true);

        //Hilo que actualiza la lista de usuarios cada 5 segundos
        new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(5000); // Espera 5 segundos
                    salida.println("GET_USERS"); // Solicita la lista actualizada
                }
            } catch (InterruptedException e) {
                System.out.println("⛔ Hilo de actualización interrumpido.");
            }
        }).start();

    }

    private void enviarMensaje() {
        String mensaje = campoMensaje.getText().trim();
        String destinatario = usuarioActualChat;

        if (!mensaje.isEmpty() && destinatario != null) {
            salida.println("TO:" + destinatario + ":" + mensaje);
            String mensajeFormateado = "Yo: " + mensaje + "\n";
            historiales.computeIfAbsent(destinatario, k -> new StringBuilder()).append(mensajeFormateado);

            if (destinatario.equals(usuarioActualChat)) {
                areaChat.append(mensajeFormateado);
            }

            campoMensaje.setText("");
        } else {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario y escribe un mensaje.", "Aviso", JOptionPane.WARNING_MESSAGE);
        }

    }
}
