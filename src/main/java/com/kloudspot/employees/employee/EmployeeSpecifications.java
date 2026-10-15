package com.kloudspot.employees.employee;

import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class EmployeeSpecifications {
    private EmployeeSpecifications() {}

    public static Specification<Employee> matching(String search, String department, EmploymentStatus status) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (search != null && !search.isBlank()) {
                String escaped = search.trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                String pattern = "%" + escaped + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("fullName")), pattern, '\\'),
                    cb.like(cb.lower(root.get("email")), pattern, '\\')));
            }
            if (department != null && !department.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("department")), department.trim().toLowerCase(Locale.ROOT)));
            }
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
