package com.minyaryung.livelab.domain.redisdemo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
            select new com.minyaryung.livelab.domain.redisdemo.CategoryStats(
                p.subCategory, count(p), avg(p.price), min(p.price), max(p.price)
            )
            from Product p where p.category = :category
            group by p.subCategory order by p.subCategory
            """)
    List<CategoryStats> aggregateByCategory(@Param("category") String category);
}
