package com.datapipeline.api;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TopSalesmanRepository extends JpaRepository<TopSalesman, String> {
}
