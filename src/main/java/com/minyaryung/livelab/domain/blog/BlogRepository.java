package com.minyaryung.livelab.domain.blog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BlogRepository extends JpaRepository<BlogPost, Long> {
    Page<BlogPost> findByPublishedTrueOrderByCreatedAtDesc(Pageable pageable);
    Page<BlogPost> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Optional<BlogPost> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
