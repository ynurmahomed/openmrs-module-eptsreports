package org.openmrs.module.eptsreports.reporting.calculation.generic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.openmrs.Concept;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.module.eptsreports.metadata.HivMetadata;
import org.openmrs.module.eptsreports.reporting.calculation.AbstractPatientCalculation;
import org.openmrs.module.eptsreports.reporting.calculation.BooleanResult;
import org.openmrs.module.eptsreports.reporting.calculation.common.EPTSCalculationService;
import org.openmrs.module.eptsreports.reporting.utils.EptsCalculationUtils;
import org.openmrs.module.reporting.common.ListMap;
import org.openmrs.module.reporting.data.person.definition.PersonAttributeDataDefinition;
import org.springframework.stereotype.Component;

@Component
public class KeyPopulationCalculation extends AbstractPatientCalculation {

  public static final String TYPE = "type";

  // Key population source by precedence in ascending order
  enum KeyPopSource {
    PERSON_ATTRIBUTE,
    APSS_FORM,
    ADULTO_FORM;
  }

  public enum KeyPop {
    DRUG_USER,
    HOMOSEXUAL,
    PRISONER,
    SEX_WORKER;

    public static KeyPop of(Concept concept) {
      HivMetadata hivMetadata = Context.getRegisteredComponents(HivMetadata.class).get(0);
      if (concept.equals(hivMetadata.getHomosexualConcept())) {
        return HOMOSEXUAL;
      } else if (concept.equals(hivMetadata.getDrugUseConcept())) {
        return DRUG_USER;
      } else if (concept.equals(hivMetadata.getImprisonmentConcept())) {
        return PRISONER;
      } else if (concept.equals(hivMetadata.getSexWorkerConcept())) {
        return SEX_WORKER;
      }
      return null;
    }

    public static KeyPop of(PersonAttribute personAttribute) {
      switch (personAttribute.getValue().toUpperCase()) {
        case "MSM":
        case "HSH":
          return HOMOSEXUAL;
        case "PID":
          return DRUG_USER;
        case "PRISONER":
        case "RC":
        case "REC":
          return PRISONER;
        case "CSW":
        case "TS":
        case "MTS":
        case "FSW":
          return SEX_WORKER;
        default:
      }
      return null;
    }
  }

  static class KeyPopAndSource implements Comparable<KeyPopAndSource> {

    private KeyPop keyPop;
    private KeyPopSource source;

    KeyPopAndSource(KeyPop keyPop, KeyPopSource source) {
      this.keyPop = keyPop;
      this.source = source;
    }

    @Override
    public int compareTo(KeyPopAndSource keyPopAndSource) {
      return this.source.compareTo(keyPopAndSource.source);
    }

    KeyPop getKeyPop() {
      return this.keyPop;
    }
  }

  @Override
  public CalculationResultMap evaluate(
      Collection<Integer> cohort,
      Map<String, Object> parameterValues,
      PatientCalculationContext context) {

    CalculationResultMap resultMap = new CalculationResultMap();

    Location location = (Location) context.getFromCache("location");
    Date onOrBefore = (Date) context.getFromCache("onOrBefore");

    CalculationResultMap adultoSeguimento =
        getAdultoSeguimento(cohort, context, location, onOrBefore);
    CalculationResultMap apssPrevencaoPositiva =
        getApssPrevencaoPositiva(cohort, context, location, onOrBefore);
    CalculationResultMap personAttribute = getPersonAttribute(cohort, context);

    KeyPop type = (KeyPop) parameterValues.get(TYPE);

    for (Integer pId : cohort) {
      boolean equals = false;

      KeyPop patientKeyPop =
          getAssignedKeyPop(pId, adultoSeguimento, apssPrevencaoPositiva, personAttribute);
      if (type != null && type.equals(patientKeyPop)) {
        equals = true;
      }
      resultMap.put(pId, new BooleanResult(equals, this));
    }

    return resultMap;
  }

  private KeyPop getAssignedKeyPop(
      Integer pId,
      CalculationResultMap adultoSeguimento,
      CalculationResultMap apssPrevencaoPositiva,
      CalculationResultMap personAttribute) {

    ListMap<Date, KeyPopAndSource> keyPopByDate = new ListMap<>(true);

    if (!adultoSeguimento.isEmpty(pId)) {
      Obs obs = adultoSeguimento.get(pId).asType(Obs.class);
      Date date;
      KeyPop keypop;
      if (obs != null
          && obs.getEncounter() != null
          && obs.getEncounter().getEncounterDatetime() != null
          && obs.getValueCoded() != null) {
        date = obs.getEncounter().getEncounterDatetime();
        keypop = KeyPop.of(obs.getValueCoded());
        keyPopByDate.putInList(date, new KeyPopAndSource(keypop, KeyPopSource.ADULTO_FORM));
      }
    }

    if (!personAttribute.isEmpty(pId)) {
      PersonAttribute attr = personAttribute.get(pId).asType(PersonAttribute.class);
      Date date = attr.getDateCreated();
      try {
        KeyPop keypop = KeyPop.of(attr);
        keyPopByDate.putInList(date, new KeyPopAndSource(keypop, KeyPopSource.PERSON_ATTRIBUTE));
      } catch (IllegalArgumentException e) {
        // Ignore unmapped key population string
      }
    }

    if (!apssPrevencaoPositiva.isEmpty(pId)) {
      Obs obs = apssPrevencaoPositiva.get(pId).asType(Obs.class);
      Date date;
      KeyPop keypop;
      if (obs != null
          && obs.getEncounter() != null
          && obs.getEncounter().getEncounterDatetime() != null
          && obs.getValueCoded() != null) {
        date = obs.getEncounter().getEncounterDatetime();
        keypop = KeyPop.of(obs.getValueCoded());
        keyPopByDate.putInList(date, new KeyPopAndSource(keypop, KeyPopSource.APSS_FORM));
      }
    }

    KeyPop assignedKeyPop = null;
    if (!keyPopByDate.isEmpty()) {
      Date maxDate = Collections.max(keyPopByDate.keySet());
      List<KeyPopAndSource> keyPops = keyPopByDate.get(maxDate);
      assignedKeyPop = Collections.max(keyPops).getKeyPop();
    }

    return assignedKeyPop;
  }

  private CalculationResultMap getPersonAttribute(
      Collection<Integer> cohort, PatientCalculationContext context) {
    PersonAttributeDataDefinition definition = new PersonAttributeDataDefinition();
    HivMetadata hivMetadata = Context.getRegisteredComponents(HivMetadata.class).get(0);
    PersonAttributeType identificadorDefinidoLocalmente01 =
        hivMetadata.getIdentificadorDefinidoLocalmente01();
    definition.setPersonAttributeType(identificadorDefinidoLocalmente01);
    return EptsCalculationUtils.evaluateWithReporting(definition, cohort, null, null, context);
  }

  private CalculationResultMap getAdultoSeguimento(
      Collection<Integer> cohort,
      PatientCalculationContext context,
      Location location,
      Date endDate) {
    EPTSCalculationService eptsCalculationService =
        Context.getRegisteredComponents(EPTSCalculationService.class).get(0);
    HivMetadata hivMetadata = Context.getRegisteredComponents(HivMetadata.class).get(0);
    ArrayList<EncounterType> encounterTypes = new ArrayList<>();
    encounterTypes.add(hivMetadata.getAdultoSeguimentoEncounterType());
    Concept keyPop = hivMetadata.getKeyPopulationConcept();
    return eptsCalculationService.lastObs(
        encounterTypes, keyPop, location, null, endDate, cohort, context);
  }

  private CalculationResultMap getApssPrevencaoPositiva(
      Collection<Integer> cohort,
      PatientCalculationContext context,
      Location location,
      Date endDate) {
    EPTSCalculationService eptsCalculationService =
        Context.getRegisteredComponents(EPTSCalculationService.class).get(0);
    HivMetadata hivMetadata = Context.getRegisteredComponents(HivMetadata.class).get(0);
    ArrayList<EncounterType> encounterTypes = new ArrayList<>();
    encounterTypes.add(hivMetadata.getPrevencaoPositivaSeguimentoEncounterType());
    Concept keyPop = hivMetadata.getKeyPopulationConcept();
    return eptsCalculationService.lastObs(
        encounterTypes, keyPop, location, null, endDate, cohort, context);
  }
}
