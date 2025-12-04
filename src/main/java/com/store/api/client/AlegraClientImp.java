package com.store.api.client;

import com.store.api.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode; // Importante para Spring Boot 3+
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class AlegraClientImp implements AlegraClient {

    private final WebClient webClient;

    public AlegraClientImp(WebClient.Builder webClientBuilder,
                           @Value("${alegra.base-url}") String baseUrl,
                           @Value("${alegra.email}") String email,
                           @Value("${alegra.token}") String token) {

        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeaders(headers -> headers.setBasicAuth(email, token))
                .build();
    }

    @Override
    public List<Map<String, Object>> getProducts() {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/items")
                        .queryParam("type", "product")
                        .build())
                .retrieve()
                // Corrección del error de Lambda: Usamos Mono.<Throwable>error
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.<Throwable>error(new IllegalStateException(
                                        "Error 4xx from Alegra: " + body))))
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.<Throwable>error(new IllegalStateException(
                                        "Error 5xx from Alegra: " + body))))
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .block();
    }

    @Override
    public Map<String, Object> getProductById(String productId) {
        return webClient.get()
                .uri("/items/{id}", productId)
                .retrieve()
                .onStatus(status -> status.value() == 404, response ->
                        Mono.error(new ResourceNotFoundException("Product not found in Alegra")))
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.<Throwable>error(new IllegalStateException(
                                        "Error 4xx from Alegra: " + body))))
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.<Throwable>error(new IllegalStateException(
                                        "Error 5xx from Alegra: " + body))))
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
    }
}