package gov.modadw.issuer.service.credential;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.modadw.issuer.config.IntegrationProperties;
import gov.modadw.issuer.entity.ApCredentialType;
import gov.modadw.issuer.repository.ApCredentialTypeRepository;
import gov.modadw.issuer.service.integration.VcHandlerClient;
import gov.modadw.issuer.web.rest.vm.CreateCredentialTypeVM;
import gov.modadw.issuer.web.rest.vm.FuncSwitchVM;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class CredentialTypeService {

  private final VcHandlerClient vcHandlerClient;
  private final ApCredentialTypeRepository repository;
  private final ObjectMapper objectMapper;
  private final IntegrationProperties integrationProperties;

  /** Create a new credential type: calls vc-handler /setseq and saves locally. */
  @Transactional
  public Map<String, Object> createCredentialType(CreateCredentialTypeVM vm)
      throws JsonProcessingException {
    String credentialType = vm.getBusinessId() + "_" + vm.getTypeName();
    log.info("Creating credential type: {}", credentialType);

    if (repository.existsByCredentialType(credentialType)) {
      throw new IllegalArgumentException("Credential type already exists: " + credentialType);
    }

    // Build vcSchema
    Map<String, Object> vcSchema = buildVcSchema(vm);
    // Build metadata
    Map<String, Object> metadata = buildMetadata(credentialType, vm);

    // Build request body for /setseq
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("schemaId", vm.getSchemaId());
    body.put("vcSchema", vcSchema);
    body.put("effectiveTimeUnit", vm.getEffectiveTimeUnit());
    body.put("effectiveTimeValue", vm.getEffectiveTimeValue());
    body.put("metadata", metadata);

    // Call vc-handler
    Map<String, Object> result = vcHandlerClient.createSequence(credentialType, body);

    // Save to local DB
    ApCredentialType entity = new ApCredentialType();
    entity.setCredentialType(credentialType);
    entity.setBusinessId(vm.getBusinessId());
    entity.setSchemaId(vm.getSchemaId());
    entity.setVcSchema(objectMapper.writeValueAsString(vcSchema));
    entity.setEffectiveTimeUnit(vm.getEffectiveTimeUnit());
    entity.setEffectiveTimeValue(vm.getEffectiveTimeValue());
    entity.setMetadata(objectMapper.writeValueAsString(metadata));
    repository.save(entity);

    log.info("Credential type created successfully: {}", credentialType);
    return result;
  }

  /** Delete a credential type: calls vc-handler /delseq and removes local record. */
  @Transactional
  public Map<String, Object> deleteCredentialType(String credentialType)
      throws JsonProcessingException {
    log.info("Deleting credential type: {}", credentialType);

    ApCredentialType entity =
        repository
            .findByCredentialType(credentialType)
            .orElseThrow(
                () -> new IllegalArgumentException("Credential type not found: " + credentialType));

    Map<String, Object> metadata = objectMapper.readValue(entity.getMetadata(), Map.class);
    Map<String, Object> body = Map.of("metadata", metadata);

    Map<String, Object> result = vcHandlerClient.deleteSequence(credentialType, body);

    repository.delete(entity);

    log.info("Credential type deleted successfully: {}", credentialType);
    return result;
  }

  /** Set function switches for a credential type. */
  @Transactional
  public Map<String, Object> setFuncSwitch(String credentialType, FuncSwitchVM vm) {
    log.info("Setting func switch for: {}", credentialType);

    if (vm.isEnableTxCode() && vm.isEnableVcTransfer()) {
      throw new IllegalArgumentException(
          "enable_tx_code and enable_vc_transfer cannot both be true");
    }

    ApCredentialType entity =
        repository
            .findByCredentialType(credentialType)
            .orElseThrow(
                () -> new IllegalArgumentException("Credential type not found: " + credentialType));

    Map<String, Object> switches = new LinkedHashMap<>();
    switches.put("enable_tx_code", vm.isEnableTxCode());
    switches.put("enable_vc_transfer", vm.isEnableVcTransfer());

    Map<String, Object> result = vcHandlerClient.setFuncSwitch(credentialType, switches);

    entity.setEnableTxCode(vm.isEnableTxCode());
    entity.setEnableVcTransfer(vm.isEnableVcTransfer());
    repository.save(entity);

    log.info("Func switch set successfully for: {}", credentialType);
    return result;
  }

  /** List all active credential types. */
  public List<ApCredentialType> listCredentialTypes() {
    return repository.findByStatusOrderByCreatedDateDesc("ACTIVE");
  }

  /** Get a single credential type by name. */
  public ApCredentialType getCredentialType(String credentialType) {
    return repository
        .findByCredentialType(credentialType)
        .orElseThrow(
            () -> new IllegalArgumentException("Credential type not found: " + credentialType));
  }

  /**
   * Build vcSchema from VM fields.
   *
   * <p>Output format matches vc-handler's full JSON Schema structure:
   *
   * <pre>
   * {
   *   "$schema": "https://json-schema.org/draft/2020-12/schema#",
   *   "$id": "{schemaId}",
   *   "title": "{typeName}",
   *   "description": "{typeName} using JsonSchema",
   *   "type": "object",
   *   "properties": {
   *     "credentialSubject": {
   *       "title": "credentialSubject",
   *       "type": "object",
   *       "properties": {
   *         "id": { "title": "id", "type": "string", "format": "uri" },
   *         "fieldName": { "title": "fieldName", "type": "string" },
   *         ...
   *       }
   *     }
   *   }
   * }
   * </pre>
   */
  private Map<String, Object> buildVcSchema(CreateCredentialTypeVM vm) {
    // credentialSubject.properties — "id" is always first (required by vc-handler)
    Map<String, Object> subjectProperties = new LinkedHashMap<>();
    Map<String, Object> idField = new LinkedHashMap<>();
    idField.put("title", "id");
    idField.put("type", "string");
    idField.put("format", "uri");
    subjectProperties.put("id", idField);

    for (CreateCredentialTypeVM.FieldDefinition field : vm.getFields()) {
      Map<String, Object> fieldDef = new LinkedHashMap<>();
      fieldDef.put("title", field.getName());
      fieldDef.put("type", field.getType());
      subjectProperties.put(field.getName(), fieldDef);
    }

    // required list — "id" is always required
    List<String> requiredFields = new java.util.ArrayList<>();
    requiredFields.add("id");
    for (CreateCredentialTypeVM.FieldDefinition field : vm.getFields()) {
      if (field.isRequired()) {
        requiredFields.add(field.getName());
      }
    }

    // credentialSubject
    Map<String, Object> credentialSubject = new LinkedHashMap<>();
    credentialSubject.put("title", "credentialSubject");
    credentialSubject.put("type", "object");
    credentialSubject.put("properties", subjectProperties);
    credentialSubject.put("additionalProperties", false);
    credentialSubject.put("required", requiredFields);

    // top-level properties
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("credentialSubject", credentialSubject);

    // full JSON Schema
    Map<String, Object> vcSchema = new LinkedHashMap<>();
    vcSchema.put("$schema", "https://json-schema.org/draft/2020-12/schema#");
    vcSchema.put("$id", vm.getSchemaId());
    vcSchema.put("title", vm.getTypeName());
    vcSchema.put("description", vm.getTypeName() + " using JsonSchema");
    vcSchema.put("type", "object");
    vcSchema.put("properties", properties);
    return vcSchema;
  }

  /**
   * Build full OID4VCI metadata (credential_issuer_meta) from VM fields.
   *
   * <p>This JSON is stored as-is in credential_issuer_config.credential_issuer_meta and returned
   * by oid4vci-handler's .well-known/openid-credential-issuer endpoint to the wallet.
   */
  private Map<String, Object> buildMetadata(String credentialType, CreateCredentialTypeVM vm) {
    String externalUrl = integrationProperties.getOid4vci().getExternalUrl();
    String issuerBase = externalUrl + "/api/issuer/";

    // credentialSubject — OID4VCI metadata format (mandatory, value_type, display)
    Map<String, Object> credentialSubject = new LinkedHashMap<>();
    for (CreateCredentialTypeVM.FieldDefinition field : vm.getFields()) {
      Map<String, Object> fieldDef = new LinkedHashMap<>();
      fieldDef.put("mandatory", field.isRequired());
      fieldDef.put("value_type", field.getType());
      fieldDef.put("display", List.of(Map.of("name", field.getName(), "locale", "zh-TW")));
      credentialSubject.put(field.getName(), fieldDef);
    }

    // credential_definition
    Map<String, Object> credentialDefinition = new LinkedHashMap<>();
    credentialDefinition.put("type", List.of("VerifiableCredential", vm.getTypeName()));
    credentialDefinition.put("credentialSubject", credentialSubject);

    // proof_types_supported
    Map<String, Object> proofTypesSupported = new LinkedHashMap<>();
    proofTypesSupported.put(
        "jwt", Map.of("proof_signing_alg_values_supported", List.of("ES256")));

    // type config (per credential type)
    Map<String, Object> typeConfig = new LinkedHashMap<>();
    typeConfig.put("format", "jwt_vc_json");
    typeConfig.put("scope", credentialType);
    typeConfig.put("cryptographic_binding_methods_supported", List.of("jwk"));
    typeConfig.put("credential_signing_alg_values_supported", List.of("ES256"));
    typeConfig.put("credential_definition", credentialDefinition);
    typeConfig.put("proof_types_supported", proofTypesSupported);
    typeConfig.put(
        "display", List.of(Map.of("name", credentialType, "locale", "en-US")));

    // credential_configurations_supported
    Map<String, Object> credentialConfigurationsSupported = new LinkedHashMap<>();
    credentialConfigurationsSupported.put(credentialType, typeConfig);

    // full metadata
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("credential_issuer", issuerBase);
    metadata.put("credential_endpoint", issuerBase + "credential");
    metadata.put("credential_configurations_supported", credentialConfigurationsSupported);
    return metadata;
  }
}
