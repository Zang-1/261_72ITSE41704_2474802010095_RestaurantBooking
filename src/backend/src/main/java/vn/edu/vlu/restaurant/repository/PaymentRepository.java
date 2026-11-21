package vn.edu.vlu.restaurant.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.vlu.restaurant.domain.Payment;
import vn.edu.vlu.restaurant.domain.PaymentMethod;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** Money received per method in a shift, without the change given back (FR-28). */
    @Query("select p.method as method, sum(p.amount - p.changeAmount) as amount, count(p) as payments "
            + "from Payment p where p.shift.id = :shiftId group by p.method")
    List<MethodTotal> totalsByMethod(@Param("shiftId") Long shiftId);

    interface MethodTotal {
        PaymentMethod getMethod();

        Long getAmount();

        Long getPayments();
    }
}
