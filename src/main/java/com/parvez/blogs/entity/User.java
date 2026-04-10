package com.parvez.blogs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.parvez.blogs.config.AuthProvider;
import com.parvez.blogs.validation.LocalUserValidation;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Version
    @Column(nullable = false)
    private Long version = 0L; // Optimistic locking

    /**
     * LOCAL users: exactly 8 chars, upper + lower + special.
     * GOOGLE users: "google_{sub}" — exempt from these rules.
     * The @Size and @Pattern are grouped under LocalUserValidation
     * so Hibernate only enforces them when explicitly requested.
     */
    @NotBlank(message = "Username is required")
    @Size(
            min = 8, max = 50,              // widened max — Google sub produces longer usernames
            groups = LocalUserValidation.class,
            message = "Username must be exactly 8 characters"
    )
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8}$",
            groups = LocalUserValidation.class,
            message = "Username must contain upper, lower, and special character"
    )
    @Column(unique = true, nullable = false, length = 50)  // widened from 8 → 50
    private String username;

    @NotBlank(message = "First name is required")
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email  format")
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * Age:
     * - Bean validation
     * - SQL-level constraint
     */
/*
    @Min(value = 0, message = "Age can not be negative")
    @Column(name = "age", nullable = false)
    private Integer age;
*/

/*
    @PositiveOrZero(message = "Salary can not be negative")
    @Column(nullable = false)
    private Double salary;
*/

    /**
     * LOCAL users: required, min 8 chars, BCrypt hashed.
     * GOOGLE users: null — they never have a password.
     *
     * @NotBlank and @Size are grouped so they only fire for LOCAL users.
     */
    @NotBlank(
            groups = LocalUserValidation.class,
            message = "Password is required"
    )
    @Size(
            min = 8,
            groups = LocalUserValidation.class,
            message = "Password must be at least 8 characters"
    )
    @JsonIgnore
    @Column(nullable = true)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider = AuthProvider.LOCAL;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL)
    private List<Post> posts;
}
