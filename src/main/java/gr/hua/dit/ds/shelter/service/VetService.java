package gr.hua.dit.ds.shelter.service;

import gr.hua.dit.ds.shelter.entities.Shelter;
import gr.hua.dit.ds.shelter.entities.User;
import gr.hua.dit.ds.shelter.entities.Vet;
import gr.hua.dit.ds.shelter.repositories.ShelterRepository;
import gr.hua.dit.ds.shelter.repositories.VetRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VetService {

    private final VetRepository vetRepository;
    private final ShelterRepository shelterRepository;

    public VetService(VetRepository vetRepository, ShelterRepository shelterRepository) {
        this.vetRepository = vetRepository;
        this.shelterRepository = shelterRepository;
    }

    @Transactional
    public void assignShelter(Integer vetId, Long shelterId) {
        Vet vet = vetRepository.findById(vetId)
                .orElseThrow(() -> new EntityNotFoundException("Vet not found: " + vetId));

        Shelter shelter = shelterRepository.findById(Math.toIntExact(shelterId))
                .orElseThrow(() -> new EntityNotFoundException("Shelter not found: " + shelterId));

        // If already assigned to this vet, prevent redundant write
        if (shelter.getVet() != null && shelter.getVet().getId() != null
                && shelter.getVet().getId().equals(vet.getId())) {
            throw new IllegalArgumentException("Shelter is already assigned to this vet.");
        }

        // Assign the vet on the owning side and persist via ShelterRepository
        shelter.setVet(vet);
        shelterRepository.save(shelter);
    }

    @Transactional
    public List<Vet> getVets() {
        return vetRepository.findAll();
    }

    /**
     * Merge-safe save: if vet has an id, load the managed entity and copy fields into it,
     * avoiding "multiple representations" of the same entity. If no id, insert new.
     */
    @Transactional
    public Vet saveVet(Vet incoming) {
        if (incoming.getId() != null) {
            Vet managed = vetRepository.findById(incoming.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Vet not found: " + incoming.getId()));
            copyEditableFields(incoming, managed);
            // keep association if incoming has it (avoid nulling unintentionally)
            if (incoming.getUserVet() != null) {
                managed.setUserVet(incoming.getUserVet());
            }
            return vetRepository.save(managed);
        }
        return vetRepository.save(incoming);
    }

    /**
     * Upsert the Vet for a given user:
     * - If user already has a Vet, update it (managed entity).
     * - Otherwise create a new Vet and link it to the user.
     */
    @Transactional
    public Vet upsertForUser(User user, Vet incoming) {
        Vet existing = user.getVet();
        if (existing != null) {
            copyEditableFields(incoming, existing);
            // ensure association is intact
            existing.setUserVet(user);
            return vetRepository.save(existing);
        } else {
            incoming.setUserVet(user); // owning side: sets FK
            return vetRepository.save(incoming);
        }
    }

    /**
     * Toggle authorization on an existing Vet (e.g., admin flow).
     */
    @Transactional
    public Vet toggleAuthorization(Vet vet) {
        vet.setAuthorized(!Boolean.TRUE.equals(vet.getAuthorized()));
        return vetRepository.save(vet);
    }

    @Transactional
    public Vet getVet(Integer vetId) {
        return vetRepository.findById(vetId).orElseThrow(() -> new IllegalArgumentException("Vet not found: " + vetId));
    }

    // Helper to control which fields are editable from the form
    private void copyEditableFields(Vet src, Vet target) {
        target.setFirstName(src.getFirstName());
        target.setLastName(src.getLastName());
        target.setIdentificationNumber(src.getIdentificationNumber());
        target.setAuthorized(src.getAuthorized());
    }
}