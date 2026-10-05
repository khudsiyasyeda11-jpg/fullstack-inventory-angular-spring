package com.portfolio.inventory.asset;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Create/update payload. Cross-field rules (e.g. ASSIGNED needs a holder) live in {@link AssetService}. */
public record AssetRequest(
        @NotBlank @Pattern(regexp = "^[A-Z]{3}-\\d{4,6}$", message = "must look like ABC-1234") String assetTag,
        @NotBlank @Size(max = 120) String name,
        @NotNull AssetCategory category,
        @NotNull AssetStatus status,
        @NotBlank @Size(max = 120) String location,
        @Size(max = 120) String assignedTo,
        @PastOrPresent LocalDate purchaseDate,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal purchaseCost,
        @Size(max = 1000) String notes) {
}
