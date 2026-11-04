package com.sistemaventas.servidor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServidorAPI {

    // REEMPLAZA CON TUS CREDENCIALES REALES DE MYSQL
    private static final String DB_URL = "jdbc:mysql://localhost:3306/sistema_ventas?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root"; 
    private static final String DB_PASS = "1234"; 

    public static void main(String[] args) {
        try {
            // Escucha en "0.0.0.0" para permitir conexiones desde el emulador (10.0.2.2 o IP local)
            HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8085), 0);
            
            // Endpoints API
            server.createContext("/api/ventas", new ManejadorVentas());
            server.createContext("/api/clientes", new ManejadorClientes());
            server.createContext("/api/vendedores", new ManejadorVendedores());
            server.createContext("/api/productos", new ManejadorProductos());
            
            server.setExecutor(null); 
            server.start();

            System.out.println("=========================================");
            System.out.println(" Servidor API activo en puerto 8085");
            System.out.println(" Escuchando peticiones en 0.0.0.0:8085");
            System.out.println("=========================================");
        } catch (IOException e) {
            System.err.println("Error al iniciar el servidor API: " + e.getMessage());
        }
    }

    // ==========================================
    // 1. MANEJADOR: REGISTRAR VENTA (POST)
    // ==========================================
    static class ManejadorVentas implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                InputStream inputStream = exchange.getRequestBody();
                String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

                System.out.println("Petición de venta recibida desde Android: " + body);

                boolean guardado = guardarVentaEnBD(body);
                String response = guardado ? "{\"status\":\"OK\"}" : "{\"status\":\"ERROR\"}";
                int statusCode = guardado ? 200 : 500;

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes(StandardCharsets.UTF_8));
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private boolean guardarVentaEnBD(String jsonBody) {
            Connection conn = null;
            try {
                int idCliente = obtenerValorInt(jsonBody, "idCliente");
                int idVendedor = obtenerValorInt(jsonBody, "idVendedor");
                int idProducto = obtenerValorInt(jsonBody, "idProducto");
                int cantidad = obtenerValorInt(jsonBody, "cantidad");

                Class.forName("com.mysql.cj.jdbc.Driver");
                conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                conn.setAutoCommit(false);

                double precioUnitario = 50.00;
                double totalCalculado = precioUnitario * cantidad;

                // Insertar Venta
                String sqlVenta = "INSERT INTO venta (id_cliente, id_vendedor, fecha_venta, usuario_emision, total, metodo_pago) " +
                                  "VALUES (?, ?, NOW(), 'AndroidApp', ?, 'EFECTIVO')";
                PreparedStatement stmtVenta = conn.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS);
                stmtVenta.setInt(1, idCliente);
                stmtVenta.setInt(2, idVendedor);
                stmtVenta.setDouble(3, totalCalculado);
                stmtVenta.executeUpdate();

                ResultSet rs = stmtVenta.getGeneratedKeys();
                int idVentaGenerado = rs.next() ? rs.getInt(1) : 0;

                // Insertar Detalle
                String sqlDetalle = "INSERT INTO detalle_venta (id_venta, id_producto, cantidad, precio_unitario, subtotal) " +
                                    "VALUES (?, ?, ?, ?, ?)";
                PreparedStatement stmtDetalle = conn.prepareStatement(sqlDetalle);
                stmtDetalle.setInt(1, idVentaGenerado);
                stmtDetalle.setInt(2, idProducto);
                stmtDetalle.setInt(3, cantidad);
                stmtDetalle.setDouble(4, precioUnitario);
                stmtDetalle.setDouble(5, totalCalculado);
                stmtDetalle.executeUpdate();

                conn.commit();
                System.out.println("¡ÉXITO! Venta " + idVentaGenerado + " registrada correctamente en MySQL.");
                return true;
            } catch (Exception e) {
                System.err.println(">>> ERROR MYSQL: " + e.getMessage());
                e.printStackTrace();
                if (conn != null) try { conn.rollback(); } catch (Exception ex) {}
                return false;
            } finally {
                if (conn != null) try { conn.close(); } catch (Exception ex) {}
            }
        }

        private int obtenerValorInt(String json, String clave) {
            Pattern pattern = Pattern.compile("\"" + clave + "\"\\s*:\\s*(\\d+)");
            Matcher matcher = pattern.matcher(json);
            return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
        }
    }

    // ==========================================
    // 2. MANEJADOR: OBTENER CLIENTES (GET)
    // ==========================================
    static class ManejadorClientes implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                StringBuilder json = new StringBuilder("[");
                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT id_cliente, nombre FROM cliente")) {
                    
                    boolean primero = true;
                    while (rs.next()) {
                        if (!primero) json.append(",");
                        json.append("{\"idCliente\":").append(rs.getInt("id_cliente"))
                            .append(",\"nombre\":\"").append(escaparJSON(rs.getString("nombre"))).append("\"}");
                        primero = false;
                    }
                } catch (Exception e) {
                    System.err.println("Error Clientes: " + e.getMessage());
                }
                json.append("]");

                responderJSON(exchange, json.toString());
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    // ==========================================
    // 3. MANEJADOR: OBTENER VENDEDORES (GET)
    // ==========================================
    static class ManejadorVendedores implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                StringBuilder json = new StringBuilder("[");
                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT id_vendedor, nombre FROM vendedor")) {
                    
                    boolean primero = true;
                    while (rs.next()) {
                        if (!primero) json.append(",");
                        json.append("{\"idVendedor\":").append(rs.getInt("id_vendedor"))
                            .append(",\"nombre\":\"").append(escaparJSON(rs.getString("nombre"))).append("\"}");
                        primero = false;
                    }
                } catch (Exception e) {
                    System.err.println("Error Vendedores: " + e.getMessage());
                }
                json.append("]");

                responderJSON(exchange, json.toString());
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    // ==========================================
    // 4. MANEJADOR: OBTENER PRODUCTOS (GET)
    // ==========================================
    static class ManejadorProductos implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                StringBuilder json = new StringBuilder("[");
                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT id_producto, nombre, precio FROM producto")) {
                    
                    boolean primero = true;
                    while (rs.next()) {
                        if (!primero) json.append(",");
                        json.append("{\"idProducto\":").append(rs.getInt("id_producto"))
                            .append(",\"nombre\":\"").append(escaparJSON(rs.getString("nombre")))
                            .append("\",\"precio\":").append(rs.getDouble("precio")).append("}");
                        primero = false;
                    }
                } catch (Exception e) {
                    System.err.println("Error Productos: " + e.getMessage());
                }
                json.append("]");

                responderJSON(exchange, json.toString());
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    // Sanea cadenas para evitar romper la sintaxis JSON
    private static String escaparJSON(String texto) {
        if (texto == null) return "";
        return texto.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    private static void responderJSON(HttpExchange exchange, String jsonResponse) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}