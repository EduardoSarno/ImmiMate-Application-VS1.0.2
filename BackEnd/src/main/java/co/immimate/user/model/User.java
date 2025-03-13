package co.immimate.user.model;

import java.time.Instant;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity  // Marks this class as a JPA entity (maps to a database table)
@Table(name = "users")  // Specifies the table name in the database
@Getter  // Generates getter methods for all fields (from Lombok)
@Setter  // Generates setter methods for all fields (from Lombok)
@NoArgsConstructor  // Generates a no-argument constructor (from Lombok)
@AllArgsConstructor  // Generates a constructor with all fields as arguments (from Lombok)

public class User {

    @Id
    @GeneratedValue
    private UUID id;  // Primary Key

    @Column(nullable = false, unique = true)
    private String email;  // Unique Email

    private String password;  // Optional for password-based login
    
    @Column(name = "google_id")
    private String googleId;  // If using Google OAuth

    private String name;  // User's Full Name
    
    @Column(name = "first_name")
    private String firstName;  // User's First Name
    
    @Column(name = "last_name")
    private String lastName;  // User's Last Name
    
    @Column(name = "phone_number")
    private String phoneNumber;  // User's Phone Number
    
    @Column(nullable = false)
    private String role = "USER";  // User role (USER, ADMIN, etc.)
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();  // Auto-set to now when created

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    // Auto-update `updatedAt` before persisting
    @PreUpdate
    public void setLastUpdated() {
        this.updatedAt = Instant.now();
    }
}