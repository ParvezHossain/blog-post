package com.parvez.blogs.repository;

import com.parvez.blogs.entity.Post;
import com.parvez.blogs.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // Find by slug (only active due to @Where)
    Optional<Post> findBySlug(String slug);

    // Hard-delete old soft-deleted posts
    @Modifying
    @Query(value = "DELETE FROM posts WHERE deleted = true AND updated_at < :cutoff", nativeQuery = true)
    int deleteByDeletedTrueAndUpdatedAtBefore(@Param("cutoff") LocalDateTime cutoff);

    // Find by ID including deleted
    @Query("SELECT p FROM Post p WHERE p.id = :id")
    Optional<Post> findByIdIncludingDeleted(@Param("id") Long id);

    // Find active post by ID
    @Query("SELECT p FROM Post p WHERE p.id = :id AND p.deleted = false")
    Optional<Post> findActivePostById(@Param("id") Long id);

    // Get all posts, optionally including deleted
    @Query("SELECT p FROM Post p WHERE (:includeDeleted = true OR p.deleted = false)")
    List<Post> findAllPosts(@Param("includeDeleted") boolean includeDeleted);

    // Get all posts of an author, optionally including deleted
    @Query("SELECT p FROM Post p WHERE p.author = :author AND (:includeDeleted = true OR p.deleted = false)")
    List<Post> findAllByAuthor(@Param("author") User author, @Param("includeDeleted") boolean includeDeleted);

    // Get a single post by slug with deleted flag option
    @Query("SELECT p FROM Post p WHERE p.slug = :slug AND (:includeDeleted = true OR p.deleted = false)")
    Optional<Post> findBySlugIncludingDeleted(@Param("slug") String slug, @Param("includeDeleted") boolean includeDeleted);


}
