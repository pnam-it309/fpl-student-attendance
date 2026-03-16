package udpm.hn.studentattendance.helpers;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import udpm.hn.studentattendance.core.authentication.oauth2.AuthUser;
import udpm.hn.studentattendance.entities.UserAdmin;
import udpm.hn.studentattendance.entities.UserStaff;
import udpm.hn.studentattendance.entities.UserStudent;
import udpm.hn.studentattendance.infrastructure.constants.RoleConstant;
import udpm.hn.studentattendance.infrastructure.constants.SessionConstant;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class SessionHelper {

    private final HttpSession httpSession;

    public AuthUser getCurrentUser() {
        return (AuthUser) httpSession.getAttribute(SessionConstant.AUTH_USER);
    }

    public void setCurrentUser(AuthUser user) {
        httpSession.setAttribute(SessionConstant.AUTH_USER, user);
    }

    public AuthUser buildAuthUser(UserAdmin user, Set<RoleConstant> role, String idFacility) {
        AuthUser authUser = new AuthUser();
        authUser.setId(user.getId());
        authUser.setCode(user.getCode());
        authUser.setName(user.getName());
        authUser.setEmail(user.getEmail());
        authUser.setRole(role);
        authUser.setIdFacility(idFacility);
        authUser.setPicture(user.getImage());
        return authUser;
    }

    public AuthUser buildAuthUser(UserStaff user, Set<RoleConstant> role, String idFacility) {
        AuthUser authUser = new AuthUser();
        authUser.setId(user.getId());
        authUser.setCode(user.getCode());
        authUser.setName(user.getName());
        authUser.setEmail(user.getEmailFpt());
        authUser.setEmailFe(user.getEmailFe());
        authUser.setEmailFpt(user.getEmailFpt());
        authUser.setRole(role);
        authUser.setIdFacility(idFacility);
        authUser.setPicture(user.getImage());
        return authUser;
    }

    public AuthUser buildAuthUser(UserStudent user, Set<RoleConstant> role, String idFacility) {
        AuthUser authUser = new AuthUser();
        authUser.setId(user.getId());
        authUser.setCode(user.getCode());
        authUser.setName(user.getName());
        authUser.setEmail(user.getEmail());
        authUser.setRole(role);
        authUser.setIdFacility(idFacility);
        authUser.setPicture(user.getImage());
        return authUser;
    }

    public String getUserId() {
        AuthUser user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    public String getUserName() {
        AuthUser user = getCurrentUser();
        return user != null ? user.getName() : null;
    }

    public Set<RoleConstant> getUserRole() {
        AuthUser user = getCurrentUser();
        return user != null ? user.getRole() : null;
    }

    public String getFacilityId() {
        AuthUser user = getCurrentUser();
        return user != null ? user.getIdFacility() : null;
    }

    public String getUserEmail() {
        AuthUser user = getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    public String getUserEmailFe() {
        AuthUser user = getCurrentUser();
        return user != null ? user.getEmailFe() : null;
    }

    public String getUserEmailFpt() {
        AuthUser user = getCurrentUser();
        return user != null ? user.getEmailFpt() : null;
    }

    public String getUserPicture() {
        AuthUser user = getCurrentUser();
        return user != null ? user.getPicture() : null;
    }

    public String getUserCode() {
        AuthUser user = getCurrentUser();
        return user != null ? user.getCode() : null;
    }

    public RoleConstant getLoginRole() {
        return (RoleConstant) httpSession.getAttribute(SessionConstant.LOGIN_ROLE);
    }

    public void setLoginRole(RoleConstant role) {
        httpSession.setAttribute(SessionConstant.LOGIN_ROLE, role);
    }

}
