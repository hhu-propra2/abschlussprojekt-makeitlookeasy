package de.propra2.ausleiherino24.service;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.junit4.SpringRunner;

import de.propra2.ausleiherino24.data.UserRepository;
import de.propra2.ausleiherino24.model.CustomUserDetails;
import de.propra2.ausleiherino24.model.User;

@RunWith(SpringRunner.class)
public class CustomUserDetailsServiceTest {

	private UserRepository users;
	private CustomUserDetailsService customUserDetailsService;
	private User user1;
	private User user2;

	@Before
	public void init() {
		users = Mockito.mock(UserRepository.class);
		user1 = new User();
		user1.setUsername("user1");
		user2 = new User();
		user2.setUsername("user2");
		customUserDetailsService = new CustomUserDetailsService(users);
	}

	@Test
	public void test() {
		Mockito.when(users.findByUsername("user1")).thenReturn(Optional.of(user1));
		CustomUserDetails expected = new CustomUserDetails(user1);
		Assertions.assertThat(customUserDetailsService.loadUserByUsername("user1")).isEqualTo(expected);
	}

	@Test(expected = UsernameNotFoundException.class)
	public void test2() {
		Mockito.when(users.findByUsername("user1")).thenThrow(UsernameNotFoundException.class);

		customUserDetailsService.loadUserByUsername("user1");
	}
}