package gr.hua.dit.ds.shelter.controllers;

import gr.hua.dit.ds.shelter.entities.Role;
import gr.hua.dit.ds.shelter.repositories.RoleRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {


    RoleRepository roleRepository;

    public AuthController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @PostConstruct
    public void setup() {
      Role role_user = new Role("ROLE_USER");
      Role role_admin = new Role("ROLE_ADMIN");
      Role role_vet = new Role("ROLE_VET");
      Role role_shelter = new Role("ROLE_SHELTER");
      Role role_visitor = new Role("ROLE_VISITOR");
      roleRepository.updateOrInsert(role_user);
      roleRepository.updateOrInsert(role_admin);
      roleRepository.updateOrInsert(role_vet);
      roleRepository.updateOrInsert(role_shelter);
      roleRepository.updateOrInsert(role_visitor);
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }
}
