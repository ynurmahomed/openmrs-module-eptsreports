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
    Concept quaterlyDispensation = hivMetadata.getQuarterlyConcept();
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
    CalculationResultMap getLastTypeOfDispensationWithQuartelyAsValueCoded =
        ePTSCalculationService.getObs(
            typeOfDispensation,
            Arrays.asList(ficha),
            cohort,
            Arrays.asList(location),
            Arrays.asList(quaterlyDispensation),
            TimeQualifier.LAST,
            null,
            onOrBefore,
            context);
    CalculationResultMap getLastQuartelyDispensationWithStartOrContinueRegimen =
        ePTSCalculationService.getObs(
            quaterlyDispensation,
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
    CalculationResultMap quartelyMap =
        ePTSCalculationService.getObs(
            quaterlyDispensation,
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

      Obs lastFilaObs = EptsCalculationUtils.obsResultForPatient(getLastFila, pId);
      Obs getLastTypeOfDispensationObsWithQuartelyValueCoded =
          EptsCalculationUtils.obsResultForPatient(
              getLastTypeOfDispensationWithQuartelyAsValueCoded, pId);
      Obs getLastQuartelyDispensationObsWithStartOrContinueRegimenObs =
          EptsCalculationUtils.obsResultForPatient(
              getLastQuartelyDispensationWithStartOrContinueRegimen, pId);
      Encounter lastFichaEncounter =
          EptsCalculationUtils.resultForPatient(lastFichaEncounterMap, pId);
      Obs lastQuartelyObsWithCompleted = EptsCalculationUtils.obsResultForPatient(quartelyMap, pId);

      Encounter lastFilaEncounter =
          EptsCalculationUtils.resultForPatient(lastFilaEncounterMap, pId);

      // case 1: fila as last encounter and has return visit date for drugs filled
      // this is compared to the date of Encounter Type Id = 6Last TYPE OF DISPENSATION
      // (id=23739)Value.code = QUARTERLY (id=23730)
      if (lastFilaEncounter != null
          && lastFilaEncounter.getEncounterDatetime() != null
          && lastFilaObs != null
          && lastFilaObs.getEncounter() != null
          && lastFilaEncounter.equals(lastFilaObs.getEncounter())
          && lastFilaObs.getEncounter().getEncounterDatetime() != null
          && lastFichaEncounter != null
          && lastFichaEncounter.getEncounterDatetime() != null
          && getLastTypeOfDispensationObsWithQuartelyValueCoded != null
          && getLastTypeOfDispensationObsWithQuartelyValueCoded.getEncounter() != null
          && lastFichaEncounter.equals(
              getLastTypeOfDispensationObsWithQuartelyValueCoded.getEncounter())
          && lastFilaEncounter
              .getEncounterDatetime()
              .after(lastFichaEncounter.getEncounterDatetime())
          && EptsCalculationUtils.daysSince(
                  lastFilaEncounter.getEncounterDatetime(), lastFilaObs.getValueDatetime())
              >= 83
          && EptsCalculationUtils.daysSince(
                  lastFilaEncounter.getEncounterDatetime(), lastFilaObs.getValueDatetime())
              <= 173) {
        found = true;
      }

      // case 2: fila as last encounter and has return visit date for drugs filled
      // this is compared to the date of Encounter Type Id = 6 Last QUARTERLY DISPENSATION (DT)
      // (id=23730)Value.coded= START DRUGS (id=1256) OR Value.coded= (CONTINUE REGIMEN id=1257)
      else if (lastFilaEncounter != null
          && lastFilaEncounter.getEncounterDatetime() != null
          && lastFilaObs != null
          && lastFilaObs.getEncounter() != null
          && lastFilaEncounter.equals(lastFilaObs.getEncounter())
          && lastFilaObs.getEncounter().getEncounterDatetime() != null
          && lastFichaEncounter != null
          && lastFichaEncounter.getEncounterDatetime() != null
          && getLastQuartelyDispensationObsWithStartOrContinueRegimenObs != null
          && getLastQuartelyDispensationObsWithStartOrContinueRegimenObs.getEncounter() != null
          && lastFichaEncounter.equals(
              getLastQuartelyDispensationObsWithStartOrContinueRegimenObs.getEncounter())
          && lastFilaEncounter
              .getEncounterDatetime()
              .after(lastFichaEncounter.getEncounterDatetime())
          && EptsCalculationUtils.daysSince(
                  lastFilaEncounter.getEncounterDatetime(), lastFilaObs.getValueDatetime())
              >= 83
          && EptsCalculationUtils.daysSince(
                  lastFilaEncounter.getEncounterDatetime(), lastFilaObs.getValueDatetime())
              <= 173) {
        found = true;
      }

      // case 1: ficha  as last encounter(ficha > fila) reverse of case1
      // this is compared to the date of Encounter Type Id = 6Last TYPE OF DISPENSATION
      // (id=23739)Value.code = QUARTERLY (id=23730)
      if (lastFilaEncounter != null
          && lastFilaEncounter.getEncounterDatetime() != null
          && lastFilaObs != null
          && lastFilaObs.getEncounter() != null
          && lastFilaEncounter.equals(lastFilaObs.getEncounter())
          && lastFilaObs.getEncounter().getEncounterDatetime() != null
          && lastFichaEncounter != null
          && lastFichaEncounter.getEncounterDatetime() != null
          && getLastTypeOfDispensationObsWithQuartelyValueCoded != null
          && getLastTypeOfDispensationObsWithQuartelyValueCoded.getEncounter() != null
          && lastFichaEncounter.equals(
              getLastTypeOfDispensationObsWithQuartelyValueCoded.getEncounter())
          && lastFichaEncounter
              .getEncounterDatetime()
              .after(lastFilaEncounter.getEncounterDatetime())) {
        found = true;
      }

      // case 4: ficha as last encounter, ficha >fila opposite of 2
      // this is compared to the date of Encounter Type Id = 6 Last QUARTERLY DISPENSATION (DT)
      // (id=23730)Value.coded= START DRUGS (id=1256) OR Value.coded= (CONTINUE REGIMEN id=1257)
      else if (lastFilaEncounter != null
          && lastFilaEncounter.getEncounterDatetime() != null
          && lastFilaObs != null
          && lastFilaObs.getEncounter() != null
          && lastFilaEncounter.equals(lastFilaObs.getEncounter())
          && lastFilaObs.getEncounter().getEncounterDatetime() != null
          && lastFichaEncounter != null
          && lastFichaEncounter.getEncounterDatetime() != null
          && getLastQuartelyDispensationObsWithStartOrContinueRegimenObs != null
          && getLastQuartelyDispensationObsWithStartOrContinueRegimenObs.getEncounter() != null
          && lastFichaEncounter.equals(
              getLastQuartelyDispensationObsWithStartOrContinueRegimenObs.getEncounter())
          && lastFichaEncounter
              .getEncounterDatetime()
              .after(lastFilaEncounter.getEncounterDatetime())) {
        found = true;
      }
      // case 5: here fila is the latest encounter with the values for the observations collected
      else if (lastFilaEncounter != null
          && lastFilaEncounter.getEncounterDatetime() != null
          && lastFilaObs != null
          && lastFilaObs.getEncounter() != null
          && lastFichaEncounter == null
          && lastFilaEncounter.equals(lastFilaObs.getEncounter())
          && lastFilaObs.getEncounter().getEncounterDatetime() != null
          && EptsCalculationUtils.daysSince(
                  lastFilaEncounter.getEncounterDatetime(), lastFilaObs.getValueDatetime())
              >= 83
          && EptsCalculationUtils.daysSince(
                  lastFilaEncounter.getEncounterDatetime(), lastFilaObs.getValueDatetime())
              <= 173) {
        found = true;
      }
      // case 5: here fila is the latest encounter with the values for the observations collected
      else if (lastFilaEncounter != null
          && lastFilaEncounter.getEncounterDatetime() != null
          && lastFilaObs != null
          && lastFilaObs.getEncounter() != null
          && lastFichaEncounter == null
          && lastFilaEncounter.equals(lastFilaObs.getEncounter())
          && lastFilaObs.getEncounter().getEncounterDatetime() != null
          && EptsCalculationUtils.daysSince(
                  lastFilaEncounter.getEncounterDatetime(), lastFilaObs.getValueDatetime())
              >= 83
          && EptsCalculationUtils.daysSince(
                  lastFilaEncounter.getEncounterDatetime(), lastFilaObs.getValueDatetime())
              <= 173) {
        found = true;
      }
      // case 6: ficha has last encounter  Last TYPE OF DISPENSATION (id=23739) Value.code =
      // QUARTERLY (id=23730)
      else if (lastFichaEncounter != null
          && getLastTypeOfDispensationObsWithQuartelyValueCoded != null
          && lastFilaEncounter == null
          && lastFichaEncounter.equals(
              getLastTypeOfDispensationObsWithQuartelyValueCoded.getEncounter())) {
        found = true;
      }
      // case 7: or Last QUARTERLY DISPENSATION (DT) (id=23730) with Value.coded= START DRUGS
      // (id=1256) OR
      // Value.coded= (CONTINUE REGIMEN id=1257)
      else if (lastFichaEncounter != null
          && getLastQuartelyDispensationObsWithStartOrContinueRegimenObs != null
          && lastFilaEncounter == null
          && lastFichaEncounter.equals(
              getLastQuartelyDispensationObsWithStartOrContinueRegimenObs.getEncounter())) {
        found = true;
      }
      // case 7: If the most recent have more than one source FILA and FICHA registered on the same
      // most recent date, then consider the information from FILA
      // get the first case for quartely
      else if (lastFilaEncounter != null & lastFichaEncounter != null
          && lastFilaObs != null
          && getLastTypeOfDispensationObsWithQuartelyValueCoded != null
          && lastFilaEncounter.equals(lastFilaObs.getEncounter())
          && lastFichaEncounter.equals(
              getLastTypeOfDispensationObsWithQuartelyValueCoded.getEncounter())
          && lastFilaEncounter
              .getEncounterDatetime()
              .equals(lastFichaEncounter.getEncounterDatetime())
          && EptsCalculationUtils.daysSince(
                  lastFilaEncounter.getEncounterDatetime(), lastFilaObs.getValueDatetime())
              >= 83
          && EptsCalculationUtils.daysSince(
                  lastFilaEncounter.getEncounterDatetime(), lastFilaObs.getValueDatetime())
              <= 173) {
        found = true;

      }
      // case 8: If the most recent have more than one source FILA and FICHA registered on the same
      // most recent date, then consider the information from FILA
      // get the first case for start and continue regimen
      else if (lastFilaEncounter != null & lastFichaEncounter != null
          && lastFilaObs != null
          && getLastQuartelyDispensationObsWithStartOrContinueRegimenObs != null
          && lastFilaEncounter.equals(lastFilaObs.getEncounter())
          && lastFichaEncounter.equals(
              getLastQuartelyDispensationObsWithStartOrContinueRegimenObs.getEncounter())
          && lastFilaEncounter
              .getEncounterDatetime()
              .equals(lastFichaEncounter.getEncounterDatetime())
          && EptsCalculationUtils.daysSince(
                  lastFilaEncounter.getEncounterDatetime(), lastFilaObs.getValueDatetime())
              >= 83
          && EptsCalculationUtils.daysSince(
                  lastFilaEncounter.getEncounterDatetime(), lastFilaObs.getValueDatetime())
              <= 173) {
        found = true;
      }
      // exclude   patients   who   have   the   last   SEMESTRAL   QUARTERLY (concept   id=23730
      // with value_coded as value_coded=1267)
      if (lastFichaEncounter != null
          && lastQuartelyObsWithCompleted != null
          && lastFichaEncounter.equals(lastQuartelyObsWithCompleted.getEncounter())) {
        found = false;
      }
      resultMap.put(pId, new BooleanResult(found, this));
    }
    return resultMap;
  }
}
