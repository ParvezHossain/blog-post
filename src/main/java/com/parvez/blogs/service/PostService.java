package com.parvez.blogs.service;

import com.parvez.blogs.dto.PostRequest;
import com.parvez.blogs.dto.PostResponse;
import com.parvez.blogs.entity.Post;
import com.parvez.blogs.entity.Role;
import com.parvez.blogs.entity.User;
import com.parvez.blogs.exception.ResourceNotFoundException;
import com.parvez.blogs.repository.PostRepository;
import com.parvez.blogs.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    @Value("${app.pagination.max-size:50}")
    private int maxPageSize;

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    /* ================= GET SINGLE POST ================= */
    public PostResponse getPostById(Long id, boolean includeDeleted) {

        Post post = includeDeleted
                ? postRepository.findByIdIncludingDeleted(id)
                  .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id))
                : postRepository.findActivePostById(id)
                  .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
        System.out.println("Post: " + post);
        return mapToResponse(post);
    }

    private Post getPostOrThrow(Long id, boolean includeDeleted) {
        return includeDeleted
                ? postRepository.findByIdIncludingDeleted(id)
                  .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id))
                : postRepository.findActivePostById(id)
                  .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
    }

    public PostResponse getPostBySlug(String slug, boolean includeDeleted) {
        return postRepository.findBySlugIncludingDeleted(slug, includeDeleted)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with slug: " + slug));
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

        String username = loggedInUserUsername();
        User user = userRepository
                .findByUsername(username)
                .orElseThrow(
                        () -> new ResourceNotFoundException("User not found with username: " + username)
                );

        Post post = new Post();
        post.setTitle(postRequest.getTitle());
        post.setContent(postRequest.getContent());
        post.setSlug(generateSlug(postRequest.getTitle()));
        post.setAuthor(user);

        Post savedPost = postRepository.save(post);
        return mapToResponse(savedPost);
    }

    /* ================= UPDATE (ADMIN) ================= */
    @Transactional
    public PostResponse updatePost(Long id, PostRequest postRequest) throws AccessDeniedException {

//        Post post = getAuthorizedPost(id, true);

        Post post = postRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

        String newSlug = generateSlug(postRequest.getTitle());

        if (post.getSlug().equals(newSlug) && postRepository.findBySlug(newSlug).isPresent()) {
            throw new DataIntegrityViolationException("Post already exists with Slug: " + newSlug);
        }

        post.setTitle(postRequest.getTitle());
        post.setContent(postRequest.getContent());
        post.setSlug(generateSlug(postRequest.getTitle()));

        // Hibernate checks the 'version' field during the flush/save
        return mapToResponse(postRepository.save(post));
    }


    /* =================  RESTORE DELETE POST ================= */
    @Transactional
    public PostResponse restorePost(Long id) throws AccessDeniedException {
//        Post post = getAuthorizedPost(id, true);
        Post post = postRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
        post.setDeleted(false);
        return mapToResponse(postRepository.save(post));
    }

    @Transactional
    public PostResponse archivePost(Long id) throws AccessDeniedException {
//        Post post = getAuthorizedPost(id, false);
        Post post = postRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
        post.setDeleted(true);
        return mapToResponse(postRepository.save(post));
    }

    /* ================= DELETE POST ================= */
    @Transactional
    public void deletePost(Long id) throws AccessDeniedException {
//        Post post = getAuthorizedPost(id, true);
        Post post = postRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
        postRepository.delete(post);
        log.info("Post ID {} was soft-deleted.", id);
    }

    /* ================= HELPERS ================= */
    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-");
    }

    private PostResponse mapToResponse(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getSlug(),
                post.getContent(),
                post.getUrl(),
                post.isDeleted(),
                post.getAuthor().getUsername(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    private Post getAuthorizedPost(Long id, boolean includeDeleted) throws AccessDeniedException {
        Post post = includeDeleted
                ? postRepository.findByIdIncludingDeleted(id).orElseThrow(() -> new ResourceNotFoundException("Post not found with id: \" + id"))
                : postRepository.findActivePostById(id).orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));

        User currentUser = getCurrentUser();

        boolean isAdmin = currentUser.getRole().name() == Role.ADMIN.name();
        boolean isAuthor = post.getAuthor().getUsername() == currentUser.getUsername();

        // If the user is NEITHER an Admin NOR the Author, block them.
        if (!(isAdmin || isAuthor)) {
            throw new AccessDeniedException("You are not allowed to modify this post");
        }
        return post;
    }

    private User getCurrentUser() {
        String username = loggedInUserUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    private String loggedInUserUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
