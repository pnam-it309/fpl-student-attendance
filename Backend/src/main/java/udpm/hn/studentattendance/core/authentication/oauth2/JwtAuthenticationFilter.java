package udpm.hn.studentattendance.core.authentication.oauth2;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import udpm.hn.studentattendance.core.authentication.repositories.AuthenticationRoleRepository;
import udpm.hn.studentattendance.core.authentication.repositories.AuthenticationUserAdminRepository;
import udpm.hn.studentattendance.core.authentication.repositories.AuthenticationUserStaffRepository;
import udpm.hn.studentattendance.core.authentication.repositories.AuthenticationUserStudentRepository;
import udpm.hn.studentattendance.core.authentication.utils.JwtUtil;
import udpm.hn.studentattendance.entities.Role;
import udpm.hn.studentattendance.entities.UserAdmin;
import udpm.hn.studentattendance.entities.UserStaff;
import udpm.hn.studentattendance.entities.UserStudent;
import udpm.hn.studentattendance.helpers.RouterHelper;
import udpm.hn.studentattendance.helpers.SessionHelper;
import udpm.hn.studentattendance.infrastructure.constants.RoleConstant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    private final SessionHelper sessionHelper;

    private final AuthenticationUserAdminRepository authenticationUserAdminRepository;

    private final AuthenticationUserStaffRepository authenticationUserStaffRepository;

    private final AuthenticationUserStudentRepository authenticationUserStudentRepository;

    private final AuthenticationRoleRepository authenticationRoleRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = JwtUtil.getAuthorization(request);

        if (token != null) {

            if (jwtUtil.validateToken(token)) {

                RoleConstant requiredRole = RouterHelper.getRequiredRoleForUrl(request.getRequestURI());

                String email = jwtUtil.getEmailFromToken(token);
                Set<String> role = jwtUtil.getRoleFromToken(token);
                String facilityID = jwtUtil.getFacilityFromToken(token);

                AuthUser authUser = null;

                if (requiredRole == RoleConstant.ADMIN) {
                    authUser = getAccountAdmin(email);
                } else if (requiredRole == RoleConstant.STAFF || requiredRole == RoleConstant.TEACHER) {
                    authUser = getAccountStaffOrTeacher(email, facilityID);
                } else if (requiredRole == RoleConstant.STUDENT) {
                    authUser = getAccountStudent(email, facilityID);
                } else {
                    if (role.contains(RoleConstant.ADMIN.name())) {
                        authUser = getAccountAdmin(email);
                    } else if (role.contains(RoleConstant.STAFF.name()) || role.contains(RoleConstant.TEACHER.name())) {
                        authUser = getAccountStaffOrTeacher(email, facilityID);
                    } else if (role.contains(RoleConstant.STUDENT.name())) {
                        authUser = getAccountStudent(email, facilityID);
                    }
                }

                if (authUser != null) {
                    sessionHelper.setCurrentUser(authUser);
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    for (RoleConstant r : authUser.getRole()) {
                        authorities.add(new SimpleGrantedAuthority(r.name()));
                    }

                    Authentication auth = new UsernamePasswordAuthenticationToken(email, token, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else if (!role.isEmpty()) {
                    // Fallback for valid token but user not found in DB (e.g., during registration)
                    AuthUser tempUser = new AuthUser();
                    Claims claims = jwtUtil.getClaimsFromToken(token).getBody();
                    tempUser.setId(claims.get("id", String.class));
                    tempUser.setEmail(email);
                    tempUser.setName(claims.get("name", String.class));
                    tempUser.setPicture(claims.get("picture", String.class));
                    
                    Set<RoleConstant> roleConstants = new HashSet<>();
                    for (String r : role) {
                        try {
                            roleConstants.add(RoleConstant.valueOf(r));
                        } catch (Exception ignored) {}
                    }
                    tempUser.setRole(roleConstants);
                    sessionHelper.setCurrentUser(tempUser);

                    List<GrantedAuthority> authorities = new ArrayList<>();
                    for (String r : role) {
                        authorities.add(new SimpleGrantedAuthority(r));
                    }
                    Authentication auth = new UsernamePasswordAuthenticationToken(email, token, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        chain.doFilter(request, response);
    }

    private AuthUser getAccountStaffOrTeacher(String email, String facilityID) {
        Optional<UserStaff> userStaff = authenticationUserStaffRepository.findLogin(email, facilityID);
        if (userStaff.isPresent()) {
            Set<RoleConstant> roles = new HashSet<>();

            List<Role> lstRole = authenticationRoleRepository.findRolesByUserId(userStaff.get().getId());
            for (Role r : lstRole) {
                roles.add(r.getCode());
            }
            sessionHelper.setLoginRole(RoleConstant.STAFF);
            return sessionHelper.buildAuthUser(userStaff.get(), roles, facilityID);
        }
        return null;
    }

    private AuthUser getAccountAdmin(String email) {
        UserAdmin userAdmin = authenticationUserAdminRepository.findByEmail(email).orElse(null);
        if (userAdmin == null) {
            return null;
        }
        sessionHelper.setLoginRole(RoleConstant.ADMIN);
        return sessionHelper.buildAuthUser(userAdmin, Set.of(RoleConstant.ADMIN), null);
    }

    private AuthUser getAccountStudent(String email, String facilityID) {
        UserStudent userStudent = authenticationUserStudentRepository.findByEmailAndFacility_Id(email,
                facilityID).orElse(null);
        if (userStudent == null) {
            return null;
        }
        sessionHelper.setLoginRole(RoleConstant.STUDENT);
        return sessionHelper.buildAuthUser(userStudent, Set.of(RoleConstant.STUDENT), facilityID);
    }

}
