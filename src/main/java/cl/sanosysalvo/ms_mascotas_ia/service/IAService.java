package cl.sanosysalvo.ms_mascotas_ia.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Base64;

@Service
public class IAService {

    @Value("${gemini.api.key}")
    private String apiKey;

    // URL oficial de Google Gemini (Versión 3.5 Flash, la más actual)
private final String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=";

    public String analizarImagen(byte[] imagenBytes) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            // 1. Transformamos la imagen a texto (Base64) para meterla en el JSON
            String base64Imagen = Base64.getEncoder().encodeToString(imagenBytes);

            // 2. Armamos la petición exacta como le gusta a Google
            String jsonBody = "{"
                    + "\"contents\": [{"
                    + "  \"parts\": ["
                    + "    {\"text\": \"Analiza esta foto de una mascota y devuelve solo etiquetas descriptivas de sus características o raza separadas por comas (Ejemplo: Perro, Golden Retriever, Cachorro, Juguetón). No agregues saludos ni texto extra, solo las etiquetas.\"},"
                    + "    {"
                    + "      \"inlineData\": {"
                    + "        \"mimeType\": \"image/jpeg\","
                    + "        \"data\": \"" + base64Imagen + "\""
                    + "      }"
                    + "    }"
                    + "  ]"
                    + "}]"
                    + "}";

            // 3. Preparamos las cabeceras
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            // 4. Disparamos el misil a los servidores de Google
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl + apiKey,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            // 5. Filtramos toda la respuesta gigante para sacar solo el texto que nos sirve
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            String resultadoIA = root.path("candidates").get(0)
                                     .path("content").path("parts").get(0)
                                     .path("text").asText();

            return resultadoIA.trim();

        } catch (Exception e) {
            throw new RuntimeException("Error al consultar Google Gemini: " + e.getMessage());
        }
    }
}