package gr.hua.dit.ds.shelter.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "shelters",
        indexes = {
                @Index(name = "idx_shelters_name", columnList = "name")
        }
        )
public class Shelter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String name;

    @Size(max = 255)
    @Column(length = 255)
    private String address;

    @Size(max = 60)
    @Column(length = 60)
    private String city;

    @Size(max = 30)
    @Column(length = 30)
    private String phone;

    @Column
    private Boolean authorized = false;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = true, unique = true)
    private User userShelter;

    public User getUserShelter() {
        return userShelter;
    }
    public void setUserShelter(User userVet) {
        this.userShelter = userVet;
    }

    @OneToMany(
            mappedBy = "shelter",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            orphanRemoval = false
    )
    private List<Animal> animals = new ArrayList<>();

    // Owning side of the relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vet_id", nullable = true,
            foreignKey = @ForeignKey(name = "fk_vets_shelter"))
    private Vet vet;

    public Vet getVet() {
        return vet;
    }
    public void setVet(Vet vet) {
        this.vet = vet;
    }

    public Shelter() {}

    public Boolean getAuthorized() {
        return authorized;
    }
    public void setAuthorized(Boolean authorized) {
        this.authorized = authorized;
    }

    // Convenience methods to keep both sides in sync
    public void addAnimal(Animal animal) {
        if (animal == null) return;
        animals.add(animal);
        animal.setShelter(this);
    }

    public void removeAnimal(Animal animal) {
        if (animal == null) return;
        animals.remove(animal);
        if (animal.getShelter() == this) {
            animal.setShelter(null);
        }
    }

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public List<Animal> getAnimals() { return animals; }
    public void setAnimals(List<Animal> animals) { this.animals = animals; }
}