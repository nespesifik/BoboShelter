package gr.hua.dit.ds.shelter.controllers;

import gr.hua.dit.ds.shelter.entities.*;
import gr.hua.dit.ds.shelter.repositories.RoleRepository;
import gr.hua.dit.ds.shelter.service.AnimalService;
import gr.hua.dit.ds.shelter.service.UserService;
import gr.hua.dit.ds.shelter.service.VisitorService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;


@Controller
@RequestMapping("visitor")
public class VisitorController {

    private final VisitorService visitorService;
    private final UserService userService;
    private final AnimalService animalService;
    private RoleRepository roleRepository;
    public VisitorController(VisitorService visitorService, UserService userService, AnimalService animalService, RoleRepository roleRepository)
    {
        this.visitorService = visitorService;
        this.userService = userService;
        this.animalService = animalService;
        this.roleRepository = roleRepository;
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("")
    public String showVisitors(Model model) {
        // Aligning with VetController pattern: list view for admins
        model.addAttribute("visitors", visitorService.getVisitors());
        return "visitor/visitors";
    }

    // Here the visitor
    @PreAuthorize("isAuthenticated() and principal instanceof T(gr.hua.dit.ds.shelter.service.CustomUserDetails)")
    @Transactional(readOnly = true)
    @GetMapping("/{id}/search-animals")
    public String searchAnimals(@PathVariable Integer id,
                                @AuthenticationPrincipal gr.hua.dit.ds.shelter.service.CustomUserDetails currentUser,
                                Model model) {
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!id.equals(currentUser.getUserId()) && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view this page");
        }

        List<Animal> userAnimals = new ArrayList<>();
        var animals = animalService.getAnimals();
        for (Animal animal : animals) {
            Visitor visitor = animal.getVisitorPlanningVisit();
            User user = (visitor != null) ? visitor.getUserVisitor() : null;
            if (Boolean.TRUE.equals(animal.getRequestToBeVisited()) && (user.getId() != id)) {
                continue;
            }
            if (!animal.getShelter().getUserShelter().isShelter()) continue;
            //if (!animal.getShelter().getUserShelter().isShelter()) continue;
            if (animal.getStatus() == Animal.Status.ADOPTED) continue;
            if (!animal.isAccepted()) continue;
            userAnimals.add(animal);
        }
        model.addAttribute("animals", userAnimals);
        model.addAttribute("id", id);
        return "visitor/search_animals";
    }

    @PreAuthorize("isAuthenticated() and principal instanceof T(gr.hua.dit.ds.shelter.service.CustomUserDetails)")
    @GetMapping("/{id}/animals")
    public String listRequestedAnimals(@PathVariable Integer id,
                                       @AuthenticationPrincipal gr.hua.dit.ds.shelter.service.CustomUserDetails currentUser,
                                       Model model) {
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!id.equals(currentUser.getUserId()) && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view this page");
        }

        User user = (User) userService.getUser(currentUser.getUserId().longValue());
        Visitor visitor = user.getVisitor();
        if (visitor == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Visitor profile not found");
        }

        var animals = visitor.getPlannedVisits();
        List<Animal> userAnimals = new ArrayList<>();
        for (Animal animal : animals) {
            if (!animal.getShelter().getUserShelter().isShelter()) continue;
            if (animal.getStatus() == Animal.Status.ADOPTED) continue;
            if (!animal.isAccepted()) continue;
            userAnimals.add(animal);
        }
        model.addAttribute("animals", userAnimals != null ? animals : java.util.List.of());
        model.addAttribute("id", id);
        return "visitor/animals";
    }

    @PreAuthorize("isAuthenticated() and principal instanceof T(gr.hua.dit.ds.shelter.service.CustomUserDetails)")
    @GetMapping("/{id}")
    public String showVisitor(@PathVariable Integer id,
                              @AuthenticationPrincipal gr.hua.dit.ds.shelter.service.CustomUserDetails currentUser,
                              Model model) {
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        User user = (User) userService.getUser(currentUser.getUserId().longValue());

        if (!id.equals(currentUser.getUserId()) && !(isAdmin || user.isShelter())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view this page");
        }

        User visitorUser = (User) userService.getUser(id.longValue());
        if (visitorUser == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found for id=" + id);
        }

        Visitor existingVisitor = visitorUser.getVisitor();
        // If admin views a profile but the user has no visitor profile yet
        if (existingVisitor == null && (isAdmin || user.isShelter())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User has not set the profile yet");
        }

        if (existingVisitor != null) {
            model.addAttribute("visitor", existingVisitor);
            model.addAttribute("id", id);
            return "visitor/visitor";
        }

        // No profile yet: return empty form
        model.addAttribute("visitor", new Visitor());
        model.addAttribute("id", id);
        return "visitor/visitor";
    }

    @PreAuthorize("isAuthenticated() and principal instanceof T(gr.hua.dit.ds.shelter.service.CustomUserDetails)")
    @PostMapping("/{id}")
    public String saveVisitor(@PathVariable Long id,
                              @Valid @ModelAttribute("visitor") Visitor visitor,
                              BindingResult bindingResult,
                              @AuthenticationPrincipal gr.hua.dit.ds.shelter.service.CustomUserDetails currentUser,
                              Model model) {

        User user = (User) userService.getUser(id);
        boolean isCurrentVisitor = currentUser.getUserId() == id.longValue();
        if (user.isAdmin()) {
            // Admin flow: show the persisted visitor (no toggle logic for visitors)
            Visitor existing = user.getVisitor();
            if (existing == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User has not set the profile yet");
            }
            model.addAttribute("visitor", existing);
        } else if (!bindingResult.hasErrors() && isCurrentVisitor) {
            // Regular user flow: upsert profile
            Visitor saved = visitorService.upsertForUser(user, visitor);
            model.addAttribute("visitor", saved);
            if (!user.hasRole("ROLE_VISITOR")){
                Role roleVisitor = roleRepository.findByName("ROLE_VISITOR")
                        .orElseThrow(() -> new IllegalStateException("ROLE_VISITOR not found. Seed roles at startup."));
                user.getRoles().add(roleVisitor);
                userService.updateUser(user);
            }
        }else{
            // Validation errors: return form with errors
            model.addAttribute("visitor", visitor);
        }

        model.addAttribute("id", id.intValue());
        return "visitor/visitor";
    }
}
