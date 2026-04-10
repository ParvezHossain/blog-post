package com.parvez.blogs.security;

import com.parvez.blogs.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("postSecurity")
@RequiredArgsConstructor
public class PostSecurity {

    private final PostRepository postRepository;

    public boolean canModify(Long postId, String username) {
        return postRepository.findByIdIncludingDeleted(postId)
                .map(post -> post.getAuthor().getUsername().equals(username))
                .orElse(false);
    }
}
