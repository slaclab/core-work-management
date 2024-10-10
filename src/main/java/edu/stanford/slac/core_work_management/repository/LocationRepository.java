package edu.stanford.slac.core_work_management.repository;

import edu.stanford.slac.core_work_management.model.Location;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends MongoRepository<Location, String>, LocationRepositoryCustom {

    /**
     * Check if a location exists by domain id and id.
     * @param domainId the domain id
     * @param id the id
     * @return true if the location exists, false otherwise
     */
    boolean existsByDomainIdAndId(String domainId, String id);

    /**
     * Find a location by domain id and id.
     *
     * @param domainId the domain id
     * @param id       the id
     * @return the location
     */
    Optional<Location> findByDomainIdAndId(String domainId, String id);

    /**
     * Find all the location up to root
     *
     * @param locationId the id of the location
     * @return the location
     */
    @Aggregation(pipeline = {
            "{ $match: { 'id': ?0 } }",
            "{ $graphLookup: { from: 'location', startWith: '$parentId', connectFromField: 'parentId', connectToField: '_id', as: 'pathToRoot', depthField: 'depth' } }",
            "{ $unwind: '$pathToRoot' }",
            "{ $project: { 'pathToRoot': 1, '_id': '0' } }",
            "{ $replaceRoot: { newRoot: '$pathToRoot' } }",
            "{ $sort: { 'depth': 1 } }" // Sort the results by depth in ascending order
    })
    List<Location> findPathToRoot(String locationId);

    // Downward Path Aggregation to get _id list
    @Aggregation(pipeline = {
            "{ $match: { 'id': ?0 } }",
            "{ $graphLookup: { from: 'location', startWith: '$_id', connectFromField: '_id', connectToField: 'parentId', as: 'pathToLeaf', depthField: 'depth' } }",
            "{ $unwind: '$pathToLeaf' }",
            "{ $project: { 'pathToLeaf': 1, '_id': '0' } }",
            "{ $replaceRoot: { newRoot: '$pathToLeaf' } }",
            "{ $sort: { 'depth': 1, 'name': 1 } }" // Sort the results by depth and name in descending order
    })
    List<Location> findIdPathToLeaf(String locationId);
}
