package com.criteo.benchmarks;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.HdrHistogram.ConcurrentHistogram;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

public class ServerBenchmark {
    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        server.setHandler(new BenchmarkHandler());
        server.start();
        server.join();
    }

    private static class BenchmarkHandler extends AbstractHandler {

        private final ConcurrentHistogram hdr = new ConcurrentHistogram(3);
        private final byte[] buffer = new byte[10*1024*1024];
        private final Random rnd = new Random(System.currentTimeMillis());

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            if (baseRequest.getPathInfo().equals("/stats")) {
                baseRequest.setHandled(true);
                response.setStatus(200);
                PrintWriter writer = response.getWriter();
                writer.println("min   : "+ hdr.getMinValue());
                writer.println("10%ile: "+ hdr.getValueAtPercentile(0.1));
                writer.println("20%ile: "+ hdr.getValueAtPercentile(0.2));
                writer.println("30%ile: "+ hdr.getValueAtPercentile(0.3));
                writer.println("40%ile: "+ hdr.getValueAtPercentile(0.4));
                writer.println("50%ile: "+ hdr.getValueAtPercentile(0.5));
                writer.println("60%ile: "+ hdr.getValueAtPercentile(0.6));
                writer.println("70%ile: "+ hdr.getValueAtPercentile(0.7));
                writer.println("80%ile: "+ hdr.getValueAtPercentile(0.8));
                writer.println("90%ile: "+ hdr.getValueAtPercentile(0.9));
                writer.println("95%ile: "+ hdr.getValueAtPercentile(0.95));
                writer.println("99%ile: "+ hdr.getValueAtPercentile(0.99));
                writer.println("max   : "+ hdr.getMaxValue());

                return;
            }
            long start = System.nanoTime();
            JsonParser parser = new JsonParser();
            String value = null;
            try (BufferedReader reader = baseRequest.getReader()) {
                JsonElement element = parser.parse(reader);
                if (element != null && element.isJsonObject()) {
                    JsonObject root = element.getAsJsonObject();
                    JsonElement jsonValue = root.get("Value");
                    value = jsonValue.isJsonNull() ? "0" : jsonValue.getAsString();
                }
            }
            int sum = 0;
            for (int i = 0; i < 100_000; i++) {
                sum += buffer[rnd.nextInt(buffer.length)];
            }
            response.setStatus(200);
            baseRequest.setHandled(true);
            PrintWriter writer = response.getWriter();
            writer.println(sum);
            writer.println(value);
            long stop = System.nanoTime();
            long delta = stop-start;
            hdr.recordValue(delta);
            writer.println("Time: " + delta + "ns");
        }
    }
}
