package com.empresa.clientes;

import com.empresa.clientes.dto.ClienteRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private ClienteRequest crearRequest(String documento, String nombre, String apellido, String email, String telefono, String direccion) {
        ClienteRequest req = new ClienteRequest();
        req.setDocumento(documento);
        req.setNombre(nombre);
        req.setApellido(apellido);
        req.setEmail(email);
        req.setTelefono(telefono);
        req.setDireccion(direccion);
        return req;
    }

    @Test
    public void P01_registrar_con_datos_correctos() throws Exception {
        ClienteRequest req = crearRequest("12345678", "Juan", "Pérez", "juan.perez@test.com", "3001234567", "Calle 123");
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.documento").value("12345678"))
                .andExpect(jsonPath("$.email").value("juan.perez@test.com"));
    }

    @Test
    public void P02_registrar_sin_campos_opcionales() throws Exception {
        ClienteRequest req = crearRequest("11111111", "Ana", "Lopez", "ana.lopez@test.com", null, null);
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    public void P03_registrar_sin_nombre() throws Exception {
        ClienteRequest req = crearRequest("22222222", null, "Ramirez", "maria.ramirez@test.com", null, null);
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.mensaje").value("El campo nombre es obligatorio"));
    }

    @Test
    public void P04_registrar_con_email_mal_escrito() throws Exception {
        ClienteRequest req = crearRequest("33333333", "Carlos", "Gomez", "correo-malo", null, null);
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400));
    }

    @Test
    public void P05_documento_repetido() throws Exception {
        ClienteRequest req1 = crearRequest("44444444", "Luis", "Torres", "luis@test.com", null, null);
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        ClienteRequest req2 = crearRequest("44444444", "Pedro", "Mendez", "pedro@test.com", null, null);
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.mensaje").value("Ya existe un cliente con ese documento"));
    }

    @Test
    public void P06_email_repetido() throws Exception {
        ClienteRequest req1 = crearRequest("55555555", "Sofia", "Diaz", "sofia@test.com", null, null);
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        ClienteRequest req2 = crearRequest("66666666", "Laura", "Ruiz", "sofia@test.com", null, null);
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.mensaje").value("Ya existe un cliente con ese email"));
    }

    @Test
    public void P07_consultar_cliente_existente() throws Exception {
        ClienteRequest req = crearRequest("77777777", "Diego", "Castro", "diego@test.com", null, null);
        String resp = mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // extraer id simple
        com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(resp);
        Long id = node.get("id").asLong();

        mockMvc.perform(get("/api/clientes/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documento").value("77777777"));
    }

    @Test
    public void P08_consultar_cliente_inexistente() throws Exception {
        mockMvc.perform(get("/api/clientes/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.mensaje").value("No existe un cliente con id 9999"));
    }

    @Test
    public void P09_listar_con_varios_clientes() throws Exception {
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearRequest("88888881", "A", "B", "a1@test.com", null, null))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crearRequest("88888882", "C", "D", "a2@test.com", null, null))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    public void P10_listar_sin_clientes() throws Exception {
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
