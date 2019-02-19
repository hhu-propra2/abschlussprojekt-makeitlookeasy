package de.propra2.ausleiherino24;

import com.github.javafaker.Faker;
import de.propra2.ausleiherino24.data.ArticleRepository;
import de.propra2.ausleiherino24.data.CaseRepository;
import de.propra2.ausleiherino24.data.PersonRepository;
import de.propra2.ausleiherino24.data.UserRepository;
import de.propra2.ausleiherino24.model.Article;
import de.propra2.ausleiherino24.model.Case;
import de.propra2.ausleiherino24.model.Category;
import de.propra2.ausleiherino24.model.Person;
import de.propra2.ausleiherino24.model.User;
import java.util.ArrayList;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.servlet.ServletContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.stereotype.Component;

@Component
public class Initializer implements ServletContextInitializer {

	private final UserRepository userRepository;
	private final ArticleRepository articleRepository;
	private final CaseRepository caseRepository;
	private final PersonRepository personRepository;

	@Autowired
	public Initializer(UserRepository userRepository, ArticleRepository articleRepository,
			CaseRepository caseRepository, PersonRepository personRepository) {
		this.userRepository = userRepository;
		this.articleRepository = articleRepository;
		this.caseRepository = caseRepository;
		this.personRepository = personRepository;
	}

	@Override
	public void onStartup(final ServletContext servletContext) {
		initTestAccounts();
		initTestArticleWithinUsers();
	}

	private void initTestArticleWithinUsers() {
		articleRepository.deleteAll();
		Faker faker = new Faker(Locale.GERMAN);
		IntStream.range(0, 15).forEach(value -> {
			Person person = createPerson(
					faker.address().fullAddress(),
					faker.name().firstName(),
					faker.name().lastName());

			User user = createUser(
					person.getFirstName() + person.getLastName() + "@mail.de",
					faker.name().fullName(),
					faker.crypto().sha256(),
					person);

			ArrayList<Case> cases = IntStream.range(0, faker.random().nextInt(1, 7)).mapToObj(value1 -> {
				Article article2 = createArticle(
						faker.pokemon().name(),
						faker.chuckNorris().fact(),
						Category.getAllCategories().get(faker.random().nextInt(0, Category.getAllCategories().size() - 1)),
						user,
						faker.random().nextInt(5, 500),
						faker.random().nextInt(100, 2000));

				return createCase(
						faker.random().nextBoolean(),
						article2);
			}).collect(Collectors.toCollection(ArrayList::new));

			personRepository.save(person);
			userRepository.save(user);
			for (Case c: cases) {
				articleRepository.save(c.getArticle());
				caseRepository.save(c);
			}
		});
	}

	private void initTestAccounts() {
		userRepository.deleteAll();

		String pw = "password";
		User user = new User();
		user.setUsername("user");
		user.setPassword(pw);
		user.setEmail("user@mail.com");
		user.setRole("user");

		User user2 = new User();
		user2.setUsername("admin");
		user2.setPassword(pw);
		user2.setEmail("useradmin@mail.com");
		user2.setRole("admin");

		userRepository.save(createUser(
				"hans@mail.de",
				"Hans",
				pw,
				createPerson(
						"HHU",
						"Hans",
						"Peter")));

		userRepository.save(user2);
		userRepository.save(user);
	}

	private Person createPerson(String address, String firstname, String lastname) {
		Person person = new Person();
		person.setAddress(address);
		person.setFirstName(firstname);
		person.setLastName(lastname);
		return person;
	}

	private User createUser(String email, String username, String password, Person person) {
		User user = new User();
		user.setEmail(email);
		user.setUsername(username);
		user.setPassword(password);
		user.setRole("user");
		user.setPerson(person);
		return user;
	}

	private Article createArticle(String name, String description, Category category, User owner, int costPerDay, int deposit) {
		Article article = new Article();
		article.setActive(true);
		article.setReserved(false);
		article.setName(name);
		article.setDescription(description);
		article.setCategory(category);
		article.setOwner(owner);
		article.setCostPerDay(costPerDay);
		article.setDeposit(deposit);
		return article;
	}

	private Case createCase(boolean active, Article article) {
		Case aCase = new Case();
		aCase.setActive(active);
		aCase.setArticle(article);
		aCase.setPrice(article.getCostPerDay());
		aCase.setDeposit(article.getDeposit());
		return aCase;
	}
}
