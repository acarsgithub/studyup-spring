package io.study.studyup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;


@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    UserDetailsService userDetailsService;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService);
    }


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()

                // HOW WILL WE DIFFERENTIATE BETWEEN APPLICATION USER AND ADMIN VS STUDYGROUP USER AND ADMIN ??
                .antMatchers("/request-group").hasAnyRole("USER", "ADMIN")
                .antMatchers("/{username}/requests").hasAnyRole("USER", "ADMIN")
                .antMatchers("/{username}").hasAnyRole("USER", "ADMIN")
                .antMatchers("/groups").hasAnyRole("USER", "ADMIN")
                .antMatchers("/create-group").hasAnyRole("USER", "ADMIN")
                .antMatchers("/signup").permitAll()
                .antMatchers("/").permitAll()
                .and().formLogin();
        http.csrf().disable();
    }

    @Bean
    public PasswordEncoder getPasswordEncoder(){
        return NoOpPasswordEncoder.getInstance();
    }
}
