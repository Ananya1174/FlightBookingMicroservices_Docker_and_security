package com.authservice.model;

//imports
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 // display name (not used for auth)
 @Column(nullable = true)
 private String username;

 @Column(nullable = false, unique = true)
 private String email;

 @Column(nullable = false)
 private String password;

 // redundant convenience field that we will keep in sync
 @Column(name = "role", nullable = false)
 private String role;

 @ManyToMany(fetch = FetchType.EAGER)
 @JoinTable(name = "user_roles",
         joinColumns = @JoinColumn(name = "user_id"),
         inverseJoinColumns = @JoinColumn(name = "role_id"))
 private Set<Role> roles = new HashSet<>();

 @Column(nullable = false)
 private Instant createdAt = Instant.now();

 /**
  * Keep the redundant 'role' column in sync automatically.
  * If multiple roles exist, store them comma-separated e.g. ROLE_ADMIN,ROLE_USER
  */
 @PrePersist
 @PreUpdate
 public void syncRoleColumn() {
     if (roles == null || roles.isEmpty()) {
         this.role = "ROLE_USER"; // default
     } else {
         // collect enum names (Role.getName().name())
         this.role = roles.stream()
                 .map(r -> r.getName().name())
                 .sorted()
                 .collect(Collectors.joining(","));
     }
 }
}