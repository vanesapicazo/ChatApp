// Cliente.java actualizado con soporte para grupos
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class ClienteChatGUI {
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private String usuario;
    private JFrame ventana;
    private JComboBox<String> listaUsuarios;
    private JTextArea areaMensajes;
    private JTextField campoEntrada;
    private JButton botonEnviar;
    private JButton botonGrupo;
    private DefaultComboBoxModel<String> modeloUsuarios;
    private java.util.List<String> grupos = new ArrayList<>();
    private JTextField campoBusqueda;
    private JList<String> listaChats;
    private DefaultListModel<String> modeloListaChats;
    private Map<String, StringBuilder> historialMensajes = new HashMap<>();
    private Map<String, Integer> mensajesNoLeidos = new HashMap<>();



    public ClienteChatGUI(String usuario, Socket socket) {
        this.usuario = usuario;
        this.socket = socket;
    
        try {
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "❌ Error al conectar con el servidor.");
            System.exit(1);
        }
    
        crearVentana(); // Esto usa 'usuario' en el título de la ventana
        salida.println("GET_GROUPS"); // para actualizar la lista inicial de grupos
        recibirMensajes(); // inicia el hilo de recepción
    }
    
    
    
    

    public void iniciarSesion() {
        while (true) {
            String usuario = JOptionPane.showInputDialog("Nombre de usuario:");
            String clave = JOptionPane.showInputDialog("Contraseña:");

            salida.println("LOGIN:" + usuario + ":" + clave);

            try {
                String respuesta = entrada.readLine();
                if ("LOGIN_OK".equals(respuesta)) {
                    this.usuario = usuario;
                    break;
                } else {
                    int opcion = JOptionPane.showConfirmDialog(null, "Usuario no registrado. ¿Deseas registrarte?", "Registro", JOptionPane.YES_NO_OPTION);
                    if (opcion == JOptionPane.YES_OPTION) {
                        salida.println("REGISTER:" + usuario + ":" + clave);
                        String resp = entrada.readLine();
                        if ("REGISTER_OK".equals(resp)) {
                            this.usuario = usuario;
                            break;
                        } else {
                            JOptionPane.showMessageDialog(null, "Ese usuario ya existe.");
                        }
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Error en comunicación con el servidor.");
            }
        }
    }

    private void crearVentana() {
        ventana = new JFrame("Chat de " + usuario);
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventana.setSize(800, 600);
        ventana.setLayout(new BorderLayout());
    
        // 🟦 Panel izquierdo: búsqueda + lista de chats
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(200, 0)); // ancho fijo
    
        campoBusqueda = new JTextField();
        campoBusqueda.setToolTipText("Buscar usuario o grupo...");
        leftPanel.add(campoBusqueda, BorderLayout.NORTH);
    
        modeloListaChats = new DefaultListModel<>();
        listaChats = new JList<>(modeloListaChats);
        listaChats.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    
        JScrollPane scrollChats = new JScrollPane(listaChats);
        leftPanel.add(scrollChats, BorderLayout.CENTER);
    
        ventana.add(leftPanel, BorderLayout.WEST);
    
        // 🟩 Área central de mensajes
        areaMensajes = new JTextArea();
        areaMensajes.setEditable(false);
        ventana.add(new JScrollPane(areaMensajes), BorderLayout.CENTER);
    
        // 🟥 Parte inferior con entrada y botones
        JPanel panelAbajo = new JPanel(new BorderLayout());
        campoEntrada = new JTextField();
        botonEnviar = new JButton("Enviar");
        botonGrupo = new JButton("Crear grupo");
    
        panelAbajo.add(campoEntrada, BorderLayout.CENTER);
        panelAbajo.add(botonEnviar, BorderLayout.EAST);
        panelAbajo.add(botonGrupo, BorderLayout.WEST);
        ventana.add(panelAbajo, BorderLayout.SOUTH);
    
        // Listeners
        botonEnviar.addActionListener(e -> enviarMensaje());
        campoEntrada.addActionListener(e -> enviarMensaje());
        botonGrupo.addActionListener(e -> crearGrupo());
    
        listaChats.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String seleccionado = listaChats.getSelectedValue();
                if (seleccionado != null) {
                    String limpio = seleccionado.replaceAll(" \\(\\d+\\)$", ""); // elimina contador
                    mensajesNoLeidos.remove(limpio);
        
                    for (int i = 0; i < modeloListaChats.size(); i++) {
                        String actual = modeloListaChats.get(i).replaceAll(" \\(\\d+\\)$", "");
                        if (actual.equals(limpio)) {
                            modeloListaChats.set(i, limpio); // restaurar sin contador
                            break;
                        }
                    }
        
                    StringBuilder historial = historialMensajes.getOrDefault(limpio, new StringBuilder());
                    areaMensajes.setText(historial.toString());
                }
            }
        });
        
        
    
        ventana.setVisible(true);
    }


    private void actualizarListaUsuarios(String mensaje) {
        String[] partes = mensaje.substring(6).split(",");
        modeloUsuarios = new DefaultComboBoxModel<>();
        for (String usuarioRemoto : partes) {
            if (!usuarioRemoto.equals(usuario) && !usuarioRemoto.isEmpty()) {
                modeloUsuarios.addElement(usuarioRemoto);
            }
        }
    
        // Actualiza la lista de chats con usuarios conectados si no están ya
        for (int i = 0; i < modeloUsuarios.getSize(); i++) {
            String u = modeloUsuarios.getElementAt(i);
            if (!modeloListaChats.contains(u)) {
                modeloListaChats.addElement(u);
            }
        }
    }

    private void actualizarNotificacion(String nombreChat) {
        int sinLeer = mensajesNoLeidos.getOrDefault(nombreChat, 0);
        String displayName = sinLeer > 0 ? nombreChat + " (" + sinLeer + ")" : nombreChat;
    
        for (int i = 0; i < modeloListaChats.size(); i++) {
            String actual = modeloListaChats.get(i);
            String limpio = actual.replaceAll(" \\(\\d+\\)$", ""); // elimina " (n)" si existe
            if (limpio.equals(nombreChat)) {
                modeloListaChats.set(i, displayName);
                return;
            }
        }
    
        // Si no existe aún, agregar
        modeloListaChats.addElement(displayName);
    }
    
    
    
    

    private void enviarMensaje() {
        String destino = listaChats.getSelectedValue();
        String mensaje = campoEntrada.getText();
        if (destino != null && !mensaje.isEmpty()) {
            salida.println("TO:" + destino + ":" + mensaje);
    
            StringBuilder historial = historialMensajes.computeIfAbsent(destino, k -> new StringBuilder());
            historial.append("Yo: ").append(mensaje).append("\n");
    
            areaMensajes.setText(historial.toString());
            campoEntrada.setText("");
        }
    }
    
    private void procesarMensajeRecibido(String linea) {
        String[] partes = linea.split(":", 3); // FROM:usuario:mensaje
        if (partes.length >= 3) {
            String remitente = partes[1];
            String mensaje = partes[2];
    
            historialMensajes.putIfAbsent(remitente, new StringBuilder());
            historialMensajes.get(remitente).append(remitente).append(": ").append(mensaje).append("\n");
    
            SwingUtilities.invokeLater(() -> {
                boolean encontrado = false;
                for (int i = 0; i < modeloListaChats.size(); i++) {
                    String val = modeloListaChats.get(i);
                    String limpio = val.replaceAll(" \\(\\d+\\)$", "");
                    if (limpio.equals(remitente)) {
                        encontrado = true;
                        break;
                    }
                }
                if (!encontrado) {
                    modeloListaChats.addElement(remitente);
                }
                if (listaChats.getSelectedValue() != null && listaChats.getSelectedValue().equals(remitente)) {
                    areaMensajes.setText(historialMensajes.get(remitente).toString());
                }
            });

            String seleccionado = listaChats.getSelectedValue();
            if (seleccionado == null || !seleccionado.equals(remitente)) {
                mensajesNoLeidos.put(remitente, mensajesNoLeidos.getOrDefault(remitente, 0) + 1);
                actualizarNotificacion(remitente);
            }

        }

    }

    private void crearGrupo() {
        String nombreGrupo = JOptionPane.showInputDialog("Nombre del grupo:");
        if (nombreGrupo != null && !nombreGrupo.isEmpty()) {
            java.util.List<String> seleccionados = new ArrayList<>();
            for (int i = 0; i < modeloUsuarios.getSize(); i++) {
                String u = modeloUsuarios.getElementAt(i);
                if (!u.equals(usuario)) {
                    int opcion = JOptionPane.showConfirmDialog(null, "¿Agregar a " + u + " al grupo?", "Grupo", JOptionPane.YES_NO_OPTION);
                    if (opcion == JOptionPane.YES_OPTION) {
                        seleccionados.add(u);
                    }
                }
            }
            if (!seleccionados.isEmpty()) {
                StringBuilder sb = new StringBuilder("CREATE_GROUP:" + nombreGrupo);
                for (String miembro : seleccionados) {
                    sb.append(":" + miembro);
                }
                salida.println(sb.toString());
                salida.println("GET_GROUPS");
            }
        }
    }

    private void actualizarListaGrupos(String mensaje) {
        String[] partes = mensaje.split(":", 2);
        if (partes.length == 2) {
            String[] nombres = partes[1].split(",");
            for (String g : nombres) {
                if (!g.isEmpty() && !modeloListaChats.contains("#" + g)) {
                    modeloListaChats.addElement("#" + g);
                }
            }
        }
    }
    

    private void procesarMensajeDeGrupo(String linea) {
        // FROM:#grupo:usuario:mensaje
        String[] partes = linea.split(":", 4);
        if (partes.length >= 4) {
            String grupo = "#" + partes[1].substring(1);  // Asegura que tenga #
            String remitente = partes[2];
            String mensaje = partes[3];
    
            historialMensajes.putIfAbsent(grupo, new StringBuilder());
            historialMensajes.get(grupo).append(remitente).append(" (").append(grupo).append("): ").append(mensaje).append("\n");
    
            SwingUtilities.invokeLater(() -> {
                boolean encontrado = false;
                for (int i = 0; i < modeloListaChats.size(); i++) {
                    String val = modeloListaChats.get(i);
                    String limpio = val.replaceAll(" \\(\\d+\\)$", "");
                    if (limpio.equals(remitente)) {
                        encontrado = true;
                        break;
                    }
                }
                if (!encontrado) {
                    modeloListaChats.addElement(remitente);
                }
                if (listaChats.getSelectedValue() != null && listaChats.getSelectedValue().equals(grupo)) {
                    areaMensajes.setText(historialMensajes.get(grupo).toString());
                }
            });

            String seleccionado = listaChats.getSelectedValue();
            if (seleccionado == null || !seleccionado.equals(grupo)) {
                mensajesNoLeidos.put(grupo, mensajesNoLeidos.getOrDefault(grupo, 0) + 1);
                actualizarNotificacion(grupo);
            }

        }

    }
    

    private void recibirMensajes() {
        new Thread(() -> {
            try {
                String linea;
                while ((linea = entrada.readLine()) != null) {
                    final String mensaje = linea;
    
                    if (mensaje.startsWith("USERS:")) {
                        SwingUtilities.invokeLater(() -> actualizarListaUsuarios(mensaje));
                    } else if (mensaje.startsWith("FROM:#")) {
                        SwingUtilities.invokeLater(() -> procesarMensajeDeGrupo(mensaje));
                    } else if (mensaje.startsWith("FROM:")) {
                        SwingUtilities.invokeLater(() -> procesarMensajeRecibido(mensaje));
                    } else if (mensaje.startsWith("GROUPS:")) {
                        SwingUtilities.invokeLater(() -> actualizarListaGrupos(mensaje));
                    }
                }
            } catch (IOException e) {
                System.out.println("❌ Error al recibir mensajes.");
            }
        }).start();
    }
    
    
    
    

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 1234);
            new ClienteChatGUI("", socket); // el constructor se encarga de todo
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "❌ No se pudo conectar al servidor.");
        }
    }
    
    
}
