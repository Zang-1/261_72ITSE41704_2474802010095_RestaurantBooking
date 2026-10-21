package vn.edu.vlu.restaurant.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.vlu.restaurant.domain.DiningTable;
import vn.edu.vlu.restaurant.domain.Role;
import vn.edu.vlu.restaurant.domain.TableSection;
import vn.edu.vlu.restaurant.domain.User;
import vn.edu.vlu.restaurant.repository.DiningTableRepository;
import vn.edu.vlu.restaurant.repository.TableSectionRepository;
import vn.edu.vlu.restaurant.repository.UserRepository;

/**
 * Creates the staff accounts and the dining-room layout on an empty database. Staff accounts are
 * seeded, not managed in the UI (Proposal, Section 1.3). The sample passwords are listed in the README.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final TableSectionRepository sectionRepository;
    private final DiningTableRepository tableRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, TableSectionRepository sectionRepository,
                      DiningTableRepository tableRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.sectionRepository = sectionRepository;
        this.tableRepository = tableRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() == 0) {
            seedUsers();
        }
        if (sectionRepository.count() == 0) {
            seedTables();
        }
    }

    private void seedUsers() {
        userRepository.save(new User("manager", passwordEncoder.encode("Manager@2026"), Role.MANAGER));
        userRepository.save(new User("waiter", passwordEncoder.encode("Waiter@2026"), Role.WAITER_HOST));
        userRepository.save(new User("kitchen", passwordEncoder.encode("Kitchen@2026"), Role.KITCHEN_STAFF));
        log.info("Seeded 3 staff accounts");
    }

    /**
     * Layout of the restaurant interviewed on 18/10/2026 (docs/interview, finding F8): four areas and
     * 27 tables, numbered area by area. Seats per table were not recorded, so the capacities are a team assumption.
     */
    private void seedTables() {
        int number = 1;
        number = seedSection("Sweet Zone", new int[] {2, 2, 4, 4, 4, 4, 6, 6}, number);      // tables 1-8
        number = seedSection("Love to Love", new int[] {2, 2, 2, 2, 2, 2, 4, 4}, number);    // tables 9-16
        number = seedSection("VIP", new int[] {8, 8, 10, 10, 12}, number);                   // tables 17-21
        number = seedSection("Mây (Rooftop)", new int[] {2, 2, 4, 4, 6, 6}, number);         // tables 22-27
        log.info("Seeded 4 sections and {} tables", number - 1);
    }

    /** Saves one section and its tables; returns the next free table number. */
    private int seedSection(String name, int[] capacities, int firstNumber) {
        TableSection section = sectionRepository.save(new TableSection(name));
        int number = firstNumber;
        for (int capacity : capacities) {
            tableRepository.save(new DiningTable(number++, capacity, section));
        }
        return number;
    }
}
