package com.infy.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.infy.order.entity.ProdOrderCompositeKey;
import com.infy.order.entity.ProductsOrdered;

public interface ProductsOrderedRepository extends JpaRepository<ProductsOrdered, ProdOrderCompositeKey>{
	
	public List<ProductsOrdered> findByOrderid(Integer orderid);

}
