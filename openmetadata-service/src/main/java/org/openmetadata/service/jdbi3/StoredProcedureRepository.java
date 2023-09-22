package org.openmetadata.service.jdbi3;

import static org.openmetadata.schema.type.Include.ALL;
import static org.openmetadata.service.Entity.DATABASE_SCHEMA;
import static org.openmetadata.service.Entity.FIELD_FOLLOWERS;
import static org.openmetadata.service.Entity.STORED_PROCEDURE;

import org.openmetadata.schema.entity.data.DatabaseSchema;
import org.openmetadata.schema.entity.data.StoredProcedure;
import org.openmetadata.schema.type.EntityReference;
import org.openmetadata.schema.type.Relationship;
import org.openmetadata.service.Entity;
import org.openmetadata.service.resources.databases.StoredProcedureResource;
import org.openmetadata.service.util.EntityUtil;
import org.openmetadata.service.util.FullyQualifiedName;

public class StoredProcedureRepository extends EntityRepository<StoredProcedure> {
  static final String PATCH_FIELDS = "storedProcedureCode,sourceUrl";
  static final String UPDATE_FIELDS = "storedProcedureCode,sourceUrl";

  public StoredProcedureRepository(CollectionDAO dao) {
    super(
        StoredProcedureResource.COLLECTION_PATH,
        STORED_PROCEDURE,
        StoredProcedure.class,
        dao.storedProcedureDAO(),
        dao,
        PATCH_FIELDS,
        UPDATE_FIELDS);
    supportsSearchIndex = true;
  }

  @Override
  public void setFullyQualifiedName(StoredProcedure storedProcedure) {
    storedProcedure.setFullyQualifiedName(
        FullyQualifiedName.add(storedProcedure.getDatabaseSchema().getFullyQualifiedName(), storedProcedure.getName()));
  }

  @Override
  public void prepare(StoredProcedure storedProcedure, boolean update) {
    DatabaseSchema schema = Entity.getEntity(storedProcedure.getDatabaseSchema(), "", ALL);
    storedProcedure
        .withDatabaseSchema(schema.getEntityReference())
        .withDatabase(schema.getDatabase())
        .withService(schema.getService())
        .withServiceType(schema.getServiceType());
  }

  @Override
  public void storeEntity(StoredProcedure storedProcedure, boolean update) {
    // Relationships and fields such as service are derived and not stored as part of json
    EntityReference service = storedProcedure.getService();
    storedProcedure.withService(null);
    store(storedProcedure, update);
    storedProcedure.withService(service);
  }

  @Override
  public void storeRelationships(StoredProcedure storedProcedure) {
    addRelationship(
        storedProcedure.getDatabaseSchema().getId(),
        storedProcedure.getId(),
        DATABASE_SCHEMA,
        STORED_PROCEDURE,
        Relationship.CONTAINS);
  }

  @Override
  public StoredProcedure setInheritedFields(StoredProcedure storedProcedure, EntityUtil.Fields fields) {
    DatabaseSchema schema =
        Entity.getEntity(DATABASE_SCHEMA, storedProcedure.getDatabaseSchema().getId(), "owner,domain", ALL);
    inheritOwner(storedProcedure, fields, schema);
    inheritDomain(storedProcedure, fields, schema);
    return storedProcedure;
  }

  @Override
  public StoredProcedure setFields(StoredProcedure storedProcedure, EntityUtil.Fields fields) {
    setDefaultFields(storedProcedure);
    storedProcedure.setFollowers(fields.contains(FIELD_FOLLOWERS) ? getFollowers(storedProcedure) : null);
    return storedProcedure;
  }

  @Override
  public StoredProcedure clearFields(StoredProcedure storedProcedure, EntityUtil.Fields fields) {
    return storedProcedure;
  }

  private void setDefaultFields(StoredProcedure storedProcedure) {
    EntityReference schemaRef = getContainer(storedProcedure.getId());
    DatabaseSchema schema = Entity.getEntity(schemaRef, "", ALL);
    storedProcedure.withDatabaseSchema(schemaRef).withDatabase(schema.getDatabase()).withService(schema.getService());
  }

  @Override
  public StoredProcedureUpdater getUpdater(StoredProcedure original, StoredProcedure updated, Operation operation) {
    return new StoredProcedureUpdater(original, updated, operation);
  }

  public void setService(StoredProcedure storedProcedure, EntityReference service) {
    if (service != null && storedProcedure != null) {
      addRelationship(
          service.getId(), storedProcedure.getId(), service.getType(), STORED_PROCEDURE, Relationship.CONTAINS);
      storedProcedure.setService(service);
    }
  }

  public class StoredProcedureUpdater extends EntityUpdater {
    public StoredProcedureUpdater(StoredProcedure original, StoredProcedure updated, Operation operation) {
      super(original, updated, operation);
    }

    @Override
    public void entitySpecificUpdate() {
      // storedProcedureCode is a required field. Cannot be null.
      if (updated.getStoredProcedureCode() != null) {
        recordChange("storedProcedureCode", original.getStoredProcedureCode(), updated.getStoredProcedureCode());
      }
      recordChange("sourceUrl", original.getSourceUrl(), updated.getSourceUrl());
    }
  }
}
