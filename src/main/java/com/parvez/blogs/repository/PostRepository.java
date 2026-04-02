package com.parvez.blogs.repository;

import com.parvez.blogs.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    Optional<Post> findBySlug(String slug);

    @Modifying
    @Query(value = "DELETE FROM posts WHERE deleted = true AND updated_at < :cutoff", nativeQuery = true)
    int deleteByDeletedTrueAndUpdatedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
