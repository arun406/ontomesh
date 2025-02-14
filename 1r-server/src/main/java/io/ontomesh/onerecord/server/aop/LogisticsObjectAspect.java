package io.ontomesh.onerecord.server.aop;

import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.services.logisticsobjects.audittrail.AuditTrailService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.eclipse.rdf4j.model.IRI;
import org.iata.onerecord.api.API;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static org.iata.onerecord.api.API.REQUEST_ACCEPTED;
import static org.iata.onerecord.api.API.REQUEST_REJECTED;

@Aspect
@Component
public class LogisticsObjectAspect {

    private final AuditTrailService auditTrailService;
    private final static Logger log = LoggerFactory.getLogger(LogisticsObjectAspect.class);

    public LogisticsObjectAspect(AuditTrailService auditTrailService) {
        this.auditTrailService = auditTrailService;
    }

    @Pointcut("execution(* io.ontomesh.onerecord.server.services.actionrequests.ActionRequestService.createChangeRequest(..))")
    public void processLogisticsObjectUpdateRequest() {
    }

    @Around("processLogisticsObjectUpdateRequest()")
    public Object aroundProcessLogisticsObjectUpdateRequest(ProceedingJoinPoint joinPoint) throws Throwable {

        // Before method execution
        log.debug("creating the change request : {}", joinPoint.getSignature().getName());
        LogisticsObject changeObject = (LogisticsObject) joinPoint.getArgs()[0];

        IRI changeRequestIRI = this.auditTrailService.saveChangeRequest(changeObject);
        this.auditTrailService.saveOrUpdate(changeRequestIRI, changeObject.findIRI(API.hasLogisticsObject, false));

        Object result = null;
        try {
            // Proceed with the method execution
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            log.error("Exception in method: {}", joinPoint.getSignature().getName(), ex);
            this.auditTrailService.updateChangeRequestStatus(changeRequestIRI, REQUEST_REJECTED);
            throw ex;
        }

        this.auditTrailService.updateChangeRequestStatus(changeRequestIRI, REQUEST_ACCEPTED);
        // After method execution
        log.debug("After method: {}", joinPoint.getSignature().getName());
        return result;
    }

}
