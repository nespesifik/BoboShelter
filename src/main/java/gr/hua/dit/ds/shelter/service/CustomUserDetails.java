package gr.hua.dit.ds.shelter.service;

import gr.hua.dit.ds.shelter.entities.User;
import gr.hua.dit.ds.shelter.entities.UserType;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;


public class CustomUserDetails extends org.springframework.security.core.userdetails.User {
    private final UserType userType;
    private final Integer userId;
    private final Integer vetId; // null-safe cache of Vet id

    public CustomUserDetails(User user, Collection<? extends GrantedAuthority> authorities) {
        super(user.getUsername(), user.getPassword(), authorities);
        this.userType = user.getUserType();
        this.userId = user.getId();
        this.vetId = (user.getVet() != null) ? user.getVet().getId() : null; // get vet id if vet is not null
    }

    public UserType getUserType() {
        return userType;
    }

    public Integer getUserId() {
        return userId;
    }

    public Integer getVetId() {
        return vetId;
    }

}

