package com.portfolio.inventory.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.portfolio.inventory.error.AssetNotFoundException;
import com.portfolio.inventory.error.BusinessRuleException;
import com.portfolio.inventory.error.DuplicateAssetTagException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    AssetRepository repository;

    @InjectMocks
    AssetService service;

    static AssetRequest request(String tag, AssetStatus status, String assignedTo) {
        return new AssetRequest(tag, " Cordless drill ", AssetCategory.TOOL, status, "Depot South", assignedTo,
                LocalDate.of(2025, 3, 1), new BigDecimal("189.50"), "  ");
    }

    static Asset persisted(long id, String tag) {
        Asset asset = new Asset(tag, "Cordless drill", AssetCategory.TOOL, AssetStatus.IN_STOCK, "Depot South");
        ReflectionTestUtils.setField(asset, "id", id);
        return asset;
    }

    @Test
    void createTrimsInputAndSaves() {
        when(repository.existsByAssetTag("TOL-3001")).thenReturn(false);
        when(repository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

        AssetResponse response = service.create(request("TOL-3001", AssetStatus.IN_STOCK, null));

        ArgumentCaptor<Asset> saved = ArgumentCaptor.forClass(Asset.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Cordless drill");
        assertThat(saved.getValue().getNotes()).as("blank notes stored as null").isNull();
        assertThat(response.assetTag()).isEqualTo("TOL-3001");
    }

    @Test
    void createRejectsDuplicateTag() {
        when(repository.existsByAssetTag("TOL-3001")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("TOL-3001", AssetStatus.IN_STOCK, null)))
                .isInstanceOf(DuplicateAssetTagException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void assignedStatusRequiresHolder() {
        assertThatThrownBy(() -> service.create(request("TOL-3001", AssetStatus.ASSIGNED, " ")))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("field").isEqualTo("assignedTo");
    }

    @Test
    void retiredAssetCannotHaveHolder() {
        assertThatThrownBy(() -> service.create(request("TOL-3001", AssetStatus.RETIRED, "Crew 9")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void updateAppliesChangesToExistingAsset() {
        Asset existing = persisted(5L, "TOL-3001");
        when(repository.findById(5L)).thenReturn(Optional.of(existing));
        when(repository.existsByAssetTagAndIdNot("TOL-3001", 5L)).thenReturn(false);
        when(repository.saveAndFlush(existing)).thenReturn(existing);

        AssetResponse response = service.update(5L, request("TOL-3001", AssetStatus.ASSIGNED, "Crew 4"));

        assertThat(response.status()).isEqualTo(AssetStatus.ASSIGNED);
        assertThat(response.assignedTo()).isEqualTo("Crew 4");
        assertThat(response.purchaseCost()).isEqualByComparingTo("189.50");
    }

    @Test
    void updateRejectsTagOwnedByAnotherAsset() {
        when(repository.findById(5L)).thenReturn(Optional.of(persisted(5L, "TOL-3001")));
        when(repository.existsByAssetTagAndIdNot("TOL-9999", 5L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(5L, request("TOL-9999", AssetStatus.IN_STOCK, null)))
                .isInstanceOf(DuplicateAssetTagException.class);
    }

    @Test
    void getAndDeleteThrowWhenMissing() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(42L)).isInstanceOf(AssetNotFoundException.class)
                .hasMessageContaining("42");
        assertThatThrownBy(() -> service.delete(42L)).isInstanceOf(AssetNotFoundException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void summaryIncludesZeroCountsForEveryStatus() {
        AssetRepository.StatusCount assigned = new AssetRepository.StatusCount() {
            public AssetStatus getStatus() { return AssetStatus.ASSIGNED; }
            public long getTotal() { return 3; }
        };
        when(repository.countGroupedByStatus()).thenReturn(List.of(assigned));

        Map<AssetStatus, Long> summary = service.summary();

        assertThat(summary).containsEntry(AssetStatus.ASSIGNED, 3L)
                .containsEntry(AssetStatus.IN_STOCK, 0L)
                .hasSize(AssetStatus.values().length);
    }
}
