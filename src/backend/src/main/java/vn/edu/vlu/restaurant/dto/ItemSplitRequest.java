package vn.edu.vlu.restaurant.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Split a bill by item (FR-24): shares.get(i) holds the bill line ids that share i + 1 pays for. */
public record ItemSplitRequest(
        @NotNull(message = "Shares are required")
        @Size(min = 2, max = 20, message = "The number of shares must be between 2 and 20")
        List<@NotEmpty(message = "Every share needs at least one dish") List<@NotNull Long>> shares) {
}
