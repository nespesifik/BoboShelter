package gr.hua.dit.ds.shelter.controllers;

import gr.hua.dit.ds.shelter.entities.Animal;
import gr.hua.dit.ds.shelter.entities.Shelter;
import gr.hua.dit.ds.shelter.entities.User;
import gr.hua.dit.ds.shelter.entities.Visitor;
import gr.hua.dit.ds.shelter.service.AnimalService;
import gr.hua.dit.ds.shelter.service.ShelterService;
import gr.hua.dit.ds.shelter.service.UserService;
import gr.hua.dit.ds.shelter.service.VisitorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Controller
@RequestMapping("animal")
public class AnimalController {

    private final AnimalService animalService;
    private final ShelterService shelterService;
    private final UserService userService;
    private final VisitorService visitorService;

    public AnimalController(AnimalService animalService, ShelterService shelterService, UserService userService, VisitorService visitorService) {
        this.animalService = animalService;
        this.shelterService = shelterService;
        this.userService = userService;
        this.visitorService = visitorService;
    }

    @PreAuthorize("isAuthenticated() and principal instanceof T(gr.hua.dit.ds.shelter.service.CustomUserDetails)")
    @GetMapping("/new")
    public String newAnimalForCurrentShelter(
            @AuthenticationPrincipal gr.hua.dit.ds.shelter.service.CustomUserDetails currentUser,
            Model model
    ) {

        User user = (User) userService.getUser(currentUser.getUserId().longValue());
        Shelter shelter = user.getShelter();

        if (shelter == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No shelter associated with current user");
        }

        if (!canAccessShelter(user, shelter)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to create animals for this shelter");
        }

        Animal animal = new Animal();
        animal.setShelter(shelter);

        model.addAttribute("animal", animal);
        model.addAttribute("id", null);
        return "animal/animal";
    }

    @PreAuthorize("isAuthenticated() and principal instanceof T(gr.hua.dit.ds.shelter.service.CustomUserDetails)")
    @PostMapping("/new")
    public String createAnimalForCurrentShelter(
            @Valid @ModelAttribute("animal") Animal formAnimal,
            BindingResult bindingResult,
            @AuthenticationPrincipal gr.hua.dit.ds.shelter.service.CustomUserDetails currentUser,
            Model model
    ) {
        //This does not change ever!
        User user = (User) userService.getUser(currentUser.getUserId().longValue());
        Shelter shelter = user.getShelter();
        if (shelter == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No shelter associated with current user");
        }

        if (!canAccessShelter(user, shelter)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to create animals for this shelter");
        }

        if (bindingResult.hasErrors()) {
            // keep association and return form with errors
            formAnimal.setShelter(shelter);
            model.addAttribute("animal", formAnimal);
            model.addAttribute("id", null);
            return "animal/animal";
        }

        // ensure this is a new entity and link it to the shelter
        formAnimal.setId(null);
        formAnimal.setShelter(shelter);

        Animal saved = animalService.saveAnimal(formAnimal);

        model.addAttribute("animal", saved);
        model.addAttribute("id", saved.getId());
        return "animal/animal";
    }

    // List animals for a specific shelter; only admin, the shelter owner, or the vet assigned to that shelter can view
    @PreAuthorize("isAuthenticated() and principal instanceof T(gr.hua.dit.ds.shelter.service.CustomUserDetails)")
    @GetMapping("/shelter/{shelterId}")
    public String listByShelter(@PathVariable Long shelterId,
                                @AuthenticationPrincipal gr.hua.dit.ds.shelter.service.CustomUserDetails currentUser,
                                Model model) {
        User user = (User) userService.getUser(currentUser.getUserId().longValue());
        Shelter shelter = shelterService.getShelter(shelterId);

        if (!canAccessShelter(user, shelter)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view this page");
        }

        List<Animal> animals = shelter.getAnimals();
        model.addAttribute("animals", animals);
        model.addAttribute("shelterId", shelterId);
        return "animal/animals";
    }

    // Show a single animal by its id (profile view)
    @PreAuthorize("isAuthenticated() and principal instanceof T(gr.hua.dit.ds.shelter.service.CustomUserDetails)")
    @GetMapping("/{id}")
    public String showAnimal(@PathVariable Long id,
                             @AuthenticationPrincipal gr.hua.dit.ds.shelter.service.CustomUserDetails currentUser,
                             Model model) {
        User user = (User) userService.getUser(currentUser.getUserId().longValue());
        Animal animal = animalService.getAnimal(id);
        System.out.println("Animal: "+animal.getId());
        Shelter shelter = animal.getShelter();
        System.out.println("Shelter: "+shelter.getId());

        System.out.println("toBeVisted: "+animal.getToBeVisited());

        if (!canAccessShelter(user, shelter)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view this page");
        }

        model.addAttribute("animal", animal);
        model.addAttribute("id", id);
        return "animal/animal";
    }

    // Save changes to an animal by its id. Admin toggles "accepted"; shelter owner or assigned vet can edit details.
    @PreAuthorize("isAuthenticated() and principal instanceof T(gr.hua.dit.ds.shelter.service.CustomUserDetails)")
    @PostMapping("/{id}")
    public String saveAnimal(@PathVariable Long id,
                             @Valid @ModelAttribute("animal") Animal formAnimal,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal gr.hua.dit.ds.shelter.service.CustomUserDetails currentUser,
                             Model model) {
        User user = (User) userService.getUser(currentUser.getUserId().longValue());
        Animal existing = animalService.getAnimal(id);
        Shelter shelter = existing.getShelter();
        formAnimal.setAccepted(existing.isAccepted());

        if (!canAccessShelter(user, shelter)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to perform this action");
        }

        Animal result;
        if (user.isVet()) {
            // Admin toggles accepted flag on the persisted animal
            result = animalService.toggleAccepted(existing);
        } else if (user.isShelter() && !bindingResult.hasErrors()) {
            // Shelter owner or assigned vet can update fields; enforce merge-safe save via service
            formAnimal.setId(id);           // ensure correct target
            formAnimal.setShelter(shelter); // keep association intact
            if (formAnimal.getStatus() == Animal.Status.ADOPTED){
                Animal animal = animalService.getAnimal(id);
                var visitor = animal.getVisitorPlanningVisit();
                if (visitor != null){
                    formAnimal.setVisitor(visitor);
                    formAnimal.setVisitorPlanningVisit(null);
                }
                formAnimal.setToBeVisited(false);
                formAnimal.setRequestToBeVisited(false);
            }
            result = animalService.saveAnimal(formAnimal);
        } else if (user.hasRole("ROLE_VISITOR")){
            if (!existing.getRequestToBeVisited())
                existing.setVisitorPlanningVisit(user.getVisitor());
            else
                existing.setVisitorPlanningVisit(null);
            existing.setToBeVisited(false);
            result = animalService.toggleRequestToBeVisited(existing);

            /*Visitor visitor = user.getVisitor();
            List<Animal> animals = visitor.getPlannedVisits();
            System.out.println("Animals Number: "+animals.size());*/
        }
        else {
            // return form with validation errors
            result = formAnimal;
        }

        model.addAttribute("animal", result);
        model.addAttribute("id", id);
        return "animal/animal";
    }

    /*private boolean isOwnerShelterId(User user, Shelter shelter) {
        // Shelter owner check
        if (shelter.getUserShelter() != null && shelter.getUserShelter().getId().equals(currentUserId)) {
            return true;
        }
    }*/
    // Authorization helper: admin OR shelter owner OR vet assigned to the shelter
    private boolean canAccessShelter(User user, Shelter shelter) {
        if (user == null || shelter == null) return false;
        if (user.isAdmin()) return true;

        Integer currentUserId = user.getId();

        // Shelter owner check
        if (shelter.getUserShelter() != null && shelter.getUserShelter().getId().equals(currentUserId) && shelter.getUserShelter().isShelter()) {
            return true;
        }

        // Vet assigned to this shelter check
        if (shelter.getVet() != null
                && shelter.getVet().getUserVet() != null
                && shelter.getVet().getUserVet().getId().equals(currentUserId)) {
            return true;
        }

        return user.hasRole("ROLE_VISITOR");

    }
}
