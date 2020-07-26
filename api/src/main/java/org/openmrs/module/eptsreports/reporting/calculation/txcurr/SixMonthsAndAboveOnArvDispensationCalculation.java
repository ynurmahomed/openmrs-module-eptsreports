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

    CalculationResultMap getLastFilaWithReturnVisitForDrugFilledMap =
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
    CalculationResultMap getLastFichaWithSemestaral =
        ePTSCalculationService.getObs(
            typeOfDispensation,
            Arrays.asList(ficha),
            cohort,
            Arrays.asList(location),
            Arrays.asList(dispensaSemestra),
            TimeQualifier.LAST,
            null,
            onOrBefore,
            context);
    CalculationResultMap lastDispensaTrimestralWithCompletedMap =
        ePTSCalculationService.getObs(
            dispensaSemestra,
            Arrays.asList(ficha),
            cohort,
            Arrays.asList(location),
            Arrays.asList(completedConcept),
            TimeQualifier.LAST,
            null,
            onOrBefore,
            context);
    CalculationResultMap lastDispensaSemestraWithStartOrContinueDrugsMap =
        ePTSCalculationService.getObs(
            dispensaSemestra,
            Arrays.asList(ficha),
            cohort,
            Arrays.asList(location),
            Arrays.asList(startDrugs, continueRegimen),
            TimeQualifier.LAST,
            null,
            onOrBefore,
            context);
    CalculationResultMap lastFichaEncounterMap =
        ePTSCalculationService.getEncounter(
            Arrays.asList(ficha), TimeQualifier.LAST, cohort, location, onOrBefore, context);
    CalculationResultMap lastFilaEncounterMap =
        ePTSCalculationService.getEncounter(
            Arrays.asList(fila), TimeQualifier.LAST, cohort, location, onOrBefore, context);
    for (Integer pId : cohort) {
      boolean found = false;
      // get last encounters
      Encounter lastFichaEncounter =
          EptsCalculationUtils.resultForPatient(lastFichaEncounterMap, pId);
      Encounter lastFilaEncounter =
          EptsCalculationUtils.resultForPatient(lastFilaEncounterMap, pId);

      Obs lastFilaWithReturnForDrugsObs =
          EptsCalculationUtils.obsResultForPatient(getLastFilaWithReturnVisitForDrugFilledMap, pId);
      Obs lastFichaObsWithSemestarlValueCoded =
          EptsCalculationUtils.obsResultForPatient(getLastFichaWithSemestaral, pId);
      Obs lastDispensaTrimestralWithCompltedObs =
          EptsCalculationUtils.obsResultForPatient(lastDispensaTrimestralWithCompletedMap, pId);
      Obs lastDispensaSemestraWithStartOrContinueDrugsObs =
          EptsCalculationUtils.obsResultForPatient(
              lastDispensaSemestraWithStartOrContinueDrugsMap, pId);

      // case 1 fila filled is after ficha filled with semestral concept id
      if (lastFilaEncounter != null
          && lastFilaEncounter.getEncounterDatetime() != null
          && lastFichaEncounter != null
          && lastFichaEncounter.getEncounterDatetime() != null
          && lastFilaWithReturnForDrugsObs != null
          && lastFilaEncounter.equals(lastFilaWithReturnForDrugsObs.getEncounter())
          && lastFichaObsWithSemestarlValueCoded != null
          && lastFichaEncounter.equals(lastFichaObsWithSemestarlValueCoded.getEncounter())
          && lastFilaEncounter
              .getEncounterDatetime()
              .after(lastFichaEncounter.getEncounterDatetime())
          && EptsCalculationUtils.daysSince(
                  lastFilaEncounter.getEncounterDatetime(),
                  lastFilaWithReturnForDrugsObs.getValueDatetime())
              > 173) {
        found = true;

      }
      // case 2 fila filled is after ficha filled with start or continue regimen concept id
      else if (lastFilaEncounter != null
          && lastFilaEncounter.getEncounterDatetime() != null
          && lastFichaEncounter != null
          && lastFichaEncounter.getEncounterDatetime() != null
          && lastFilaWithReturnForDrugsObs != null
          && lastFilaEncounter.equals(lastFilaWithReturnForDrugsObs.getEncounter())
          && lastDispensaSemestraWithStartOrContinueDrugsObs != null
          && lastFichaEncounter.equals(
              lastDispensaSemestraWithStartOrContinueDrugsObs.getEncounter())
          && lastFilaEncounter
              .getEncounterDatetime()
              .after(lastFichaEncounter.getEncounterDatetime())
          && EptsCalculationUtils.daysSince(
                  lastFilaEncounter.getEncounterDatetime(),
                  lastFilaWithReturnForDrugsObs.getValueDatetime())
              > 173) {
        found = true;

      }
      // case 3 ficha filled is after fila filled with semestral concept id reverse of 1
      else if (lastFilaEncounter != null
          && lastFichaEncounter != null
          && lastFichaEncounter.getEncounterDatetime() != null
          && lastFilaWithReturnForDrugsObs != null
          && lastFilaEncounter.equals(lastFilaWithReturnForDrugsObs.getEncounter())
          && lastFichaObsWithSemestarlValueCoded != null
          && lastFichaEncounter.equals(lastFichaObsWithSemestarlValueCoded.getEncounter())
          && lastFichaEncounter
              .getEncounterDatetime()
              .after(lastFilaEncounter.getEncounterDatetime())) {
        found = true;
      }
      // case 4 ficha filled is after fila filled with start or continue regimen concept id
      else if (lastFilaEncounter != null
          && lastFichaEncounter != null
          && lastFichaEncounter.getEncounterDatetime() != null
          && lastFilaWithReturnForDrugsObs != null
          && lastFilaEncounter.equals(lastFilaWithReturnForDrugsObs.getEncounter())
          && lastDispensaSemestraWithStartOrContinueDrugsObs != null
          && lastFichaEncounter.equals(
              lastDispensaSemestraWithStartOrContinueDrugsObs.getEncounter())
          && lastFichaEncounter
              .getEncounterDatetime()
              .after(lastFilaEncounter.getEncounterDatetime())) {
        found = true;

      }
      // case 5 if there are multiple fila filled for the same date, pick the latest that has
      // information filled
      else if (lastFilaEncounter != null
          && lastFilaWithReturnForDrugsObs != null
          && lastFilaEncounter.equals(lastFilaWithReturnForDrugsObs.getEncounter())
          && EptsCalculationUtils.daysSince(
                  lastFilaEncounter.getEncounterDatetime(),
                  lastFilaWithReturnForDrugsObs.getValueDatetime())
              > 173) {
        found = true;
      }
      // case 6 if ficha filled is after fila filled with semestral concept id
      else if (lastFichaEncounter != null
          && lastFichaObsWithSemestarlValueCoded != null
          && lastFichaEncounter.equals(lastFichaObsWithSemestarlValueCoded.getEncounter())) {
        found = true;
      }
      // case 7 if ficha filled is after fila filled with start and continue regimen concept id
      else if (lastFichaEncounter != null
          && lastDispensaSemestraWithStartOrContinueDrugsObs != null
          && lastFichaEncounter.equals(
              lastDispensaSemestraWithStartOrContinueDrugsObs.getEncounter())) {
        found = true;
      }
      // case 8 if there is a fila filled with ficha filled with semestral concept filled on the
      // same date
      // we will end up picking the fila
      else if (lastFilaEncounter != null
          && lastFichaEncounter != null
          && lastFichaEncounter.getEncounterDatetime() != null
          && lastFilaWithReturnForDrugsObs != null
          && lastFilaEncounter.equals(lastFilaWithReturnForDrugsObs.getEncounter())
          && lastFichaObsWithSemestarlValueCoded != null
          && lastFichaEncounter.equals(lastFichaObsWithSemestarlValueCoded.getEncounter())
          && lastFichaEncounter
              .getEncounterDatetime()
              .equals(lastFilaEncounter.getEncounterDatetime())
          && EptsCalculationUtils.daysSince(
                  lastFilaEncounter.getEncounterDatetime(),
                  lastFilaWithReturnForDrugsObs.getValueDatetime())
              > 173) {
        found = true;
      }
      // case 9: if there is fila filled and ficha filled with start and continue regimen concept id
      // on the date date we consider fila
      else if (lastFilaEncounter != null
          && lastFichaEncounter != null
          && lastFichaEncounter.getEncounterDatetime() != null
          && lastFilaWithReturnForDrugsObs != null
          && lastFilaEncounter.equals(lastFilaWithReturnForDrugsObs.getEncounter())
          && lastDispensaSemestraWithStartOrContinueDrugsObs != null
          && lastFichaEncounter.equals(
              lastDispensaSemestraWithStartOrContinueDrugsObs.getEncounter())
          && lastFichaEncounter
              .getEncounterDatetime()
              .equals(lastFilaEncounter.getEncounterDatetime())
          && EptsCalculationUtils.daysSince(
                  lastFilaEncounter.getEncounterDatetime(),
                  lastFilaWithReturnForDrugsObs.getValueDatetime())
              > 173) {
        found = true;
      }
      // case 10:
      if (lastFichaEncounter != null
          && lastDispensaTrimestralWithCompltedObs != null
          && lastFichaEncounter.equals(lastDispensaTrimestralWithCompltedObs.getEncounter())) {
        found = false;
      }

      resultMap.put(pId, new BooleanResult(found, this));
    }

    return resultMap;
  }
}
