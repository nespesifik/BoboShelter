package gr.hua.dit.ds.shelter.entities;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
public class Visitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String phone;

    @Column(nullable = false)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sex sex;

    @Column(nullable = false)
    private Integer age;

    @Lob
    private String bio; // Long String field for storing visitor's bio

    @OneToMany(mappedBy = "visitor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Animal> adoptedAnimals; // Animals adopted by this visitor

    public void addAdoptedAnimal(Animal animal) {
        adoptedAnimals.add(animal);
        animal.setVisitor(this);
    }

    public List<Animal> removeAdoptedAnimal(Animal animal){
        adoptedAnimals.remove(animal);
        animal.setVisitor(null);
        return adoptedAnimals;
    }

    //@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = true, unique = true)
    private User userVisitor;

    public User getUserVisitor() {
        return userVisitor;
    }

    public void setUserVisitor(User userVisitor) {
        this.userVisitor = userVisitor;
    }

    // These are the planned visits of the visitor
    @OneToMany(mappedBy = "visitorPlanningVisit")
    private List<Animal> plannedVisits; // Animals the visitor has planned to visit

    public List<Animal> getPlannedVisits() {
        return plannedVisits;
    }

    public void setPlannedVisits(List<Animal> plannedVisits) {
        this.plannedVisits = plannedVisits;
    }

    public List<Animal> removePlannedVisit(Animal animal) {
        plannedVisits.remove(animal);
        animal.setVisitorPlanningVisit(null);
        return plannedVisits;
    }

    public Visitor() {}

    public enum Sex {
        MALE, FEMALE;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Sex getSex() {
        return sex;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public List<Animal> getAdoptedAnimals() {
        return adoptedAnimals;
    }

    public void setAdoptedAnimals(List<Animal> adoptedAnimals) {
        this.adoptedAnimals = adoptedAnimals;
    }
}