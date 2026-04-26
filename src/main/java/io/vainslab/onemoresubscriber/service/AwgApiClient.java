package io.vainslab.onemoresubscriber.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vainslab.onemoresubscriber.config.BotProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@Slf4j
public class AwgApiClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient;
    private final BotProperties.AwgApi config;

    public AwgApiClient(BotProperties botProperties) {
        this.config = botProperties.getAwgApi();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public boolean isConfigured() {
        return config.getUrl() != null && !config.getUrl().isBlank()
                && config.getToken() != null && !config.getToken().isBlank();
    }

    public CreatePeerResult createPeer() {
        HttpRequest request = newRequest("/peers")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 201) {
                log.error("AWG API create peer failed: {} {}", response.statusCode(), response.body());
                return null;
            }
            JsonNode json = MAPPER.readTree(response.body());
            return new CreatePeerResult(
                    json.get("client_ip").asText(),
                    json.get("vpn_uri").asText()
            );
        } catch (Exception e) {
            log.error("AWG API create peer error", e);
            return null;
        }
    }

    public boolean deletePeer(String clientIp) {
        HttpRequest request = newRequest("/peers/" + clientIp)
                .DELETE()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return true;
            }
            log.error("AWG API delete peer failed: {} {}", response.statusCode(), response.body());
            return false;
        } catch (Exception e) {
            log.error("AWG API delete peer error", e);
            return false;
        }
    }

    private HttpRequest.Builder newRequest(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(config.getUrl() + path))
                .header("Authorization", "Bearer " + config.getToken())
                .timeout(Duration.ofSeconds(15));
    }

    public record CreatePeerResult(String clientIp, String vpnUri) {}
}
