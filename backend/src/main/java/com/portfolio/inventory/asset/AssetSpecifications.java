package com.portfolio.inventory.asset;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Builds dynamic filters for the asset list endpoint. */
public final class AssetSpecifications {

    private AssetSpecifications() {
    }

    public static Specification<Asset> matching(AssetFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.q() != null && !filter.q().isBlank()) {
                String like = "%" + escape(filter.q().trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("assetTag")), like, '\\'),
                        cb.like(cb.lower(root.get("name")), like, '\\'),
                        cb.like(cb.lower(root.get("location")), like, '\\'),
                        cb.like(cb.lower(root.get("assignedTo")), like, '\\')));
            }
            if (filter.category() != null) {
                predicates.add(cb.equal(root.get("category"), filter.category()));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Escapes LIKE wildcards so user input is matched literally. */
    static String escape(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
