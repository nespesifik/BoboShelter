package gr.hua.dit.ds.shelter.repositories;

import gr.hua.dit.ds.shelter.entities.Shelter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShelterRepository extends JpaRepository<Shelter, Integer> {
}
