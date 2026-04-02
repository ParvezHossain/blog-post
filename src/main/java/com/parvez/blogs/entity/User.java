package com.parvez.blogs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;

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
     * Username rules:
     * - exactly 8 characters
     * - at least one uppercase
     * - at least one lowercase
     * - at least one special character
     */

    @NotBlank(message = "Username is required")
    @Size(min = 8, max = 8, message = "Username must be exactly 8 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8}$",
            message = "Username must contain upper, lower, and special character"
    )
    @Column(unique = true, nullable = false, length = 8)
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
     * Password:
     * - Stored as BCrypt hash
     * - Never serialized to JSON
     */

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role = Role.ADMIN;
}
