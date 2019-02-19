package de.propra2.ausleiherino24.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Article {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	Long id;

	String name;

	@Column(length = 10485760)
	String description;

	String image;

	Boolean reserved;  // If this is true the article is not available for rental ("reserved/rented")

	int deposit;

	int costPerDay;

	String location;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	User owner;

	Boolean active;    // If this is true the article is not available for rental ("deleted")

	Category category;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	List<Case> cases;

	/**
	 * Die Konstruktion ist nötig, damit der Case stets mit geupdatet wird. Analoges ist im Case
	 * Siehe
	 * <a href="https://notesonjava.wordpress.com/2008/11/03/managing-the-bidirectional-relationship/">hier</a>
	 */
	public void addCase(Case aCase) {
		addCase(aCase, false);
	}

	void addCase(Case aCase, boolean repetition) {
		if (aCase == null) {
			return;
		}
		if (cases == null) {
			cases = new ArrayList<>();
		}
		if (cases.contains(aCase)) {
			cases.set(cases.indexOf(aCase), aCase);
		} else {
			cases.add(aCase);
		}
		if (!repetition) {
			aCase.setArticle(this, true);
		}
	}

	public void removeCase(Case aCase) {
		cases.remove(aCase);
		aCase.setArticle(null);
	}

	public void setOwner(User user) {
		setOwner(user, false);
	}

	void setOwner(User user, boolean repetition) {
		this.owner = user;
		if (user != null && !repetition) {
			user.addArticle(this, true);
		}
	}
}
