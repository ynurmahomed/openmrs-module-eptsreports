package org.openmrs.module.eptsreports.reporting.calculation.txcurr;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import org.openmrs.Concept;
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
            Arrays.asList(monthly),
            TimeQualifier.LAST,
            null,
            onOrBefore,
            context);
    for (Integer pId : cohort) {
      boolean found = false;

      Obs lastFilaObs = EptsCalculationUtils.obsResultForPatient(getLastFila, pId);
      Obs lastFicha = EptsCalculationUtils.obsResultForPatient(getLastFicha, pId);
      Date returnDateForDrugPickup = null;
      Date filaEncounterDate = null;
      Date fichaEncounterDate = null;
      if (lastFilaObs != null
          && lastFilaObs.getEncounter().getEncounterDatetime() != null
          && lastFilaObs.getValueDatetime() != null) {
        returnDateForDrugPickup = lastFilaObs.getValueDatetime();
        filaEncounterDate = lastFilaObs.getEncounter().getEncounterDatetime();
      }

      if (lastFicha != null
          && lastFicha.getEncounter().getEncounterDatetime() != null
          && lastFicha.getValueCoded() != null) {
        fichaEncounterDate = lastFicha.getEncounter().getEncounterDatetime();
      }
      if (filaEncounterDate != null
          && fichaEncounterDate != null
          && returnDateForDrugPickup != null
          && filaEncounterDate.after(fichaEncounterDate)
          && EptsCalculationUtils.daysSince(filaEncounterDate, returnDateForDrugPickup) < 83) {
        found = true;
      } else if (filaEncounterDate != null
          && fichaEncounterDate != null
          && fichaEncounterDate.after(filaEncounterDate)) {
        found = true;
      } else if (filaEncounterDate != null
          && fichaEncounterDate == null
          && returnDateForDrugPickup != null
          && EptsCalculationUtils.daysSince(filaEncounterDate, returnDateForDrugPickup) < 83) {
        found = true;
      } else if ((filaEncounterDate == null || returnDateForDrugPickup == null)
          && fichaEncounterDate != null) {
        found = true;
      } else if (filaEncounterDate != null
          && returnDateForDrugPickup != null
          && filaEncounterDate.equals(fichaEncounterDate)
          && EptsCalculationUtils.daysSince(filaEncounterDate, returnDateForDrugPickup) < 83) {
        found = true;
      }

      resultMap.put(pId, new BooleanResult(found, this));
    }
    return resultMap;
  }
}
