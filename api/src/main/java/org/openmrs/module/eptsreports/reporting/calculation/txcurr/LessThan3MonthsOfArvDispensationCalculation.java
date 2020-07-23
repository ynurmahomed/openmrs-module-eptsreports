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
public class LessThan3MonthsOfArvDispensationCalculation extends AbstractPatientCalculation {
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
    Concept monthly = hivMetadata.getMonthlyConcept();

    CalculationResultMap getFilaEncounterMap =
        ePTSCalculationService.getEncounter(
            Arrays.asList(fila), TimeQualifier.LAST, cohort, location, onOrBefore, context);
    CalculationResultMap getFichaEncounterMap =
        ePTSCalculationService.getEncounter(
            Arrays.asList(ficha), TimeQualifier.LAST, cohort, location, onOrBefore, context);
    CalculationResultMap getLastEncounterWithReturnDateForArv =
        ePTSCalculationService.getObs(
            returnVisitDateForArvDrugs,
            Arrays.asList(fila),
            cohort,
            Arrays.asList(location),
            null,
            TimeQualifier.LAST,
            null,
            context);
    CalculationResultMap getLastEncounterWithDepositionAndMonthlyAsCodedValue =
        ePTSCalculationService.getObs(
            typeOfDispensation,
            Arrays.asList(ficha),
            cohort,
            Arrays.asList(location),
            Arrays.asList(monthly),
            TimeQualifier.LAST,
            null,
            context);

    for (Integer pId : cohort) {
      boolean found = false;

      Encounter lastFilaEncounter = EptsCalculationUtils.resultForPatient(getFilaEncounterMap, pId);
      Encounter lastFichaEncounter =
          EptsCalculationUtils.resultForPatient(getFichaEncounterMap, pId);
      Obs getObsWithReturnVisitDateFilled =
          EptsCalculationUtils.obsResultForPatient(getLastEncounterWithReturnDateForArv, pId);
      Obs getObsWithDepositionAndMonthlyAsCodedValue =
          EptsCalculationUtils.obsResultForPatient(
              getLastEncounterWithDepositionAndMonthlyAsCodedValue, pId);

      Date lastFilaEncounterDate = null;
      Date lastFichaEncounterDate = null;

      if (lastFilaEncounter != null && lastFilaEncounter.getEncounterDatetime() != null) {
        lastFilaEncounterDate = lastFilaEncounter.getEncounterDatetime();
      }

      if (lastFichaEncounter != null && lastFichaEncounter.getEncounterDatetime() != null) {
        lastFichaEncounterDate = lastFichaEncounter.getEncounterDatetime();
      }
      // case 1: fila as last encounter and has return visit date for drugs filled
      if (lastFilaEncounterDate != null
          && getObsWithReturnVisitDateFilled != null
          && getObsWithReturnVisitDateFilled.getEncounter() != null
          && lastFichaEncounterDate != null
          && getObsWithReturnVisitDateFilled.getEncounter().getEncounterDatetime() != null
          && lastFilaEncounter.equals(getObsWithReturnVisitDateFilled.getEncounter())
          && lastFilaEncounterDate.after(lastFichaEncounterDate)
          && EptsCalculationUtils.daysSince(
                  lastFilaEncounterDate, getObsWithReturnVisitDateFilled.getValueDatetime())
              < 83) {
        found = true;
      }
      // case 2: ficha as the last encounter and has Last TYPE OF DISPENSATION and value coded as
      // monthly
      else if (lastFilaEncounterDate != null
          && lastFichaEncounterDate != null
          && getObsWithDepositionAndMonthlyAsCodedValue != null
          && getObsWithDepositionAndMonthlyAsCodedValue.getEncounter() != null
          && lastFichaEncounterDate.after(lastFilaEncounterDate)
          && lastFichaEncounter.equals(getObsWithDepositionAndMonthlyAsCodedValue.getEncounter())) {
        found = true;
      }
      // case 3: Only fila available and has value datetime collected for the next drug pick up
      else if (lastFilaEncounterDate != null
          && lastFichaEncounterDate == null
          && getObsWithReturnVisitDateFilled != null
          && getObsWithReturnVisitDateFilled.getEncounter() != null
          && getObsWithReturnVisitDateFilled.getEncounter().getEncounterDatetime() != null
          && lastFilaEncounter.equals(getObsWithReturnVisitDateFilled.getEncounter())
          && EptsCalculationUtils.daysSince(
                  lastFilaEncounterDate, getObsWithReturnVisitDateFilled.getValueDatetime())
              < 83) {
        found = true;
      }
      // case 4: if only ficha is available and has Last TYPE OF DISPENSATION and value coded as
      // monthly
      else if (lastFilaEncounterDate == null
          && lastFichaEncounterDate != null
          && getObsWithDepositionAndMonthlyAsCodedValue != null
          && getObsWithDepositionAndMonthlyAsCodedValue.getEncounter() != null
          && lastFichaEncounter.equals(getObsWithDepositionAndMonthlyAsCodedValue.getEncounter())) {
        found = true;
      }
      // case 5: if both fila and ficha are available taken on the same date, we pick fila first if
      // it has the next drug pick up date
      // otherwise we consider ficha if fila is null, but ficha has to contain the required obs to
      // pass
      else if (lastFilaEncounterDate != null
          && lastFilaEncounterDate.equals(lastFichaEncounterDate)) {

        // check the fila if it has the value date time recorded for the next drug pick up
        if (getObsWithReturnVisitDateFilled != null
            && getObsWithReturnVisitDateFilled.getEncounter() != null
            && getObsWithReturnVisitDateFilled.getEncounter().getEncounterDatetime() != null
            && lastFilaEncounter.equals(getObsWithReturnVisitDateFilled.getEncounter())
            && EptsCalculationUtils.daysSince(
                    lastFilaEncounterDate, getObsWithReturnVisitDateFilled.getValueDatetime())
                < 83) {
          found = true;
        }
        // check if fila is empty, we check the ficha if there is any obs saved
        else if (getObsWithDepositionAndMonthlyAsCodedValue != null
            && getObsWithDepositionAndMonthlyAsCodedValue.getEncounter() != null
            && lastFichaEncounter.equals(
                getObsWithDepositionAndMonthlyAsCodedValue.getEncounter())) {
          found = true;
        }
      }
      resultMap.put(pId, new BooleanResult(found, this));
    }
    return resultMap;
  }
}
