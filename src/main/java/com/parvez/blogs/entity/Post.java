package com.parvez.blogs.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "posts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_post_slug", columnNames = {"slug"})
        }
)
@SoftDelete(columnName = "deleted", strategy = SoftDeleteType.ACTIVE)

@Data
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String title;

    @Column(unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String url;
    @Column(nullable = true, columnDefinition = "TEXT")

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Version
    private Long version;
}
