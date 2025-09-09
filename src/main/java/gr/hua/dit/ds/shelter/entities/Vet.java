package gr.hua.dit.ds.shelter.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vet")
public class Vet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Integer id;

    @Column
    @NotEmpty(message = "First name is required")
    private String firstName;

    @Column
    @NotEmpty(message = "Last name is required")
    private String lastName;

    @Column
    private Boolean authorized = false;

    // Nullable, unique, exactly 10 digits if provided
    @Column(name = "identification_number", unique = true, length = 10)
    @Pattern(regexp = "\\d{10}", message = "Identification number must be exactly 10 digits")
    private String identificationNumber;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = true, unique = true)
    private User userVet;

    @OneToMany(
            mappedBy = "vet",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            orphanRemoval = false
    )
    private List<Shelter> shelterVet = new ArrayList<>();

    public List<Shelter> getShelterVet() {return shelterVet;}
    public void setShelterVet(List<Shelter> shelterVet) {this.shelterVet = shelterVet;}

    public Vet()
    {
        this.authorized = false;
    }

    public Vet(String firstName, String lastName, String email, String identificationNumber, Boolean authorized) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.identificationNumber = identificationNumber;
        this.authorized = authorized;
    }

    public User getUserVet() {
        return userVet;
    }

    public void setUserVet(User userVet) {
        this.userVet = userVet;
    }

    public Boolean getAuthorized() {
        return authorized;
    }

    public void setAuthorized(Boolean authorized) {
        this.authorized = authorized;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getIdentificationNumber() { return identificationNumber; }
    public void setIdentificationNumber(String identificationNumber) { this.identificationNumber = identificationNumber; }

    @Override
    public String toString() {
        return "Vet{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", identificationNumber='" + identificationNumber + '\'' +
                '}';
    }
}

