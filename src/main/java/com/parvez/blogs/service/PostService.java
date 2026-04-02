package com.parvez.blogs.service;

import com.parvez.blogs.dto.PostRequest;
import com.parvez.blogs.dto.PostResponse;
import com.parvez.blogs.entity.Post;
import com.parvez.blogs.exception.ResourceNotFoundException;
import com.parvez.blogs.repository.PostRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    @Value("${app.pagination.max-size:50}")
    private int maxPageSize;

    private final PostRepository postRepository;

    /* ================= GET SINGLE POST ================= */
    public PostResponse getPostById(Long id) {
        return postRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
    }

    /* ================= GET ALL POSTS ================= */
    public Page<PostResponse> getAllPosts(int page, int size) {

        int validatedSize = size > maxPageSize ? maxPageSize : size;

        Pageable pageable = PageRequest.of(page, validatedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postRepository.findAll(pageable).map(this::mapToResponse);
    }

    /* ================= CREATE (ADMIN) ================= */
    @Transactional
    public PostResponse createPost(PostRequest postRequest) {

        String slug = generateSlug(postRequest.getTitle());

        if (postRepository.findBySlug(slug).isPresent()) {
            throw new DataIntegrityViolationException("Post already exists with Slug: " + slug);
        }

        Post post = new Post();
        post.setTitle(postRequest.getTitle());
        post.setContent(postRequest.getContent());
        post.setSlug(generateSlug(postRequest.getTitle()));
        post.setCreatedAt(LocalDateTime.now());

        Post savedPost = postRepository.save(post);
        return mapToResponse(savedPost);
    }

    /* ================= UPDATE (ADMIN) ================= */
    @Transactional
    public PostResponse updatePost(Long id, PostRequest postRequest) {

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

        String newSlug = generateSlug(postRequest.getTitle());

        if (post.getSlug().equals(newSlug) && postRepository.findBySlug(newSlug).isPresent()) {
            throw new DataIntegrityViolationException("Post already exists with Slug: " + newSlug);
        }

        post.setTitle(postRequest.getTitle());
        post.setContent(postRequest.getContent());
        post.setSlug(newSlug);

        // Hibernate checks the 'version' field during the flush/save
        post.setUpdatedAt(LocalDateTime.now());
        return mapToResponse(postRepository.save(post));
    }

    /* ================= DELETE POST ================= */
    @Transactional
    public void deletePost(Long id) {

        Post post = postRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
        postRepository.delete(post);
        log.info("Post ID {} was soft-deleted.", id);
    }

    /* ================= HELPERS ================= */
    private String generateSlug(String title) {
        // Simple regex to make title URL-friendly
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-");
    }

    private PostResponse mapToResponse(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getSlug(),
                post.getUrl(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
