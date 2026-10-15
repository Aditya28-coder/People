package com.kloudspot.employees.department;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findAllByOrderByNameAsc();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Department> findByNameKey(String nameKey);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Department d where d.id = :id")
    Optional<Department> findLockedById(Long id);
}
