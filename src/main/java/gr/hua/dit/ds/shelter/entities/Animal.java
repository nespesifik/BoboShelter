package gr.hua.dit.ds.shelter.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(
        name = "animals",
        indexes = {
                @Index(name = "idx_animals_species", columnList = "species"),
                @Index(name = "idx_animals_status", columnList = "status"),
                @Index(name = "idx_animals_shelter", columnList = "shelter_id")
        }
)
public class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Core identity
    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    @NotBlank
    @Size(max = 60)
    @Column(nullable = false, length = 60)
    private String species; // e.g., "Dog", "Cat"

    @Size(max = 120)
    @Column(length = 120)
    private String breed;

    @Min(0)
    @Max(60)
    @Column(name = "age_years")
    private Integer ageYears;

    @Min(0)
    @Max(11)
    @Column(name = "age_months")
    private Integer ageMonths;

    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private Sex sex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.AVAILABLE;

    @Column(nullable = false)
    private boolean vaccinated = false;

    @Column(nullable = false)
    private boolean neutered = false;

    @Size(max = 2048)
    @Column(name = "photo_url", length = 2048)
    private String photoUrl;

    @Size(max = 4000)
    @Column(length = 4000)
    private String description;

    // Owning side of the relationship
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shelter_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_animals_shelter"))
    private Shelter shelter;


    @Column
    private boolean accepted = false;
    public boolean isAccepted() {
        return accepted;
    }
    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    @ManyToOne
    @JoinColumn(name = "visitor_id")
    private Visitor visitor; // Reference to the adopting visitor

    // Getter and Setter for visitor
    public Visitor getVisitor() {
        return visitor;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_planned_visitor_id", foreignKey = @ForeignKey(name = "fk_animals_visit_planned_visitor"))
    private Visitor visitorPlanningVisit; // Visitor planning to visit this animal

    // other relationships
    public Visitor getVisitorPlanningVisit() {
        return visitorPlanningVisit;
    }

    public void setVisitorPlanningVisit(Visitor visitorPlanningVisit) {
        this.visitorPlanningVisit = visitorPlanningVisit;
    }

    @Column
    private Boolean requestToBeVisited = false;

    public Boolean getRequestToBeVisited() {
        return requestToBeVisited;
    }

    public void setRequestToBeVisited(Boolean requestToBeVisited) {
        this.requestToBeVisited = requestToBeVisited;
    }

    @Column
    private Boolean toBeVisited = false;

    public Boolean getToBeVisited() {
        return toBeVisited;
    }

    public void setToBeVisited(Boolean toBeVisited) {
        this.toBeVisited = toBeVisited;
    }

    public void setVisitor(Visitor visitor) {
        this.visitor = visitor;
    }

    public Animal() {}

    // Getters and setters
    public Shelter getShelter() { return shelter; }
    public void setShelter(Shelter shelter) { this.shelter = shelter; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species; }

    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }

    public Integer getAgeYears() { return ageYears; }
    public void setAgeYears(Integer ageYears) { this.ageYears = ageYears; }

    public Integer getAgeMonths() { return ageMonths; }
    public void setAgeMonths(Integer ageMonths) { this.ageMonths = ageMonths; }

    public Sex getSex() { return sex; }
    public void setSex(Sex sex) { this.sex = sex; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public boolean isVaccinated() { return vaccinated; }
    public void setVaccinated(boolean vaccinated) { this.vaccinated = vaccinated; }

    public boolean isNeutered() { return neutered; }
    public void setNeutered(boolean neutered) { this.neutered = neutered; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // One to one relation with the visitor entity. TO DO.
    // Domain enums
    public enum Status {
        AVAILABLE,
        PENDING,
        ADOPTED
    }

    public enum Sex {
        MALE,
        FEMALE,
        UNKNOWN
    }

}