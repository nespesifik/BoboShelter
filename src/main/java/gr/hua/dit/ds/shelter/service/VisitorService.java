package gr.hua.dit.ds.shelter.service;

import gr.hua.dit.ds.shelter.entities.User;
import gr.hua.dit.ds.shelter.entities.Visitor;
import gr.hua.dit.ds.shelter.repositories.VisitorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VisitorService {

    private final VisitorRepository visitorRepository;

    public VisitorService(VisitorRepository visitorRepository) {
        this.visitorRepository = visitorRepository;
    }

    @Transactional
    public List<Visitor> getVisitors() {
        return visitorRepository.findAll();
    }

    /**
     * Merge-safe save: if visitor has an id, load the managed entity and copy fields into it,
     * avoiding "multiple representations" of the same entity. If no id, insert new.
     */
    @Transactional
    public Visitor saveVisitor(Visitor incoming) {
        if (incoming.getId() != null) {
            Visitor managed = visitorRepository.findById(incoming.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Visitor not found: " + incoming.getId()));
            copyEditableFields(incoming, managed);
            // keep association if incoming has it (avoid nulling unintentionally)
            try {
                var getter = Visitor.class.getMethod("getUserVisitor");
                var setter = Visitor.class.getMethod("setUserVisitor", User.class);
                Object u = getter.invoke(incoming);
                if (u != null) {
                    setter.invoke(managed, u);
                }
            } catch (ReflectiveOperationException ignored) {
                // If Visitor doesn't expose user binding methods, just ignore.
            }
            return visitorRepository.save(managed);
        }
        return visitorRepository.save(incoming);
    }

    /**
     * Upsert the Visitor for a given user:
     * - If user already has a Visitor, update it (managed entity).
     * - Otherwise create a new Visitor and link it to the user.
     */
    @Transactional
    public Visitor upsertForUser(User user, Visitor incoming) {
        if (user == null) throw new IllegalArgumentException("User must not be null");
        if (incoming == null) throw new IllegalArgumentException("Visitor payload must not be null");

        Visitor existing = user.getVisitor();
        if (existing != null) {
            copyEditableFields(incoming, existing);
            // ensure association is intact (owning side: Visitor has FK to User)
            try {
                Visitor.class.getMethod("setUserVisitor", User.class).invoke(existing, user);
            } catch (ReflectiveOperationException ignored) {
                // If Visitor doesn't expose user binding methods, just ignore.
            }
            return visitorRepository.save(existing);
        } else {
            try {
                Visitor.class.getMethod("setUserVisitor", User.class).invoke(incoming, user);
            } catch (ReflectiveOperationException ignored) {
                // If Visitor doesn't expose user binding methods, just ignore.
            }
            return visitorRepository.save(incoming);
        }
    }

    @Transactional
    public Visitor getVisitor(Long visitorId) {
        return visitorRepository.findById(visitorId)
                .orElseThrow(() -> new IllegalArgumentException("Visitor not found: " + visitorId));
    }

    // Helper to control which fields are editable from the form
    private void copyEditableFields(Visitor src, Visitor target) {
        target.setFirstName(src.getFirstName());
        target.setLastName(src.getLastName());
        target.setPhone(src.getPhone());
        target.setAddress(src.getAddress());
        target.setSex(src.getSex());
        target.setAge(src.getAge());
        target.setBio(src.getBio());
        // Associations like adopted/planned animals are not overwritten here.
    }
}