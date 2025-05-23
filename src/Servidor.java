// Servidor.java con soporte para grupos
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Servidor {
    private static final int PUERTO = 1234;
    private static final String ARCHIVO_USUARIOS = "usuarios.txt";
    private static ConcurrentHashMap<String, String> usuarios = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, PrintWriter> clientesConectados = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, Set<String>> grupos = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        cargarUsuarios();

        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("🟢 Servidor activo en el puerto " + PUERTO);

            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> manejarCliente(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void manejarCliente(Socket socket) {
        String usuarioActual = null;

        try (
            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter salida = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String mensaje = entrada.readLine();
            String[] partes = mensaje.split(":");

            if (partes[0].equals("LOGIN")) {
                String usuario = partes[1];
                String clave = partes[2];
                if (usuarios.containsKey(usuario) && usuarios.get(usuario).equals(clave)) {
                    salida.println("LOGIN_OK");
                    usuarioActual = usuario;
                    clientesConectados.put(usuario, salida);
                } else {
                    salida.println("LOGIN_FAIL");
                    socket.close();
                    return;
                }
            } else if (partes[0].equals("REGISTER")) {
                String usuario = partes[1];
                String clave = partes[2];
                if (usuarios.containsKey(usuario)) {
                    salida.println("REGISTER_FAIL");
                    socket.close();
                    return;
                } else {
                    usuarios.put(usuario, clave);
                    guardarUsuario(usuario, clave);
                    salida.println("REGISTER_OK");
                    usuarioActual = usuario;
                    clientesConectados.put(usuario, salida);
                }
            }

            enviarListaUsuarios();

            String input;
            while ((input = entrada.readLine()) != null) {
                if (input.equals("GET_USERS")) {
                    salida.println(crearMensajeUsuarios());
                } else if (input.startsWith("TO:")) {
                    String[] partesMensaje = input.split(":", 3);
                    String destino = partesMensaje[1];
                    String mensajeTexto = partesMensaje[2];

                    if (destino.startsWith("#")) {
                        // mensaje a grupo
                        String nombreGrupo = destino.substring(1);
                        Set<String> miembros = grupos.get(nombreGrupo);
                        if (miembros != null) {
                            for (String miembro : miembros) {
                                if (!miembro.equals(usuarioActual)) {
                                    PrintWriter salidaDestino = clientesConectados.get(miembro);
                                    if (salidaDestino != null) {
                                        salidaDestino.println("FROM:#" + nombreGrupo + ":" + usuarioActual + ":" + mensajeTexto);
                                    }
                                }
                            }
                        } else {
                            salida.println("ERROR:Grupo no existe.");
                        }
                    } else {
                        // mensaje privado
                        PrintWriter salidaDestino = clientesConectados.get(destino);
                        if (salidaDestino != null) {
                            salidaDestino.println("FROM:" + usuarioActual + ":" + mensajeTexto);
                        } else {
                            salida.println("ERROR:Usuario '" + destino + "' no conectado.");
                        }
                    }
                } else if (input.startsWith("CREATE_GROUP:")) {
                    String[] partesGrupo = input.split(":");
                    String nombreGrupo = partesGrupo[1];
                    Set<String> miembros = new HashSet<>(Arrays.asList(partesGrupo).subList(2, partesGrupo.length));
                    miembros.add(usuarioActual);
                    grupos.put(nombreGrupo, miembros);
                    salida.println("GROUP_CREATED:" + nombreGrupo);
                } else if (input.equals("GET_GROUPS")) {
                    List<String> gruposUsuario = new ArrayList<>();
                    for (Map.Entry<String, Set<String>> entry : grupos.entrySet()) {
                        if (entry.getValue().contains(usuarioActual)) {
                            gruposUsuario.add(entry.getKey());
                        }
                    }
                    salida.println("GROUPS:" + String.join(",", gruposUsuario));
                }
            }

        } catch (IOException e) {
            System.out.println("❌ Error con usuario " + usuarioActual);
        } finally {
            if (usuarioActual != null) {
                clientesConectados.remove(usuarioActual);
                enviarListaUsuarios();
                System.out.println("🔌 Usuario desconectado: " + usuarioActual);
            }
        }
    }

    private static void cargarUsuarios() {
        try (BufferedReader reader = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] partes = linea.split(":");
                if (partes.length == 2) {
                    usuarios.put(partes[0], partes[1]);
                }
            }
        } catch (IOException e) {
            System.out.println("⚠️ Archivo de usuarios no encontrado. Se creará uno nuevo.");
        }
    }

    private static void guardarUsuario(String user, String pass) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARCHIVO_USUARIOS, true))) {
            writer.write(user + ":" + pass);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("❌ Error al guardar usuario.");
        }
    }

    private static void enviarListaUsuarios() {
        StringBuilder lista = new StringBuilder("USERS:");
        for (String usuario : clientesConectados.keySet()) {
            lista.append(usuario).append(",");
        }

        String listaFinal = lista.toString();
        for (PrintWriter pw : clientesConectados.values()) {
            pw.println(listaFinal);
        }
    }

    private static String crearMensajeUsuarios() {
        StringBuilder lista = new StringBuilder("USERS:");
        for (String usuario : clientesConectados.keySet()) {
            lista.append(usuario).append(",");
        }
        return lista.toString();
    }
}