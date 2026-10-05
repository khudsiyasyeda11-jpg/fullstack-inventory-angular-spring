package com.portfolio.inventory.asset;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface AssetRepository extends JpaRepository<Asset, Long>, JpaSpecificationExecutor<Asset> {

    boolean existsByAssetTag(String assetTag);

    boolean existsByAssetTagAndIdNot(String assetTag, Long id);

    @Query("select a.status as status, count(a) as total from Asset a group by a.status")
    List<StatusCount> countGroupedByStatus();

    /** Interface projection for the status summary query. */
    interface StatusCount {
        AssetStatus getStatus();

        long getTotal();
    }
}
