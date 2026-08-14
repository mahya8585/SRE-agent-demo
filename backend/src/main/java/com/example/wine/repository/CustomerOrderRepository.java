package com.example.wine.repository;

import com.example.wine.model.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
	@Query("select coalesce(sum(customerOrder.total), 0) from CustomerOrder customerOrder")
	double calculateTotalRevenue();

	List<CustomerOrder> findAllByOrderByCreatedAtDesc();
}