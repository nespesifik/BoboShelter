package gr.hua.dit.ds.shelter.controllers;

import gr.hua.dit.ds.shelter.entities.Animal;
import gr.hua.dit.ds.shelter.entities.Shelter;
import gr.hua.dit.ds.shelter.entities.User;
import gr.hua.dit.ds.shelter.entities.Vet;
import gr.hua.dit.ds.shelter.service.AnimalService;
import gr.hua.dit.ds.shelter.service.ShelterService;
import gr.hua.dit.ds.shelter.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

@Controller
@RequestMapping("shelter")
public class ShelterController {

    private final ShelterService shelterService;
    private final UserService userService;
    private final AnimalService animalService;

    public ShelterController(ShelterService shelterService, UserService userService, AnimalService animalService) {
        this.shelterService = shelterService;
        this.userService = userService;
        this.animalService = animalService;
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("")
    public String showShelters(Model model) {
        model.addAttribute("shelters", shelterService.getShelters());
        return "shelter/shelters";
    }

    @PreAuthorize("isAuthenticated() and principal instanceof T(gr.hua.dit.ds.shelter.service.CustomUserDetails)")
    @GetMapping("/{id}")
    public String showShelter(@PathVariable Integer id,
                              @AuthenticationPrincipal gr.hua.dit.ds.shelter.service.CustomUserDetails currentUser,
                              Model model) {
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        User myCurentUser = (User) userService.getUser(currentUser.getUserId().longValue());

        if (!isAdmin && !id.equals(currentUser.getUserId()) && !myCurentUser.isVisitor()){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view this page");
        }

        User user = (User) userService.getUser(id.longValue());
        Shelter shelter = user.getShelter();
        if (shelter == null && (isAdmin || myCurentUser.isVisitor())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User has not set the profile yet");
        }

        Long shelterId = (user.getShelter() != null) ? user.getShelter().getId() : null;

        if (shelterId != null) {
            model.addAttribute("shelter", shelterService.getShelter(shelterId));
            model.addAttribute("id", id);
            return "shelter/shelter";
        }

        model.addAttribute("shelter", new Shelter());
        model.addAttribute("id", id);
        return "shelter/shelter";
    }

    @PreAuthorize("isAuthenticated() and principal instanceof T(gr.hua.dit.ds.shelter.service.CustomUserDetails)")
    @PostMapping("/{id}")
    public String saveShelter(@PathVariable Long id,
                              @Valid @ModelAttribute("shelter") Shelter shelter,
                              BindingResult bindingResult,
                              @AuthenticationPrincipal gr.hua.dit.ds.shelter.service.CustomUserDetails currentUser,
                              Model model) {

        User user = (User) userService.getUser(currentUser.getUserId().longValue());

        if (user.isAdmin()) {
            System.out.println("Into ADMIN 1");
            var userShelter = (User) userService.getUser(id);
            if (userShelter == null){
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User has not set the profile yet");
            }
            System.out.println("Into ADMIN 2");
            Shelter existing = userShelter.getShelter();
            if (existing == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User has not set the profile yet");
            }
            System.out.println("Into ADMIN 3");
            System.out.println("Existing shelter: " + existing.getId());
            userService.toggleUserRole(userShelter, "ROLE_SHELTER");
            Shelter updated = shelterService.toggleAuthorization(existing);
            model.addAttribute("shelter", updated);
        } else if (!bindingResult.hasErrors()) {
            var userShelter = (User) userService.getUser(id);
            if (userShelter.getShelter() != null){
                shelter.setAuthorized(userShelter.getShelter().getAuthorized());
            }
            userService.toggleUserRole(userShelter, "ROLE_SHELTER");
            Shelter saved = shelterService.upsertForUser(user, shelter);
            if (Boolean.FALSE.equals(saved.getAuthorized())) {
                saved.setVet(null);
            }
            model.addAttribute("shelter", saved);
        } else {
            model.addAttribute("shelter", shelter);
        }

        model.addAttribute("id", id.intValue());
        return "shelter/shelter";
    }

    @PreAuthorize("isAuthenticated() and principal instanceof T(gr.hua.dit.ds.shelter.service.CustomUserDetails)")
    @Transactional(readOnly = true)
    @GetMapping("/{id}/animals")
    public String showShelterAnimals(@PathVariable Integer id,
                                     @AuthenticationPrincipal gr.hua.dit.ds.shelter.service.CustomUserDetails currentUser,
                                     Model model) {
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!isAdmin && !id.equals(currentUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view this page");
        }

        User user = (User) userService.getUser(id.longValue());
        Shelter shelter = user.getShelter();
        if (shelter == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User has not set the profile yet");
        }

        System.out.println("Shelter: " + shelter.getId());
        System.out.println("Shelter animal size: "+shelter.getAnimals().size());
        model.addAttribute("shelter", shelter);
        model.addAttribute("animals", shelter.getAnimals());
        model.addAttribute("id", id);
        return "shelter/shelter_animals";
    }

    @PreAuthorize("isAuthenticated() and principal instanceof T(gr.hua.dit.ds.shelter.service.CustomUserDetails)")
    @Transactional(readOnly = true)
    @GetMapping("/{id}/pending_animals")
    public String showShelterPendingAnimals(@PathVariable Integer id,
                                            @AuthenticationPrincipal gr.hua.dit.ds.shelter.service.CustomUserDetails currentUser,
                                            Model model) {
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!isAdmin && !id.equals(currentUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view this page");
        }

        User user = (User) userService.getUser(id.longValue());
        Shelter shelter = user.getShelter();
        if (shelter == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User has not set the profile yet");
        }

        // Bundle animal and its planned visitor into a single row structure to avoid parallel lists in the view.
        record PendingVisitRow(gr.hua.dit.ds.shelter.entities.Animal animal,
                               gr.hua.dit.ds.shelter.entities.Visitor visitor,
                               Long userid) {}

        var rows = new java.util.ArrayList<PendingVisitRow>();

        for (var animal : shelter.getAnimals()) {
            if (java.lang.Boolean.TRUE.equals(animal.getRequestToBeVisited())) {
                var visitor = animal.getVisitorPlanningVisit(); // may be null
                // Safe logging to avoid NPEs
                System.out.println("Visitor to visit: " + (visitor != null ? visitor.getId() : "none"));
                rows.add(new PendingVisitRow(animal, visitor, visitor.getUserVisitor().getId().longValue()));
            }
        }

        model.addAttribute("shelter", shelter);
        model.addAttribute("rows", rows);
        model.addAttribute("id", id);
        return "shelter/shelter_animals_pending_to_be_visited";
    }

    @PreAuthorize("isAuthenticated() and principal instanceof T(gr.hua.dit.ds.shelter.service.CustomUserDetails)")
    @Transactional
    @GetMapping("/{id}/acceptAnimalVisit/{idanim}")
    public String acceptVisitAnimal(@PathVariable Integer id,
                                    @PathVariable Integer idanim,
                                    @AuthenticationPrincipal gr.hua.dit.ds.shelter.service.CustomUserDetails currentUser,
                                    Model model) {
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        System.out.println("Accepting visit for animal: "+idanim);
        System.out.println("Shelter: "+id);
        if (!isAdmin && !id.equals(currentUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view this page");
        }

        User user = (User) userService.getUser(id.longValue());
        Shelter shelter = user.getShelter();
        if (shelter == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User has not set the profile yet");
        }

        Animal specificAnimal = animalService.getAnimal(idanim.longValue());
        specificAnimal.setToBeVisited(true);
        animalService.saveAnimal(specificAnimal);

        // Bundle animal and its planned visitor into a single row structure to avoid parallel lists in the view.
        record PendingVisitRow(gr.hua.dit.ds.shelter.entities.Animal animal,
                               gr.hua.dit.ds.shelter.entities.Visitor visitor,
                               Long userid) {}

        var rows = new java.util.ArrayList<PendingVisitRow>();

        for (var animal : shelter.getAnimals()) {
            if (java.lang.Boolean.TRUE.equals(animal.getRequestToBeVisited())) {
                var visitor = animal.getVisitorPlanningVisit(); // may be null
                // Safe logging to avoid NPEs
                System.out.println("Visitor to visit: " + (visitor != null ? visitor.getId() : "none"));
                rows.add(new PendingVisitRow(animal, visitor, visitor.getUserVisitor().getId().longValue()));
            }
        }

        model.addAttribute("shelter", shelter);
        model.addAttribute("rows", rows);
        model.addAttribute("id", id);
        return "shelter/shelter_animals_pending_to_be_visited";
    }

    @PreAuthorize("isAuthenticated() and principal instanceof T(gr.hua.dit.ds.shelter.service.CustomUserDetails)")
    @Transactional
    @GetMapping("/{id}/denyAnimalVisit/{idanim}")
    public String denyVisitAnimal(@PathVariable Integer id,
                                    @PathVariable Integer idanim,
                                    @AuthenticationPrincipal gr.hua.dit.ds.shelter.service.CustomUserDetails currentUser,
                                    Model model) {
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        System.out.println("Accepting visit for animal: "+idanim);
        System.out.println("Shelter: "+id);
        if (!isAdmin && !id.equals(currentUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view this page");
        }

        User user = (User) userService.getUser(id.longValue());
        Shelter shelter = user.getShelter();
        if (shelter == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User has not set the profile yet");
        }

        Animal specificAnimal = animalService.getAnimal(idanim.longValue());
        specificAnimal.setToBeVisited(false);
        specificAnimal.setVisitorPlanningVisit(null);
        specificAnimal.setRequestToBeVisited(false);
        animalService.saveAnimal(specificAnimal);

        // Bundle animal and its planned visitor into a single row structure to avoid parallel lists in the view.
        record PendingVisitRow(gr.hua.dit.ds.shelter.entities.Animal animal,
                               gr.hua.dit.ds.shelter.entities.Visitor visitor,
                               Long userid) {}

        var rows = new java.util.ArrayList<PendingVisitRow>();

        for (var animal : shelter.getAnimals()) {
            if (java.lang.Boolean.TRUE.equals(animal.getRequestToBeVisited())) {
                var visitor = animal.getVisitorPlanningVisit(); // may be null
                // Safe logging to avoid NPEs
                System.out.println("Visitor to visit: " + (visitor != null ? visitor.getId() : "none"));
                rows.add(new PendingVisitRow(animal, visitor, visitor.getUserVisitor().getId().longValue()));
            }
        }

        model.addAttribute("shelter", shelter);
        model.addAttribute("rows", rows);
        model.addAttribute("id", id);
        return "shelter/shelter_animals_pending_to_be_visited";
    }
}