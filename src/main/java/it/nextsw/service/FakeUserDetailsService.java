///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package it.nextsw.service;
//
//import it.nextsw.entities.Utenti;
//import it.nextsw.repository.UtentiRepository;
//import static java.util.Arrays.asList;
//import java.util.Collection;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.AuthorityUtils;
//import org.springframework.security.core.userdetails.User;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//
//@Service
//public class FakeUserDetailsService implements UserDetailsService {
//
//    @Autowired
//    private UtentiRepository utenteRepository;
//
//    @Override
//    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//        try {
//            Utenti user = utenteRepository.findByUsername(username);
//            if (user == null) {
//                throw new UsernameNotFoundException("Username " + username + " not found");
//            }
//            return new User(username, user.getPassword(), getGrantedAuthorities(user));
//        } catch (Exception e) {
//            System.err.println(e);
//            throw e;
//        }
//        
//        
//    }
//
//    private Collection<? extends GrantedAuthority> getGrantedAuthorities(Utenti user) {
//
//        Collection<? extends GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(user.getRole());
//
////        if (user.equals("John")) {
////            authorities = asList(() -> "ROLE_ADMIN", () -> "ROLE_BASIC");
////        } else {
////            authorities = asList(() -> "ROLE_BASIC");
////        }
//        return authorities;
//    }
//}
