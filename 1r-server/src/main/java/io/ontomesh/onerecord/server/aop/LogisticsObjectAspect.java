package io.ontomesh.onerecord.server.aop;

import io.ontomesh.onerecord.server.domain.LogisticsObject;
import io.ontomesh.onerecord.server.services.AuditTrailService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.eclipse.rdf4j.model.IRI;
import org.iata.onerecord.api.API;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.iata.onerecord.api.API.REQUEST_ACCEPTED;
import static org.iata.onerecord.api.API.REQUEST_REJECTED;

@Slf4j
@Aspect
@Component
public class LogisticsObjectAspect {

    @Autowired
    private AuditTrailService auditTrailService;

    @Pointcut("execution(* io.ontomesh.onerecord.server.services.ActionRequestService.createChangeRequest(..))")
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
