package com.portfolio.inventory.asset;

import com.portfolio.inventory.error.AssetNotFoundException;
import com.portfolio.inventory.error.BusinessRuleException;
import com.portfolio.inventory.error.DuplicateAssetTagException;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AssetService {

    private final AssetRepository repository;

    public AssetService(AssetRepository repository) {
        this.repository = repository;
    }

    public Page<AssetResponse> list(AssetFilter filter, Pageable pageable) {
        return repository.findAll(AssetSpecifications.matching(filter), pageable).map(AssetResponse::from);
    }

    public AssetResponse get(Long id) {
        return AssetResponse.from(find(id));
    }

    @Transactional
    public AssetResponse create(AssetRequest request) {
        validateRules(request);
        if (repository.existsByAssetTag(request.assetTag())) {
            throw new DuplicateAssetTagException(request.assetTag());
        }
        Asset asset = new Asset(request.assetTag(), request.name(), request.category(), request.status(),
                request.location());
        apply(asset, request);
        return AssetResponse.from(repository.save(asset));
    }

    @Transactional
    public AssetResponse update(Long id, AssetRequest request) {
        Asset asset = find(id);
        validateRules(request);
        if (repository.existsByAssetTagAndIdNot(request.assetTag(), id)) {
            throw new DuplicateAssetTagException(request.assetTag());
        }
        apply(asset, request);
        // saveAndFlush so updatedAt (@PreUpdate) is populated in the response
        return AssetResponse.from(repository.saveAndFlush(asset));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    /** Count of assets per status, including zero counts for unused statuses. */
    public Map<AssetStatus, Long> summary() {
        Map<AssetStatus, Long> counts = new EnumMap<>(AssetStatus.class);
        for (AssetStatus s : AssetStatus.values()) {
            counts.put(s, 0L);
        }
        repository.countGroupedByStatus().forEach(c -> counts.put(c.getStatus(), c.getTotal()));
        return counts;
    }

    private Asset find(Long id) {
        return repository.findById(id).orElseThrow(() -> new AssetNotFoundException(id));
    }

    private static void validateRules(AssetRequest request) {
        boolean hasHolder = request.assignedTo() != null && !request.assignedTo().isBlank();
        if (request.status() == AssetStatus.ASSIGNED && !hasHolder) {
            throw new BusinessRuleException("assignedTo", "is required when status is ASSIGNED");
        }
        if (request.status() == AssetStatus.RETIRED && hasHolder) {
            throw new BusinessRuleException("assignedTo", "must be empty when status is RETIRED");
        }
    }

    private static void apply(Asset asset, AssetRequest r) {
        asset.setAssetTag(r.assetTag());
        asset.setName(r.name().trim());
        asset.setCategory(r.category());
        asset.setStatus(r.status());
        asset.setLocation(r.location().trim());
        asset.setAssignedTo(blankToNull(r.assignedTo()));
        asset.setPurchaseDate(r.purchaseDate());
        asset.setPurchaseCost(r.purchaseCost());
        asset.setNotes(blankToNull(r.notes()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
