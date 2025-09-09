package gr.hua.dit.ds.shelter.repositories;

import gr.hua.dit.ds.shelter.entities.Visitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VisitorRepository extends JpaRepository<Visitor, Long> {
}

