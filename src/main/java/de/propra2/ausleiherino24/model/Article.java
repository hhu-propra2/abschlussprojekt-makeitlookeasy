package de.propra2.ausleiherino24.model;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    /*
    public void setACase(Case aCase) {
        setACase(aCase, false);
    }

    void setACase(Case aCase, boolean repetition) {
        this.aCase = aCase;
        if (aCase != null && !repetition) {
            aCase.setArticle(this, true);
        }
    }
*/
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
