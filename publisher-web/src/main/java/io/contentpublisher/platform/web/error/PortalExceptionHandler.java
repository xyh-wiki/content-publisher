package io.contentpublisher.platform.web.error;

import io.contentpublisher.platform.application.ApplicationException;
import io.contentpublisher.platform.web.controller.PortalManagementController;
import io.contentpublisher.platform.web.controller.PortalAiSettingsController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice(assignableTypes = {PortalManagementController.class, PortalAiSettingsController.class})
public class PortalExceptionHandler {
    @ExceptionHandler(ApplicationException.class)
    ModelAndView applicationError(ApplicationException exception) {
        return error(exception.code(), exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ModelAndView invalidArgument(IllegalArgumentException exception) {
        return error("INVALID_ARGUMENT", exception.getMessage());
    }

    private ModelAndView error(String code, String message) {
        ModelAndView view = new ModelAndView("portal-error");
        view.addObject("code", code);
        view.addObject("message", message);
        return view;
    }
}
