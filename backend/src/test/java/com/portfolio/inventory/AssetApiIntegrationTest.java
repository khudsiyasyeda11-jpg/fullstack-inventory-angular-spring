package com.portfolio.inventory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Full stack (controller -> service -> JPA -> Flyway-managed H2) with the synthetic demo data loaded. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AssetApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void demoDataIsQueryableWithFiltersSortingAndSummary() throws Exception {
        mvc.perform(get("/api/assets/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ASSIGNED").value(10))
                .andExpect(jsonPath("$.IN_STOCK").value(8))
                .andExpect(jsonPath("$.IN_REPAIR").value(4))
                .andExpect(jsonPath("$.RETIRED").value(2));

        mvc.perform(get("/api/assets").param("category", "VEHICLE").param("sort", "purchaseCost,desc").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].assetTag").value("VEH-5003"));

        mvc.perform(get("/api/assets").param("q", "crew 4"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void unknownSortPropertyIsBadRequest() throws Exception {
        mvc.perform(get("/api/assets").param("sort", "doesNotExist,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid sort property"));
    }

    @Test
    void createReadUpdateDeleteLifecycle() throws Exception {
        String body = mvc.perform(post("/api/assets").contentType(MediaType.APPLICATION_JSON).content("""
                        {"assetTag":"GEN-9001","name":"Generator 5kW","category":"OTHER","status":"IN_STOCK",
                         "location":"Depot West","purchaseCost":1999.99}
                        """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(body, "$.id");

        mvc.perform(get("/api/assets/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Generator 5kW"))
                .andExpect(jsonPath("$.createdAt").exists());

        mvc.perform(post("/api/assets").contentType(MediaType.APPLICATION_JSON).content("""
                        {"assetTag":"GEN-9001","name":"Copy","category":"OTHER","status":"IN_STOCK","location":"X"}
                        """))
                .andExpect(status().isConflict());

        mvc.perform(put("/api/assets/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"assetTag":"GEN-9001","name":"Generator 5kW","category":"OTHER","status":"ASSIGNED",
                         "location":"Depot West","assignedTo":"Crew 9","purchaseCost":1999.99}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.assignedTo").value("Crew 9"));

        mvc.perform(delete("/api/assets/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/assets/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void openApiDocumentIsPublished() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Field Asset Tracker API"))
                .andExpect(jsonPath("$.paths['/api/assets']").exists());
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
}
