package vn.edu.vlu.restaurant.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.vlu.restaurant.domain.WaitlistEntry;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {

    /** Parties still waiting, in arrival order (BR-04). */
    List<WaitlistEntry> findBySeatedTableIsNullOrderByArrivedAtAscIdAsc();
}
