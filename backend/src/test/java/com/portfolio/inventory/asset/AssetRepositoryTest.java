package com.portfolio.inventory.asset;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** Runs the real Flyway migrations against H2 and exercises the JPA mapping and dynamic queries. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // use the PostgreSQL-mode H2 from application.yml
class AssetRepositoryTest {

    @Autowired
    AssetRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll(); // start from a known state rather than the demo rows
        save("LAP-0001", "Rugged laptop", AssetCategory.LAPTOP, AssetStatus.ASSIGNED, "Depot North", "Crew 1", "1450.00");
        save("LAP-0002", "Ultralight laptop", AssetCategory.LAPTOP, AssetStatus.IN_STOCK, "Regional Office", null, "1180.00");
        save("VEH-0001", "Service van", AssetCategory.VEHICLE, AssetStatus.IN_REPAIR, "Fleet Garage", null, "38500.00");
        save("TOL-0001", "Thermal camera", AssetCategory.TOOL, AssetStatus.ASSIGNED, "Depot East", "Crew 2", "1899.00");
    }

    private void save(String tag, String name, AssetCategory c, AssetStatus s, String location, String holder, String cost) {
        Asset a = new Asset(tag, name, c, s, location);
        a.setAssignedTo(holder);
        a.setPurchaseCost(new BigDecimal(cost));
        repository.save(a);
    }

    private Page<Asset> find(AssetFilter filter, PageRequest page) {
        return repository.findAll(AssetSpecifications.matching(filter), page);
    }

    @Test
    void persistsWithGeneratedIdAndTimestamps() {
        Asset saved = repository.saveAndFlush(new Asset("FUR-0001", "Desk", AssetCategory.FURNITURE, AssetStatus.IN_STOCK, "Office"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(repository.existsByAssetTag("FUR-0001")).isTrue();
        assertThat(repository.existsByAssetTagAndIdNot("FUR-0001", saved.getId())).isFalse();
    }

    @Test
    void freeTextSearchIsCaseInsensitiveAcrossColumns() {
        assertThat(find(new AssetFilter("LAPTOP", null, null), PageRequest.of(0, 10)).getTotalElements()).isEqualTo(2);
        assertThat(find(new AssetFilter("crew 2", null, null), PageRequest.of(0, 10)).getContent())
                .extracting(Asset::getAssetTag).containsExactly("TOL-0001");
        assertThat(find(new AssetFilter("garage", null, null), PageRequest.of(0, 10)).getContent())
                .extracting(Asset::getAssetTag).containsExactly("VEH-0001");
    }

    @Test
    void combinesCategoryAndStatusFilters() {
        Page<Asset> page = find(new AssetFilter(null, AssetCategory.LAPTOP, AssetStatus.ASSIGNED), PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Asset::getAssetTag).containsExactly("LAP-0001");
    }

    @Test
    void pagesAndSorts() {
        Page<Asset> first = find(AssetFilter.none(), PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "purchaseCost")));

        assertThat(first.getTotalElements()).isEqualTo(4);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(first.getContent()).extracting(Asset::getAssetTag).containsExactly("VEH-0001", "TOL-0001", "LAP-0001");
    }

    @Test
    void countsGroupedByStatus() {
        List<AssetRepository.StatusCount> counts = repository.countGroupedByStatus();

        assertThat(counts).extracting(AssetRepository.StatusCount::getStatus, AssetRepository.StatusCount::getTotal)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(AssetStatus.ASSIGNED, 2L),
                        org.assertj.core.groups.Tuple.tuple(AssetStatus.IN_STOCK, 1L),
                        org.assertj.core.groups.Tuple.tuple(AssetStatus.IN_REPAIR, 1L));
    }
}
