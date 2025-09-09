package gr.hua.dit.ds.shelter.service;

import gr.hua.dit.ds.shelter.entities.Shelter;
import gr.hua.dit.ds.shelter.entities.User;
import gr.hua.dit.ds.shelter.repositories.ShelterRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShelterService {

    private final ShelterRepository shelterRepository;

    public ShelterService(ShelterRepository shelterRepository) {
        this.shelterRepository = shelterRepository;
    }

    @Transactional
    public List<Shelter> getShelters() {
        return shelterRepository.findAll();
    }

    /**
     * Merge-safe save: if shelter has an id, load the managed entity and copy fields into it,
     * avoiding "multiple representations" of the same entity. If no id, insert new.
     */
    @Transactional
    public Shelter saveShelter(Shelter incoming) {
        if (incoming.getId() != null) {
            Shelter managed = shelterRepository.findById(incoming.getId().intValue())
                    .orElseThrow(() -> new IllegalArgumentException("Shelter not found: " + incoming.getId()));
            copyEditableFields(incoming, managed);
            // keep associations if incoming has them (avoid nulling unintentionally)
            if (incoming.getUserShelter() != null) {
                managed.setUserShelter(incoming.getUserShelter());
            }
            return shelterRepository.save(managed);
        }
        return shelterRepository.save(incoming);
    }

    /**
     * Upsert the Shelter for a given user:
     * - If user already has a Shelter, update it (managed entity).
     * - Otherwise create a new Shelter and link it to the user.
     */
    @Transactional
    public Shelter upsertForUser(User user, Shelter incoming) {
        Shelter existing = user.getShelter();
        if (existing != null) {
            copyEditableFields(incoming, existing);
            // ensure association is intact (owning side)
            existing.setUserShelter(user);
            return shelterRepository.save(existing);
        } else {
            incoming.setUserShelter(user); // owning side: sets FK
            return shelterRepository.save(incoming);
        }
    }

    /**
     * Toggle authorization on an existing Shelter (e.g., admin flow).
     */
    @Transactional
    public Shelter toggleAuthorization(Shelter shelter) {
        shelter.setAuthorized(!Boolean.TRUE.equals(shelter.getAuthorized()));
        return shelterRepository.save(shelter);
    }

    @Transactional
    public Shelter getShelter(Long shelterId) {
        return shelterRepository.findById(shelterId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("Shelter not found: " + shelterId));
    }

    // Helper to control which fields are editable from the form
    private void copyEditableFields(Shelter src, Shelter target) {
        target.setName(src.getName());
        target.setAddress(src.getAddress());
        target.setCity(src.getCity());
        target.setPhone(src.getPhone());
        // Do not set authorized here unless non-admins are allowed to modify it via the form.
        // target.setAuthorized(src.getAuthorized());
    }
}