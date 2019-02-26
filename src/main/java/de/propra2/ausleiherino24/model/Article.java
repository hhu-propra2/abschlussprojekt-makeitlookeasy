package de.propra2.ausleiherino24.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
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
    private Long id;

    private String name;

    @Column(length = 10485760)
    private String description;

    private String image;

    private Double deposit;

    private Double costPerDay;

    private String location;

    /**
     * true: Artikel existiert noch false: Artikel gelöscht
     */
    private boolean active;

    /**
     * true: Artikel kann zur Zeit ausgeliehen werden false: Artikel ist zur Zeit nicht zum
     * Ausleihen verfügbar
     */
    //TODO must be changed if status changes
    private boolean forRental;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn
    private User owner;

    private Category category;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "article")
    private List<Case> cases;

    /**
     * TODO : Welche Konstruktion? Englisch? Neuschreiben! Die Konstruktion ist nötig, damit der
     * Case stets mit geupdated wird. Analoges ist im Case Siehe
     * <a href="https://notesonjava.wordpress.com/2008/11/03/managing-the-bidirectional-relationship/">hier</a>
     */

    public void addCase(final Case aCase) {
        addCase(aCase, false);
    }

    @SuppressWarnings("Duplicates")
        // TODO: Extract duplicate code. Fix!
    void addCase(final Case aCase, final boolean repetition) {
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

    // TODO: Method is never used. Delete?
    public void removeCase(final Case aCase) {
        cases.remove(aCase);
        aCase.setArticle(null);
    }

    public void setOwner(final User user) {
        setOwner(user, false);
    }

    void setOwner(final User user, final boolean repetition) {
        this.owner = user;
        if (user != null && !repetition) {
            user.addArticle(this, true);
        }
    }

    /**
     * @return returns true if article has only cases where the requeststatus is REQUEST_DECLINED,
     * RENTAL_NOT_POSSIBLE or FINISHED, otherwise returns false
     */
    public boolean allCasesClosed() {
        List<Case> activeCases = new ArrayList<>();

        if (getCases() != null) {
            activeCases = getCases().stream()
                    .filter(c -> c.getRequestStatus() != 12 && c.getRequestStatus() != 4
                            && c.getRequestStatus() != 14)
                    .collect(Collectors.toList());
        }

        return activeCases.isEmpty();
    }
}
