package uk.bit1.resilience4jcircuitbreakerdemo.rest;

import org.springframework.data.jpa.repository.JpaRepository;

interface OrderRepository extends JpaRepository<Order, Long> {
}
