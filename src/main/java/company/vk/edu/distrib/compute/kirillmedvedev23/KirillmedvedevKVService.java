package company.vk.edu.distrib.compute.kirillmedvedev23;

import company.vk.edu.distrib.compute.Dao;
import company.vk.edu.distrib.compute.KVService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KirillmedvedevKVService implements KVService {
    private static final Logger log = LoggerFactory.getLogger(KirillmedvedevKVService.class);

    private final int port;
    private final Dao<byte[]> dao;
    private HttpServer server;
    private ExecutorService executor;

    public KirillmedvedevKVService(int port, Dao<byte[]> dao) {
        this.port = port;
        this.dao = dao;
    }

    @Override
    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/v0/status", new StatusHandler());
            server.createContext("/v0/entity", new EntityHandler(dao));
            executor = Executors.newFixedThreadPool(4);
            server.setExecutor(executor);
            server.start();
            log.info("Server started on port {}", port);
        } catch (IOException e) {
            throw new RuntimeException("Failed to start server", e);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop(0);
        }
        if (executor != null) {
            executor.shutdown();
        }
        try {
            dao.close();
        } catch (IOException e) {
            log.debug("Error closing dao", e);
        }
    }

    private static final class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    private static final class EntityHandler implements HttpHandler {
        private static final String ID_PARAM = "id";
        private final Dao<byte[]> dao;

        EntityHandler(Dao<byte[]> dao) {
            this.dao = dao;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String id = extractId(exchange.getRequestURI());

            if (id == null || id.isEmpty()) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            try {
                switch (exchange.getRequestMethod()) {
                    case "GET" -> handleGet(exchange, id);
                    case "PUT" -> handlePut(exchange, id);
                    case "DELETE" -> handleDelete(exchange, id);
                    default -> exchange.sendResponseHeaders(405, -1);
                }
            } catch (NoSuchElementException e) {
                exchange.sendResponseHeaders(404, -1);
            } catch (IllegalArgumentException e) {
                exchange.sendResponseHeaders(400, -1);
            } catch (Exception e) {
                log.error("Internal error", e);
                exchange.sendResponseHeaders(500, -1);
            }
        }

        private void handleGet(HttpExchange exchange, String id) throws IOException {
            byte[] value = dao.get(id);
            exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
            exchange.sendResponseHeaders(200, value.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(value);
            }
        }

        private void handlePut(HttpExchange exchange, String id) throws IOException {
            try (InputStream is = exchange.getRequestBody()) {
                byte[] body = is.readAllBytes();
                dao.upsert(id, body);
            }
            exchange.sendResponseHeaders(201, -1);
        }

        private void handleDelete(HttpExchange exchange, String id) throws IOException {
            dao.delete(id);
            exchange.sendResponseHeaders(202, -1);
        }

        private static String extractId(URI uri) {
            String query = uri.getRawQuery();
            if (query == null || query.isEmpty()) {
                return null;
            }
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2 && ID_PARAM.equals(kv[0])) {
                    return kv[1];
                }
            }
            return null;
        }
    }
}
