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
    public static final int STOP_DELAY_SEC = 2;

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
                exchange.sendResponseHeaders(HttpStatus.OK, message.getBytes().length);
                exchange.getResponseBody().write(message.getBytes());
                exchange.close();
            };

            HttpHandler stopHandler = exchange -> {
                String message = "Server Stopping";
                exchange.sendResponseHeaders(HttpStatus.OK, message.getBytes().length);
                exchange.getResponseBody().write(message.getBytes());
                exchange.close();
                // Find out how to delay closing this application and server
                server.stop(STOP_DELAY_SEC);
                Logger.log("Server stopping");
            };

            HttpHandler mediaHandler = new MediaHandler();

            List<HttpContext> contexts = new ArrayList<>();
            HttpContext rootContext = server.createContext("/");
            rootContext.setHandler(rootHandler);
            contexts.add(rootContext);
            contexts.add(server.createContext("/exit", stopHandler));
            contexts.add(server.createContext("/media", mediaHandler));

            List<Filter> filters = getFilters();
            contexts.forEach(context -> context.getFilters().addAll(filters));

            Logger.log("Server Started");
        } catch (IOException e) {
            System.exit(1);
        }
    }

    private static List<Filter> getFilters() {
        Filter logFilter = new Filter() {

            @Override
            public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
                // work before request handling
                Logger.log("Request URI: " + exchange.getRequestURI());
                Logger.log("Request method: " + exchange.getRequestMethod());
                Logger.log("Request body: " + exchange.getRequestBody());
                Logger.log("Request headers: " + exchange.getRequestHeaders());
//                Logger.log("Request protocol: " + exchange.getProtocol());
//                Logger.log("Request local address: " + exchange.getLocalAddress());
//                Logger.log("Request principal: " + exchange.getPrincipal());
//                Logger.log("Request remote address: " + exchange.getRemoteAddress());
                // processing request filter chain
                chain.doFilter(exchange);
                // work after request handling
                Logger.log("Processing request finished");
                Logger.log("Request Response Code: " + exchange.getResponseCode());
            }

            @Override
            public String description() {
                return "Logging filter to log requests";
            }
        };

        Filter beforeFilter = Filter.beforeHandler("Before Filter", (exchange) -> {
//            Logger.log("Recording time for request");
            exchange.setAttribute("time", System.currentTimeMillis());
        });

        Filter afterFilter = Filter.afterHandler("After Filter", (exchange) -> {
            long startTime = (long) exchange.getAttribute("time");
            long timeTaken = System.currentTimeMillis() - startTime;
            Logger.log("Time taken: " + timeTaken + "ms");
        });

        return List.of(beforeFilter, afterFilter, logFilter);
    }
}
