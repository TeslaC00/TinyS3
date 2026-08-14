package dev.teslac00;

import com.sun.net.httpserver.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static final int PORT = 5000;
    /**
     * The amount of request to store in memory before processing
     */
    public static final int BACKLOG = 10;

    static void main() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), BACKLOG);
            server.start();

            HttpHandler rootHandler = exchange -> {
                String message = "TinyS3 is Live";
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                exchange.sendResponseHeaders(200, message.getBytes().length);
                exchange.getResponseBody().write(message.getBytes());
                exchange.close();
            };

            HttpHandler stopHandler = exchange -> {
                String message = "Server Stopping";
                exchange.sendResponseHeaders(200, message.getBytes().length);
                exchange.getResponseBody().write(message.getBytes());
                exchange.close();
                // Find out how to delay closing this application and server
                server.stop(2);
                System.out.println("Server stopping");
            };

            HttpHandler mediaHandler = exchange -> {
                String message = "Media Endpoint not implemented yet";
                exchange.sendResponseHeaders(500, message.getBytes().length);
                exchange.getResponseBody().write(message.getBytes());
                exchange.close();
            };

            List<HttpContext> contexts = new ArrayList<>();
            HttpContext rootContext = server.createContext("/");
            rootContext.setHandler(rootHandler);
            contexts.add(rootContext);
            contexts.add(server.createContext("/exit", stopHandler));
            contexts.add(server.createContext("/media", mediaHandler));

            List<Filter> filters = getFilters();
            contexts.forEach(context -> context.getFilters().addAll(filters));

            System.out.println("Server Started");
        } catch (IOException e) {
            System.exit(1);
        }
    }

    private static List<Filter> getFilters() {
        Filter logFilter = new Filter() {

            @Override
            public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
                // work before request handling
                System.out.println("Request URI: " + exchange.getRequestURI());
                System.out.println("Request method: " + exchange.getRequestMethod());
                System.out.println("Request body: " + exchange.getRequestBody());
                System.out.println("Request headers: " + exchange.getRequestHeaders());
//                System.out.println("Request protocol: " + exchange.getProtocol());
//                System.out.println("Request local address: " + exchange.getLocalAddress());
//                System.out.println("Request principal: " + exchange.getPrincipal());
//                System.out.println("Request remote address: " + exchange.getRemoteAddress());
                // processing request filter chain
                chain.doFilter(exchange);
                // work after request handling
                System.out.println("Processing request finished");
                System.out.println("Request Response Code: " + exchange.getResponseCode());
            }

            @Override
            public String description() {
                return "Logging filter to log requests";
            }
        };

        Filter beforeFilter = Filter.beforeHandler("Before Filter", (exchange) -> {
//            System.out.println("Recording time for request");
            exchange.setAttribute("time", System.currentTimeMillis());
        });

        Filter afterFilter = Filter.afterHandler("After Filter", (exchange) -> {
            long startTime = (long) exchange.getAttribute("time");
            long timeTaken = System.currentTimeMillis() - startTime;
            System.out.println("Time taken: " + timeTaken + "ms");
        });

        return List.of(beforeFilter, afterFilter, logFilter);
    }
}
