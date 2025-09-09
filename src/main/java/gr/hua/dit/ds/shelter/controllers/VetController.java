package gr.hua.dit.ds.shelter.controllers;

import gr.hua.dit.ds.shelter.entities.Role;
import gr.hua.dit.ds.shelter.entities.Vet;
import gr.hua.dit.ds.shelter.entities.User;

import gr.hua.dit.ds.shelter.service.ShelterService;
import gr.hua.dit.ds.shelter.service.VetService;
import gr.hua.dit.ds.shelter.service.UserService;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("vet")
public class VetController {

    VetService vetService;
    UserService userService;
    ShelterService shelterService;

    public VetController(VetService vetService, UserService userService, ShelterService shelterService)
    {
        this.vetService = vetService;
        this.userService = userService;
        this.shelterService = shelterService;
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("")
    public String showVets(Model model){
        model.addAttribute("vets", vetService.getVets());
        // Provide shelters for the assign dropdown(s) in the view
        var acceptedShelters = shelterService.getShelters()
                .stream()
                .filter(s -> java.lang.Boolean.TRUE.equals(s.getAuthorized()))
                .toList();
        model.addAttribute("shelters", acceptedShelters);
        return "vet/vets"; //TO-DO: Add the view
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/{id}/assign-shelter")
    public String assignShelterToVet(@PathVariable Integer id,
                                     @RequestParam("shelterId") Long shelterId,
                                     org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            // Delegate the actual association to the service layer
            vetService.assignShelter(id, shelterId);
            redirectAttributes.addFlashAttribute("message", "Shelter assigned successfully.");
        } catch (jakarta.persistence.EntityNotFoundException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Unexpected error while assigning shelter.");
        }
        return "redirect:/vet";
    }

    @PreAuthorize("isAuthenticated() and principal instanceof T(gr.hua.dit.ds.shelter.service.CustomUserDetails)")
    @GetMapping("/{id}/animals")
    public String showVetAnimals(@PathVariable Integer id,
                                 @AuthenticationPrincipal gr.hua.dit.ds.shelter.service.CustomUserDetails currentUser,
                                 Model model) {
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (!isAdmin && !id.equals(currentUser.getUserId())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "You are not authorized to view this page");
        }

        User user = (User) userService.getUser(id.longValue());
        Vet vet = user != null ? user.getVet() : null;
        if (vet == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "User has not set the profile yet");
        }

        java.util.List<gr.hua.dit.ds.shelter.entities.Animal> animals = new java.util.ArrayList<>();
        java.util.List<gr.hua.dit.ds.shelter.entities.Shelter> shelters =
                java.util.Optional.ofNullable(vet.getShelterVet()).orElse(java.util.Collections.emptyList());
        for (gr.hua.dit.ds.shelter.entities.Shelter s : shelters) {
            if (s != null && s.getAnimals() != null) {
                animals.addAll(s.getAnimals());
            }
        }

        model.addAttribute("animals", animals);
        model.addAttribute("id", id);
        return "vet/vet_animals";
    }

    @PreAuthorize("isAuthenticated() and principal instanceof T(gr.hua.dit.ds.shelter.service.CustomUserDetails)")
    @GetMapping("/{id}")
    public String showVet(@PathVariable Integer id,
                          @AuthenticationPrincipal gr.hua.dit.ds.shelter.service.CustomUserDetails currentUser,
                          Model model) {
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        if (currentUser.getUserId() != id && !isAdmin){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view this page");
        }

        User user = (User) userService.getUser(id.longValue());

        if (user.getVet() == null && isAdmin){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User has not set the profile yet");
        }

        Integer vetId = (user.getVet() != null) ? user.getVet().getId() : null;

        if (vetId != null) {
            model.addAttribute("vet", vetService.getVet(vetId));
            model.addAttribute("id", id);
            return "vet/vet";
        }

        Vet vet = new Vet();
        model.addAttribute("vet", vet);
        model.addAttribute("id", id);
        return "vet/vet";
    }

    @PreAuthorize("isAuthenticated() and principal instanceof T(gr.hua.dit.ds.shelter.service.CustomUserDetails)")
    @PostMapping("/{id}")
    public String saveVet(@PathVariable Long id,
                          @Valid @ModelAttribute("vet") Vet vet,
                          BindingResult theBindingResult,
                          @AuthenticationPrincipal gr.hua.dit.ds.shelter.service.CustomUserDetails currentUser,
                          Model model) {

        User user = (User) userService.getUser(currentUser.getUserId().longValue());

        if (user.isAdmin()) {
            // Admin toggles authorization on the persisted vet
            var userVet = (User) userService.getUser(id);
            if (userVet == null){
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User has not set the profile yet");
            }
            Vet existing = userVet.getVet();
            if (existing == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User has not set the profile yet");
            }
            userService.toggleUserRole(userVet, "ROLE_VET");
            Vet updated = vetService.toggleAuthorization(existing);
            if (Boolean.FALSE.equals(updated.getAuthorized())) {
                java.util.List<gr.hua.dit.ds.shelter.entities.Shelter> shelters =
                        new java.util.ArrayList<>(
                                java.util.Optional.ofNullable(updated.getShelterVet())
                                        .orElse(java.util.Collections.emptyList())
                        );
                for (gr.hua.dit.ds.shelter.entities.Shelter s : shelters) {
                    if (s != null) {
                        s.setVet(null);
                        shelterService.saveShelter(s);
                    }
                }

                if (updated.getShelterVet() != null) {
                    updated.getShelterVet().clear();
                }
            }
            model.addAttribute("vet", updated);
        } else if (!theBindingResult.hasErrors()) {
            // User flow: upsert while avoiding multiple representations
            var userVet = (User) userService.getUser(id);
            if (userVet.getVet() != null){
                vet.setAuthorized(userVet.getVet().getAuthorized());
            }
            //vet.setAuthorized(userVet.ge);
            Vet saved = vetService.upsertForUser(user, vet);
            model.addAttribute("vet", saved);
        } else {
            // Return form with validation errors
            model.addAttribute("vet", vet);
        }

        model.addAttribute("id", id);
        return "vet/vet";
    }
}