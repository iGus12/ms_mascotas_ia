package cl.sanosysalvo.ms_mascotas_ia.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Service
public class IAService {

    @Value("${gemini.api.key}")
    private String apiKey;

    /*
     * Modelo estable que acepta texto e imágenes.
     */
   private static final String API_URL =
        "https://generativelanguage.googleapis.com/v1beta/"
                + "models/gemini-3.5-flash:generateContent";

    public String analizarImagen(byte[] imagenBytes) {

        if (imagenBytes == null || imagenBytes.length == 0) {
            throw new IllegalArgumentException(
                    "La imagen enviada está vacía."
            );
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "No se encontró la variable GEMINI_API_KEY."
            );
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            ObjectMapper mapper = new ObjectMapper();

            String imagenBase64 = Base64
                    .getEncoder()
                    .encodeToString(imagenBytes);

            /*
             * Parte 1: instrucción para Gemini.
             */
            ObjectNode texto = mapper.createObjectNode();

            texto.put(
                    "text",
                    "Analiza esta fotografía de una mascota. "
                            + "Devuelve solamente etiquetas descriptivas "
                            + "separadas por comas. Considera especie, raza "
                            + "aparente, color del pelaje, tamaño, manchas, "
                            + "orejas y características visibles. "
                            + "No agregues saludos, explicaciones ni texto extra."
            );

            /*
             * Parte 2: imagen en Base64.
             */
            ObjectNode datosImagen = mapper.createObjectNode();

            datosImagen.put("mime_type", "image/jpeg");
            datosImagen.put("data", imagenBase64);

            ObjectNode imagen = mapper.createObjectNode();
            imagen.set("inline_data", datosImagen);

            /*
             * Armamos el arreglo parts.
             */
            ArrayNode parts = mapper.createArrayNode();
            parts.add(texto);
            parts.add(imagen);

            ObjectNode contenido = mapper.createObjectNode();
            contenido.set("parts", parts);

            ArrayNode contents = mapper.createArrayNode();
            contents.add(contenido);

            ObjectNode cuerpoSolicitud = mapper.createObjectNode();
            cuerpoSolicitud.set("contents", contents);

            /*
             * La API key debe enviarse en x-goog-api-key.
             * No debe enviarse como Authorization Bearer.
             */
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", apiKey.trim());

            HttpEntity<String> solicitud = new HttpEntity<>(
                    mapper.writeValueAsString(cuerpoSolicitud),
                    headers
            );

            ResponseEntity<String> respuesta = restTemplate.exchange(
                    API_URL,
                    HttpMethod.POST,
                    solicitud,
                    String.class
            );

            if (respuesta.getBody() == null
                    || respuesta.getBody().isBlank()) {

                throw new RuntimeException(
                        "Gemini devolvió una respuesta vacía."
                );
            }

            JsonNode raiz = mapper.readTree(respuesta.getBody());
            JsonNode candidatos = raiz.path("candidates");

            if (!candidatos.isArray() || candidatos.isEmpty()) {
                throw new RuntimeException(
                        "Gemini no devolvió resultados. Respuesta: "
                                + respuesta.getBody()
                );
            }

            JsonNode partesRespuesta = candidatos
                    .get(0)
                    .path("content")
                    .path("parts");

            if (!partesRespuesta.isArray()
                    || partesRespuesta.isEmpty()) {

                throw new RuntimeException(
                        "Gemini no devolvió texto. Respuesta: "
                                + respuesta.getBody()
                );
            }

            String resultado = partesRespuesta
                    .get(0)
                    .path("text")
                    .asText();

            if (resultado == null || resultado.isBlank()) {
                throw new RuntimeException(
                        "No fue posible obtener el análisis de la imagen."
                );
            }

            return resultado.trim();

        } catch (HttpStatusCodeException errorGemini) {

            /*
             * Esto permitirá ver el error real:
             * clave inválida, modelo incorrecto, límite agotado, etc.
             */
            throw new RuntimeException(
                    "Gemini respondió con estado "
                            + errorGemini.getStatusCode()
                            + ": "
                            + errorGemini.getResponseBodyAsString(),
                    errorGemini
            );

        } catch (Exception error) {

            if (error instanceof RuntimeException) {
                throw (RuntimeException) error;
            }

            throw new RuntimeException(
                    "Error al consultar Google Gemini: "
                            + error.getMessage(),
                    error
            );
        }
    }
}