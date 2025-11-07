package com.training.customer_service.repositories;

import com.training.customer_service.entities.Customer;
import com.training.customer_service.enums.CustomerStatus;
import org.springframework.data.jpa.domain.Specification;

// Best practice: Also make the class 'final'
public final class CustomerSpecification {

    // --- SONARQUBE FIX ---
    // Add a private constructor to hide the implicit public one
    // and prevent instantiation.
    private CustomerSpecification() {
        // This constructor is private and intentionally empty
    }

    public static Specification<Customer> isAnything() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
    }

    public static Specification<Customer> hasName(String name) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Customer> hasAddress(String address) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get("address")), "%" + address.toLowerCase() + "%");
    }

    public static Specification<Customer> hasNeighborhood(String neighborhood) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("neighborhood"), neighborhood);
    }

    public static Specification<Customer> hasStatus(CustomerStatus status) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), status);
    }
}