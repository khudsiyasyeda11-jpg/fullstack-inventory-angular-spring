package com.portfolio.inventory.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.portfolio.inventory.error.AssetNotFoundException;
import com.portfolio.inventory.error.BusinessRuleException;
import com.portfolio.inventory.error.DuplicateAssetTagException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AssetController.class)
class AssetControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AssetService service;

    static final String VALID_JSON = """
            {"assetTag":"TOL-3001","name":"Cordless drill","category":"TOOL","status":"IN_STOCK",
             "location":"Depot South","purchaseDate":"2025-03-01","purchaseCost":189.50}
            """;

    static AssetResponse response(long id) {
        Instant now = Instant.parse("2025-03-01T10:00:00Z");
        return new AssetResponse(id, "TOL-3001", "Cordless drill", AssetCategory.TOOL, AssetStatus.IN_STOCK,
                "Depot South", null, LocalDate.of(2025, 3, 1), new BigDecimal("189.50"), null, now, now);
    }

    @Test
    void listReturnsPageShapeAndAppliesDefaults() throws Exception {
        when(service.list(any(), any())).thenReturn(new PageImpl<>(List.of(response(1)), PageRequest.of(0, 20), 1));

        mvc.perform(get("/api/assets").param("q", "drill").param("status", "IN_STOCK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].assetTag").value("TOL-3001"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0));

        ArgumentCaptor<AssetFilter> filter = ArgumentCaptor.forClass(AssetFilter.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(service).list(filter.capture(), pageable.capture());
        assertThat(filter.getValue()).isEqualTo(new AssetFilter("drill", null, AssetStatus.IN_STOCK));
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageable.getValue().getSort().getOrderFor("assetTag")).isNotNull();
    }

    @Test
    void listCapsPageSizeAndHonoursSortParam() throws Exception {
        when(service.list(any(), any())).thenReturn(new PageImpl<>(List.of()));

        mvc.perform(get("/api/assets").param("size", "500").param("sort", "name,desc")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(service).list(any(), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
        assertThat(pageable.getValue().getSort().getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void invalidEnumFilterIsBadRequest() throws Exception {
        mvc.perform(get("/api/assets").param("status", "BROKEN"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void missingAssetIsProblemDetail404() throws Exception {
        when(service.get(99L)).thenThrow(new AssetNotFoundException(99L));

        mvc.perform(get("/api/assets/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Asset not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Asset 99 does not exist"));
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        when(service.create(any())).thenReturn(response(7));

        mvc.perform(post("/api/assets").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/assets/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.purchaseDate").value("2025-03-01"));
    }

    @Test
    void createWithInvalidFieldsReturnsFieldErrors() throws Exception {
        String invalid = """
                {"assetTag":"drill-1","name":"","category":"TOOL","location":"Depot","purchaseCost":-5}
                """;

        mvc.perform(post("/api/assets").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.assetTag").value("must look like ABC-1234"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.status").exists())
                .andExpect(jsonPath("$.errors.purchaseCost").exists());
        verify(service, never()).create(any());
    }

    @Test
    void malformedJsonIsBadRequest() throws Exception {
        mvc.perform(post("/api/assets").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateTagIsConflict() throws Exception {
        when(service.create(any())).thenThrow(new DuplicateAssetTagException("TOL-3001"));

        mvc.perform(post("/api/assets").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.assetTag").value("already in use"));
    }

    @Test
    void businessRuleViolationIs422() throws Exception {
        when(service.update(eq(3L), any())).thenThrow(new BusinessRuleException("assignedTo", "is required when status is ASSIGNED"));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/assets/3")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.assignedTo").value("is required when status is ASSIGNED"));
    }

    @Test
    void deleteReturns204AndMissingReturns404() throws Exception {
        mvc.perform(delete("/api/assets/1")).andExpect(status().isNoContent());
        verify(service).delete(1L);

        doThrow(new AssetNotFoundException(2L)).when(service).delete(2L);
        mvc.perform(delete("/api/assets/2")).andExpect(status().isNotFound());
    }
}
