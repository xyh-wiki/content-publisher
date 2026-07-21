package io.contentpublisher.platform.web.controller;

import io.contentpublisher.platform.application.ApplicationException;
import io.contentpublisher.platform.web.form.ChangePasswordForm;
import io.contentpublisher.platform.web.security.LocalPasswordService;
import io.contentpublisher.platform.web.security.LocalUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@ConditionalOnProperty(name = "publisher.security.mode", havingValue = "LOCAL")
public class PasswordController {
    private final LocalPasswordService passwords;

    public PasswordController(LocalPasswordService passwords) {
        this.passwords = passwords;
    }

    @GetMapping("/change-password")
    public String changePassword(Authentication authentication, Model model) {
        LocalUserPrincipal principal = principal(authentication);
        model.addAttribute("forced", principal.mustChangePassword());
        model.addAttribute("changePasswordForm", new ChangePasswordForm());
        return "change-password";
    }

    @PostMapping("/change-password")
    public String updatePassword(Authentication authentication,
                                 @Valid @ModelAttribute("changePasswordForm") ChangePasswordForm form,
                                 BindingResult bindingResult, Model model, HttpServletRequest request) {
        LocalUserPrincipal principal = principal(authentication);
        model.addAttribute("forced", principal.mustChangePassword());
        if (bindingResult.hasErrors()) return "change-password";
        try {
            passwords.changePassword(principal, form.getCurrentPassword(), form.getNewPassword(),
                    form.getConfirmPassword());
            if (request.getSession(false) != null) request.getSession(false).invalidate();
            return "redirect:/login?passwordChanged";
        } catch (ApplicationException exception) {
            model.addAttribute("error", exception.getMessage());
            form.setCurrentPassword(null);
            form.setNewPassword(null);
            form.setConfirmPassword(null);
            return "change-password";
        }
    }

    private LocalUserPrincipal principal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof LocalUserPrincipal principal)) {
            throw new ApplicationException("AUTHENTICATION_REQUIRED", "请先登录");
        }
        return principal;
    }
}
