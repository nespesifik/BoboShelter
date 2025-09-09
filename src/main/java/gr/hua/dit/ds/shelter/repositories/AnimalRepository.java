package gr.hua.dit.ds.shelter.repositories;

import gr.hua.dit.ds.shelter.entities.Animal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, Long> {}