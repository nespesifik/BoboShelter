package gr.hua.dit.ds.shelter.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;


@Entity
@Table(	name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "username"),
                @UniqueConstraint(columnNames = "email")
        })
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Size(max = 20)
    private String username;

    @NotBlank
    @Size(max = 50)
    @Email
    private String email;

    @NotBlank
    @Size(max = 120)
    private String password;

    @Enumerated
    private UserType userType;

    @OneToOne(mappedBy = "userVet", cascade = {CascadeType.ALL}, fetch = FetchType.LAZY)
    private Vet vet;

    @OneToOne(mappedBy = "userShelter", cascade = {CascadeType.ALL}, fetch = FetchType.LAZY)
    private Shelter shelter;

    @OneToOne(mappedBy = "userVisitor", cascade = {CascadeType.ALL}, fetch = FetchType.LAZY)
    private Visitor visitor;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(	name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    public User() {
    }

    public User(String username, String email, String password, UserType userType) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.userType = userType;
    }

    /**
     * Checks if this user has the given role name (e.g., "ROLE_ADMIN").
     */
    public boolean hasRole(String roleName) {
        if (roleName == null || roles == null) {
            return false;
        }
        return roles.stream().anyMatch(r -> roleName.equals(r.getName()));
    }

    /**
     * Convenience method to check if the user has the ADMIN role.
     */

    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    public boolean isShelter() {
        return hasRole("ROLE_SHELTER");
    }

    public boolean isVet() {
        return hasRole("ROLE_VET");
    }

    public boolean isVisitor() {
        return hasRole("ROLE_VISITOR");
    }

    public Vet getVet() {
        return vet;
    }

    public Shelter getShelter() {
        return shelter;
    }

    public Visitor getVisitor() {
        return visitor;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    @Override
    public String toString() {
        return username;
    }
}