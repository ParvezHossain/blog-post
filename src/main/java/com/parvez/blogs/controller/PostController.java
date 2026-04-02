package com.parvez.blogs.controller;

import com.parvez.blogs.config.ApiPaths;
import com.parvez.blogs.dto.PostRequest;
import com.parvez.blogs.dto.PostResponse;
import com.parvez.blogs.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping(ApiPaths.POSTS)
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * PUBLIC: Anyone can view the paginated list of posts.
     */
    @GetMapping
    public ResponseEntity<Page<PostResponse>> getPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(postService.getAllPosts(page, size));
    }

    /**
     * PUBLIC: Anyone can view a specific post by its ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPostById(id));
    }

    /**
     * SECURE: Restricted to ADMIN. Creates a new blog post.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PostResponse> createPost(@Valid @RequestBody PostRequest postRequest) {

        PostResponse postResponse = postService.createPost(postRequest);

        // Create a URI for the new post (e.g., /api/v1/posts/45)
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(postResponse.getId())
                .toUri();

        postResponse.setUrl(location.toString());
        return ResponseEntity.created(location).body(postResponse);
    }

    /**
     * SECURE: Restricted to ADMIN. Updates an existing post.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PostResponse> updatePost(@PathVariable Long id, @RequestBody PostRequest request) {
        return ResponseEntity.ok(postService.updatePost(id, request));
    }

    /**
     * SECURE: Restricted to ADMIN. Deletes a post.
     * Returns 204 No Content on success.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }
}