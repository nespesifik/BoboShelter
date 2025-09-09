package gr.hua.dit.ds.shelter.service;

import gr.hua.dit.ds.shelter.entities.Animal;
import gr.hua.dit.ds.shelter.entities.Shelter;
import gr.hua.dit.ds.shelter.repositories.AnimalRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnimalService {

    private final AnimalRepository animalRepository;

    public AnimalService(AnimalRepository animalRepository) {
        this.animalRepository = animalRepository;
    }

    @Transactional
    public List<Animal> getAnimals() {
        return animalRepository.findAll();
    }

    /**
     * Merge-safe save: if animal has an id, load the managed entity and copy fields into it,
     * avoiding "multiple representations" of the same entity. If no id, insert new.
     */
    @Transactional
    public Animal saveAnimal(Animal incoming) {
        if (incoming.getId() != null) {
            Animal managed = animalRepository.findById(incoming.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Animal not found: " + incoming.getId()));
            copyEditableFields(incoming, managed);
            // keep association if incoming has it (avoid nulling unintentionally)
            if (incoming.getShelter() != null) {
                managed.setShelter(incoming.getShelter());
            }
            return animalRepository.save(managed);
        }
        return animalRepository.save(incoming);
    }

    /**
     * Upsert the Animal for a given shelter:
     * - If animal has an id, update it within that shelter.
     * - Otherwise create a new Animal and link it to the shelter.
     */
    @Transactional
    public Animal upsertForShelter(Shelter shelter, Animal incoming) {
        if (incoming.getId() != null) {
            Animal managed = animalRepository.findById(incoming.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Animal not found: " + incoming.getId()));
            copyEditableFields(incoming, managed);
            managed.setShelter(shelter);
            return animalRepository.save(managed);
        } else {
            incoming.setShelter(shelter);
            return animalRepository.save(incoming);
        }
    }

    // Toggle whether an animal is accepted
    @Transactional
    public Animal toggleAccepted(Animal animal) {
        animal.setAccepted(!animal.isAccepted());
        return animalRepository.save(animal);
    }

    @Transactional
    public Animal toggleToBeVisited(Animal animal) {
        boolean next = !Boolean.TRUE.equals(animal.getToBeVisited()); // null treated as false
        animal.setToBeVisited(next);
        return animalRepository.save(animal);
    }

    @Transactional
    public Animal toggleRequestToBeVisited(Animal animal) {
        boolean next = !Boolean.TRUE.equals(animal.getRequestToBeVisited()); // null treated as false
        animal.setRequestToBeVisited(next);
        return animalRepository.save(animal);
    }

    @Transactional
    public Animal getAnimal(Long animalId) {
        return animalRepository.findById(animalId)
                .orElseThrow(() -> new IllegalArgumentException("Animal not found: " + animalId));
    }

    // Helper to control which fields are editable from the form
    private void copyEditableFields(Animal src, Animal target) {
        target.setName(src.getName());
        target.setSpecies(src.getSpecies());
        target.setBreed(src.getBreed());
        target.setAgeYears(src.getAgeYears());
        target.setAgeMonths(src.getAgeMonths());
        target.setSex(src.getSex());
        target.setStatus(src.getStatus());
        target.setVaccinated(src.isVaccinated());
        target.setNeutered(src.isNeutered());
        target.setPhotoUrl(src.getPhotoUrl());
        target.setDescription(src.getDescription());

        // visit/adoption flow fields
        target.setAccepted(src.isAccepted());
        target.setRequestToBeVisited(src.getRequestToBeVisited());
        target.setToBeVisited(src.getToBeVisited());
        target.setVisitorPlanningVisit(src.getVisitorPlanningVisit());
        target.setVisitor(src.getVisitor());
    }
}