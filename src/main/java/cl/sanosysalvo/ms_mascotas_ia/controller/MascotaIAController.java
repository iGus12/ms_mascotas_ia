package cl.sanosysalvo.ms_mascotas_ia.controller;

import cl.sanosysalvo.ms_mascotas_ia.service.IAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ia")
@CrossOrigin(origins = "*")
public class MascotaIAController {

    @Autowired
    private IAService iaService;

    @PostMapping("/analizar")
    public ResponseEntity<?> analizarImagenMascota(@RequestParam("imagen") MultipartFile archivo) {
        
        if (archivo.isEmpty()) {
            return ResponseEntity.badRequest().body("Error: No se envió ninguna imagen.");
        }

        try {
            // 1. Obtenemos los bytes de la imagen
            byte[] imagenBytes = archivo.getBytes();

            // 2. Le pasamos los bytes al servicio de IA de Hugging Face
            String etiquetas = iaService.analizarImagen(imagenBytes);

            // 3. Devolvemos la respuesta con el texto actualizado
            return ResponseEntity.ok().body("La IA detectó las siguientes características: " + etiquetas);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al procesar la imagen con la IA: " + e.getMessage());
        }
    }
}