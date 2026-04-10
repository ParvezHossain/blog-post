package com.parvez.blogs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "posts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_post_slug", columnNames = {"slug"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
//@SoftDelete(columnName = "deleted", strategy = SoftDeleteType.ACTIVE)
@SQLDelete(sql = "UPDATE posts SET deleted = true WHERE id=?")
@FilterDef(name = "deletedPostFilter", parameters = @ParamDef(name = "isDeleted", type = Boolean.class))
@Filter(name = "deletedPostFilter", condition = "deleted = :isDeleted")
public class Post extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String title;

    @Column(unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String content;


    @Column(nullable = true, columnDefinition = "TEXT")
    private String url;


    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    @ToString.Exclude
    private User author;

    private boolean deleted = Boolean.FALSE;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Post)) return false;
        Post post = (Post) o;
        return id != null && id.equals(post.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
