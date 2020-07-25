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
public class ThreeToFiveMonthsOnArtDispensationCalculation extends AbstractPatientCalculation {

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
    Concept dispensaTrimestra = hivMetadata.getQuarterlyDispensation();
    Concept dispensaSemestra = hivMetadata.getSemiannualDispensation();
    Concept startDrugs = hivMetadata.getStartDrugs();
    Concept continueRegimen = hivMetadata.getContinueRegimenConcept();

    CalculationResultMap getLastFilaWithReturnVistForDrugsRecordedMap =
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
    CalculationResultMap getLastTypeOfDispensationWithQuartelyValueMap =
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
    CalculationResultMap getLastdispensaTrimestraMap =
        ePTSCalculationService.getObs(
            dispensaTrimestra,
            Arrays.asList(ficha),
            cohort,
            Arrays.asList(location),
            null,
            TimeQualifier.LAST,
            null,
            onOrBefore,
            context);
    CalculationResultMap getLastQuartelyDispensationWithStarOrContinueDrugsMap =
        ePTSCalculationService.getObs(
            dispensaTrimestra,
            Arrays.asList(ficha),
            cohort,
            Arrays.asList(location),
            Arrays.asList(startDrugs, continueRegimen),
            TimeQualifier.LAST,
            null,
            onOrBefore,
            context);
    CalculationResultMap lastDispensaSemestraWithMontlyMap =
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
    CalculationResultMap lastFilaEncounterMap =
        ePTSCalculationService.getEncounter(
            Arrays.asList(fila), TimeQualifier.LAST, cohort, location, onOrBefore, context);
    CalculationResultMap quartelyWithcompletedConceptMap =
        ePTSCalculationService.getObs(
            dispensaTrimestra,
            Arrays.asList(ficha),
            cohort,
            Arrays.asList(location),
            Arrays.asList(completedConcept),
            TimeQualifier.LAST,
            null,
            onOrBefore,
            context);
    for (Integer pId : cohort) {
      boolean found = false;

      Obs lastFilaObsWithNextDrugPickUp =
          EptsCalculationUtils.obsResultForPatient(
              getLastFilaWithReturnVistForDrugsRecordedMap, pId);
      Obs getLastTypeOfDispensationWithQuartelyObs =
          EptsCalculationUtils.obsResultForPatient(
              getLastTypeOfDispensationWithQuartelyValueMap, pId);
      Obs getLastQuartelyDispensationWithStartOrContinueObs =
          EptsCalculationUtils.obsResultForPatient(
              getLastQuartelyDispensationWithStarOrContinueDrugsMap, pId);
      Encounter lastFichaEncounter =
          EptsCalculationUtils.resultForPatient(lastFichaEncounterMap, pId);
      Obs lastQuartelyObsWithCompleted =
          EptsCalculationUtils.obsResultForPatient(quartelyWithcompletedConceptMap, pId);

      Encounter lastFilaEncounter =
          EptsCalculationUtils.resultForPatient(lastFilaEncounterMap, pId);

      Date returnDateForDrugPickup = null;
      Date filaEncounterDate = null;
      Date lastFichaEncounterDate = null;
      Date lastFilaEncounterDate = null;

      if (lastFilaEncounter != null && lastFilaEncounter.getEncounterDatetime() != null) {
        lastFilaEncounterDate = lastFilaEncounter.getEncounterDatetime();
      }

      if (lastFichaEncounter != null && lastFichaEncounter.getEncounterDatetime() != null) {
        lastFichaEncounterDate = lastFichaEncounter.getEncounterDatetime();
      }

      if (lastFilaObsWithNextDrugPickUp != null
          && lastFilaObsWithNextDrugPickUp.getEncounter() != null
          && lastFilaObsWithNextDrugPickUp.getEncounter().getEncounterDatetime() != null
          && lastFilaObsWithNextDrugPickUp.getValueDatetime() != null) {
        returnDateForDrugPickup = lastFilaObsWithNextDrugPickUp.getValueDatetime();
        filaEncounterDate = lastFilaObsWithNextDrugPickUp.getEncounter().getEncounterDatetime();
      }
      if (lastFichaEncounter != null && lastFichaEncounter.getEncounterDatetime() != null) {
        lastFichaEncounterDate = lastFichaEncounter.getEncounterDatetime();
      }
      // case 1: fila as last encounter and has return visit date for drugs filled
      // ficha is filled with Last TYPE OF DISPENSATION (id=23739) and Value.code = QUARTERLY
      // (id=23730)
      // last fila encounter has observation recorded, but fila is after ficha
      if (lastFilaEncounter != null
          && lastFichaEncounter != null
          && lastFilaEncounter.getEncounterDatetime() != null
          && lastFichaEncounter.getEncounterDatetime() != null
          && lastFilaObsWithNextDrugPickUp != null
          && getLastTypeOfDispensationWithQuartelyObs != null
          && lastFilaObsWithNextDrugPickUp.getEncounter() != null
          && getLastTypeOfDispensationWithQuartelyObs.getEncounter() != null
          && lastFichaEncounter.equals(getLastTypeOfDispensationWithQuartelyObs.getEncounter())
          && lastFilaEncounter.equals(lastFilaObsWithNextDrugPickUp.getEncounter())
          && lastFilaObsWithNextDrugPickUp.getEncounter().getEncounterDatetime() != null
          && lastFilaEncounter
              .getEncounterDatetime()
              .after(lastFichaEncounter.getEncounterDatetime())
          && EptsCalculationUtils.daysSince(
                  lastFilaEncounter.getEncounterDatetime(),
                  lastFilaObsWithNextDrugPickUp.getValueDatetime())
              >= 83
          && EptsCalculationUtils.daysSince(
                  lastFilaEncounter.getEncounterDatetime(),
                  lastFilaObsWithNextDrugPickUp.getValueDatetime())
              <= 173) {
        found = true;
      }

      // case 2: fila as last encounter and has return visit date for drugs filled
      // ficha is filled with Last QUARTERLY DISPENSATION (DT) (id=23730) and Value.coded= START
      // DRUGS (id=1256) OR Value.coded= (CONTINUE REGIMEN id=1257)
      // last fila encounter has observation recorded, but fila is after ficha
      if (lastFilaEncounter != null
          && lastFichaEncounter != null
          && lastFilaEncounter.getEncounterDatetime() != null
          && lastFichaEncounter.getEncounterDatetime() != null
          && lastFilaObsWithNextDrugPickUp != null
          && getLastQuartelyDispensationWithStartOrContinueObs != null
          && lastFilaObsWithNextDrugPickUp.getEncounter() != null
          && getLastQuartelyDispensationWithStartOrContinueObs.getEncounter() != null
          && lastFichaEncounter.equals(
              getLastQuartelyDispensationWithStartOrContinueObs.getEncounter())
          && lastFilaEncounter.equals(lastFilaObsWithNextDrugPickUp.getEncounter())
          && lastFilaObsWithNextDrugPickUp.getEncounter().getEncounterDatetime() != null
          && lastFilaEncounter
              .getEncounterDatetime()
              .after(lastFichaEncounter.getEncounterDatetime())
          && EptsCalculationUtils.daysSince(
                  lastFilaEncounter.getEncounterDatetime(),
                  lastFilaObsWithNextDrugPickUp.getValueDatetime())
              >= 83
          && EptsCalculationUtils.daysSince(
                  lastFilaEncounter.getEncounterDatetime(),
                  lastFilaObsWithNextDrugPickUp.getValueDatetime())
              <= 173) {
        found = true;
      }

      // case 3: fila as last encounter and has return visit date for drugs filled
      // ficha is filled with Last TYPE OF DISPENSATION (id=23739) and Value.code = QUARTERLY
      // (id=23730)
      // last fila encounter has observation recorded, but ficha is after fila = opposite of case 1
      if (lastFilaEncounter != null
          && lastFichaEncounter != null
          && lastFilaObsWithNextDrugPickUp != null
          && getLastTypeOfDispensationWithQuartelyObs != null
          && lastFilaObsWithNextDrugPickUp.getEncounter() != null
          && getLastTypeOfDispensationWithQuartelyObs.getEncounter() != null
          && lastFichaEncounter.equals(getLastTypeOfDispensationWithQuartelyObs.getEncounter())
          && lastFilaEncounter.equals(lastFilaObsWithNextDrugPickUp.getEncounter())
          && lastFichaEncounter
              .getEncounterDatetime()
              .after(lastFilaEncounter.getEncounterDatetime())) {
        found = true;
      }

      // case 4: fila as last encounter and has return visit date for drugs filled
      // ficha is filled with Last QUARTERLY DISPENSATION (DT) (id=23730) and Value.coded= START
      // DRUGS (id=1256) OR Value.coded= (CONTINUE REGIMEN id=1257)
      // last fila encounter has observation recorded, but ficha is after fila opposite of case 2
      if (lastFilaEncounter != null
          && lastFichaEncounter != null
          && lastFilaObsWithNextDrugPickUp != null
          && getLastQuartelyDispensationWithStartOrContinueObs != null
          && lastFilaObsWithNextDrugPickUp.getEncounter() != null
          && getLastQuartelyDispensationWithStartOrContinueObs.getEncounter() != null
          && lastFichaEncounter.equals(
              getLastQuartelyDispensationWithStartOrContinueObs.getEncounter())
          && lastFilaEncounter.equals(lastFilaObsWithNextDrugPickUp.getEncounter())
          && lastFichaEncounter
              .getEncounterDatetime()
              .after(lastFilaEncounter.getEncounterDatetime())) {
        found = true;
      }
      // case 5: if last encounter is fila, take the last one which has next drug pick up date with
      // 83 and 173 days
      else if (lastFilaEncounter != null
          && lastFilaEncounter.getEncounterDatetime() != null
          && lastFilaObsWithNextDrugPickUp != null
          && lastFilaObsWithNextDrugPickUp.getEncounter() != null
          && lastFilaEncounter.equals(lastFilaObsWithNextDrugPickUp.getEncounter())
          && EptsCalculationUtils.daysSince(
                  lastFilaEncounter.getEncounterDatetime(),
                  lastFilaObsWithNextDrugPickUp.getValueDatetime())
              >= 83
          && EptsCalculationUtils.daysSince(
                  lastFilaEncounter.getEncounterDatetime(),
                  lastFilaObsWithNextDrugPickUp.getValueDatetime())
              <= 173) {
        found = true;
      }
      // case 6: If the most recent have more than one FICHA registered on the same most recent
      // date, then consider the record with QUARTELY response Value.code = QUARTERLY (id=23730)

      else if ((lastFichaEncounterDate != null
              && getLastTypeOfDispensationWithQuartelyObs != null
              && getLastTypeOfDispensationWithQuartelyObs.getEncounter() != null
              && lastFichaEncounter.equals(getLastTypeOfDispensationWithQuartelyObs.getEncounter()))
          || (lastFichaEncounterDate != null
              && getLastTypeOfDispensationWithQuartelyObs != null
              && getLastTypeOfDispensationWithQuartelyObs.getEncounter() != null
              && lastFichaEncounter.equals(
                  getLastTypeOfDispensationWithQuartelyObs.getEncounter()))) {
        found = true;
      }
      // case 5: If the most recent have more than one source FILA and FICHA registered on the same
      // most recent date, then consider the information from FILA
      else if (filaEncounterDate != null
          && returnDateForDrugPickup != null
          && lastFilaEncounterDate != null
          && filaEncounterDate.equals(lastFichaEncounterDate)) {
        // check if the fila has the indended observation recorded within the required ranges
        if (lastFilaObsWithNextDrugPickUp != null
            && lastFilaObsWithNextDrugPickUp.getEncounter() != null
            && lastFilaObsWithNextDrugPickUp.getEncounter().getEncounterDatetime() != null
            && lastFilaEncounter.equals(lastFilaObsWithNextDrugPickUp.getEncounter())
            && EptsCalculationUtils.daysSince(
                    lastFilaEncounterDate, lastFilaObsWithNextDrugPickUp.getValueDatetime())
                >= 83
            && EptsCalculationUtils.daysSince(
                    lastFilaEncounterDate, lastFilaObsWithNextDrugPickUp.getValueDatetime())
                <= 173) {
          found = true;
        }
        // if such records are missing in the fila with sane date as ficha, we are required to check
        // ficha
        else if (getLastTypeOfDispensationWithQuartelyObs != null
            && getLastTypeOfDispensationWithQuartelyObs.getEncounter() != null
            && lastFichaEncounter.equals(getLastTypeOfDispensationWithQuartelyObs.getEncounter())) {
          found = true;
        }
      }
      // exclude   patients   who   have   the   last   SEMESTRAL   QUARTERLY (concept   id=23730
      // with value_coded as value_coded=1267)
      if (lastQuartelyObsWithCompleted != null) {
        found = false;
      }
      resultMap.put(pId, new BooleanResult(found, this));
    }
    return resultMap;
  }
}
