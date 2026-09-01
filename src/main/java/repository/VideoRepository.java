package repository;

import model.VideoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**Interacts with local DB*/

@Repository
public interface VideoRepository extends JpaRepository<VideoMetadata, String> {
    /**
     * Spring Data JPA derived query method.
     * Spring generates the implementation automatically from the method name at startup
     */
    List<VideoMetadata> findByTitleContainingIgnoreCase(String title);


}
