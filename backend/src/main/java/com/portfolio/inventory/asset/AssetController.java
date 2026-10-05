package com.portfolio.inventory.asset;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/assets")
@Tag(name = "Assets", description = "Track field equipment")
public class AssetController {

    private final AssetService service;

    public AssetController(AssetService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List assets", description = "Paged, sortable and filterable. Example: ?q=van&status=IN_REPAIR&sort=name,asc")
    public PageResponse<AssetResponse> list(
            @Parameter(description = "Free-text match on tag, name, location or holder") @RequestParam(required = false) String q,
            @RequestParam(required = false) AssetCategory category,
            @RequestParam(required = false) AssetStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "assetTag", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.of(service.list(new AssetFilter(q, category, status), pageable));
    }

    @GetMapping("/summary")
    @Operation(summary = "Asset counts per status")
    public Map<AssetStatus, Long> summary() {
        return service.summary();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one asset")
    public AssetResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @Operation(summary = "Create an asset")
    public ResponseEntity<AssetResponse> create(@Valid @RequestBody AssetRequest request) {
        AssetResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace an asset")
    public AssetResponse update(@PathVariable Long id, @Valid @RequestBody AssetRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an asset")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
