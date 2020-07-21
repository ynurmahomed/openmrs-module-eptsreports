package org.openmrs.module.eptsreports.reporting.calculation.txcurr;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.module.eptsreports.metadata.HivMetadata;
import org.openmrs.module.eptsreports.reporting.calculation.AbstractPatientCalculation;
import org.openmrs.module.eptsreports.reporting.calculation.BooleanResult;
import org.openmrs.module.eptsreports.reporting.calculation.common.EPTSCalculationService;
import org.openmrs.module.eptsreports.reporting.utils.EptsCalculationUtils;
import org.openmrs.module.reporting.common.TimeQualifier;
import org.springframework.stereotype.Component;

@Component
public class SixMonthsAndAboveOnArvDispensationCalculation extends AbstractPatientCalculation {

  @Override
  public CalculationResultMap evaluate(
      Collection<Integer> cohort, Map<String, Object> map, PatientCalculationContext context) {

    HivMetadata hivMetadata = Context.getRegisteredComponents(HivMetadata.class).get(0);
    EPTSCalculationService ePTSCalculationService =
        Context.getRegisteredComponents(EPTSCalculationService.class).get(0);

    CalculationResultMap resultMap = new CalculationResultMap();

    Location location = (Location) context.getFromCache("location");
    Date onOrBefore = (Date) context.getFromCache("onOrBefore");

    EncounterType fila = hivMetadata.getARVPharmaciaEncounterType();
    EncounterType ficha = hivMetadata.getAdultoSeguimentoEncounterType();

    Concept returnVisitDateForArvDrugs = hivMetadata.getReturnVisitDateForArvDrugConcept();
    Concept typeOfDispensation = hivMetadata.getTypeOfDispensationConcept();
    Concept completedConcept = hivMetadata.getCompletedConcept();
    Concept quaterly = hivMetadata.getQuarterlyDispensation();
    Concept dispensaSemestra = hivMetadata.getSemiannualDispensation();
    Concept startDrugs = hivMetadata.getStartDrugs();
    Concept continueRegimen = hivMetadata.getContinueRegimenConcept();

    CalculationResultMap getLastFila =
        ePTSCalculationService.getObs(
            returnVisitDateForArvDrugs,
            Arrays.asList(fila),
            cohort,
            Arrays.asList(location),
            null,
            TimeQualifier.LAST,
            null,
            onOrBefore,
            context);
    CalculationResultMap getLastFicha =
        ePTSCalculationService.getObs(
            typeOfDispensation,
            Arrays.asList(ficha),
            cohort,
            Arrays.asList(location),
            null,
            TimeQualifier.LAST,
            null,
            onOrBefore,
            context);
    CalculationResultMap lastDispensaTrimestralMap =
        ePTSCalculationService.getObs(
            quaterly,
            Arrays.asList(ficha),
            cohort,
            Arrays.asList(location),
            null,
            TimeQualifier.LAST,
            null,
            onOrBefore,
            context);
    CalculationResultMap lastDispensaSemestraMap =
        ePTSCalculationService.getObs(
            dispensaSemestra,
            Arrays.asList(ficha),
            cohort,
            Arrays.asList(location),
            null,
            TimeQualifier.LAST,
            null,
            onOrBefore,
            context);
    CalculationResultMap lastFichaEncounterMap =
        ePTSCalculationService.getEncounter(
            Arrays.asList(ficha), TimeQualifier.LAST, cohort, location, onOrBefore, context);
    for (Integer pId : cohort) {
      boolean found = false;
      Obs lastFilaObs = EptsCalculationUtils.obsResultForPatient(getLastFila, pId);
      Obs lastFichaObs = EptsCalculationUtils.obsResultForPatient(getLastFicha, pId);
      Obs lastDispensaTrimestralObs =
          EptsCalculationUtils.obsResultForPatient(lastDispensaTrimestralMap, pId);
      Obs lastSemiQuartelyObs =
          EptsCalculationUtils.obsResultForPatient(lastDispensaSemestraMap, pId);
      Encounter lastFichaEncounter =
          EptsCalculationUtils.resultForPatient(lastFichaEncounterMap, pId);

      Date returnDateForDrugPickup = null;
      Date filaEncounterDate = null;
      Date lastFichaEncounterDate = null;

      if (lastFilaObs != null
          && lastFilaObs.getEncounter() != null
          && lastFilaObs.getEncounter().getEncounterDatetime() != null
          && lastFilaObs.getValueDatetime() != null) {
        returnDateForDrugPickup = lastFilaObs.getValueDatetime();
        filaEncounterDate = lastFilaObs.getEncounter().getEncounterDatetime();
      }
      if (lastFichaEncounter != null && lastFichaEncounter.getEncounterDatetime() != null) {
        lastFichaEncounterDate = lastFichaEncounter.getEncounterDatetime();
      }
      if (filaEncounterDate != null
          && lastFichaEncounterDate != null
          && returnDateForDrugPickup != null
          && filaEncounterDate.after(lastFichaEncounterDate)
          && EptsCalculationUtils.daysSince(filaEncounterDate, returnDateForDrugPickup) > 173) {
        found = true;

      } else if ((filaEncounterDate != null
              && lastFichaEncounterDate != null
              && lastFichaEncounterDate.after(filaEncounterDate)
              && lastFichaObs != null
              && lastFichaObs.getValueCoded() != null
              && lastFichaObs.getValueCoded().equals(quaterly))
          || (lastDispensaTrimestralObs != null
              && (lastDispensaTrimestralObs.getValueCoded().equals(startDrugs)
                  || lastDispensaTrimestralObs.getValueCoded().equals(continueRegimen)))) {
        found = true;
      } else if (filaEncounterDate != null
          && lastFichaEncounterDate == null
          && returnDateForDrugPickup != null
          && EptsCalculationUtils.daysSince(filaEncounterDate, returnDateForDrugPickup) > 173) {
        found = true;
      } else if ((filaEncounterDate == null
              && lastFichaEncounterDate != null
              && lastFichaObs != null
              && lastFichaObs.getValueCoded().equals(dispensaSemestra))
          || (lastSemiQuartelyObs != null
              && (lastSemiQuartelyObs.getValueCoded().equals(startDrugs)
                  || lastSemiQuartelyObs.getValueCoded().equals(continueRegimen)))) {
        found = true;
      } else if (filaEncounterDate != null
          && returnDateForDrugPickup != null
          && filaEncounterDate.equals(lastFichaEncounterDate)
          && EptsCalculationUtils.daysSince(filaEncounterDate, returnDateForDrugPickup) > 173) {
        found = true;
      }
      // exclude   patients   who   have   the   last   SEMESTRAL   DISPENSATION   (concept
      // id=23888)   with   value   COMPLETED
      // with value_coded as value_coded=1267)
      if (lastSemiQuartelyObs != null
          && lastSemiQuartelyObs.getValueCoded().equals(completedConcept)) {
        found = false;
      }
      resultMap.put(pId, new BooleanResult(found, this));
    }

    return resultMap;
  }
}
