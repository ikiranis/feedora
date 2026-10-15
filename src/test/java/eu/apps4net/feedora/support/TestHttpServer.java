package eu.apps4net.feedora.support;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal local HTTP server, so the code that downloads feeds and pages can be tested without the internet.
 */
public class TestHttpServer implements AutoCloseable {
    private record Response(int status, String contentType, String body, Map<String, String> headers) {}

    private final HttpServer server;
    private final Map<String, Response> responses = new ConcurrentHashMap<>();

    public TestHttpServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            Response response = responses.get(exchange.getRequestURI().getPath());
            if (response == null) {
                response = new Response(404, "text/plain", "Not found", Map.of());
            }
            response.headers().forEach((name, value) -> exchange.getResponseHeaders().add(name, value));
            exchange.getResponseHeaders().add("Content-Type", response.contentType());
            byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(response.status(), bytes.length == 0 ? -1 : bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
    }

    public TestHttpServer respond(String path, int status, String contentType, String body) {
        responses.put(path, new Response(status, contentType, body, Map.of()));
        return this;
    }

    public TestHttpServer redirect(String path, String location) {
        responses.put(path, new Response(302, "text/plain", "", Map.of("Location", location)));
        return this;
    }

    public String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    @Override
    public void close() {
        server.stop(0);
    }

    public static String rss(String title, String... items) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<rss version=\"2.0\"><channel><title>" + title + "</title><link>http://example.com</link>"
                + "<description>Feed description</description>");
        for (String item : items) {
            xml.append(item);
        }
        return xml.append("</channel></rss>").toString();
    }

    public static String rssItem(String title, String link, String description) {
        return "<item><title>" + title + "</title><link>" + link + "</link><description>" + description
                + "</description><pubDate>Mon, 06 Jan 2025 10:00:00 GMT</pubDate></item>";
    }
}
