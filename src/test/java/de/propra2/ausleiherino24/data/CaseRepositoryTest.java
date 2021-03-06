package de.propra2.ausleiherino24.data;

import de.propra2.ausleiherino24.model.Article;
import de.propra2.ausleiherino24.model.Case;
import de.propra2.ausleiherino24.model.Person;
import de.propra2.ausleiherino24.model.User;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@ActiveProfiles(profiles = "test")

public class CaseRepositoryTest {

    @Autowired
    private CaseRepository cases;

    @Autowired
    private UserRepository users;

    @Autowired
    private PersonRepository persons;

    private Case case1;
    private Case case2;

    @BeforeEach
    public void init() {
        case1 = new Case();
        case1.setReceiver(new User());
        case1.setArticle(new Article());
        case1.setPrice(80D);
        case1.setDeposit(200D);
        case1.setStartTime(12022019L);
        case1.setEndTime(19022019L);

        case2 = new Case();
        case2.setReceiver(new User());
        case2.setArticle(new Article());
        case2.setPrice(60D);
        case2.setDeposit(150D);
        case2.setStartTime(10022019L);
        case2.setEndTime(15022019L);

        cases.saveAll(Arrays.asList(case1, case2));
    }

    @Test
    public void databaseShouldSaveEntities() {
        final List<Case> us = cases.findAll();
        Assertions.assertThat(us.size()).isEqualTo(2);
        Assertions.assertThat(us).containsExactlyInAnyOrder(case1, case2);
    }

    @Test
    public void databaseShouldRemoveCorrectEntity() {
        cases.delete(case2);

        final List<Case> us = cases.findAll();
        Assertions.assertThat(us.size()).isOne();
        Assertions.assertThat(us.get(0)).isEqualTo(case1);
    }

    @Test
    public void databaseShouldReturnCountOfTwoIfDatabaseHasTwoEntries() {
        Assertions.assertThat(cases.count()).isEqualTo(2);
    }

    @Test
    public void queryFindByArticleAndRequestStatusShouldReturnCaseWithCorrespondingArticleIfStatusIsMatching() {
        case2.setRequestStatus(Case.RUNNING);
        final List<Case> expectedCases = cases
                .findAllByArticleAndRequestStatus(case2.getArticle(), Case.RUNNING);
        Assertions.assertThat(expectedCases.size()).isOne();
        Assertions.assertThat(expectedCases.get(0)).isEqualTo(case2);
    }

    @Test
    public void queryFindByArticleAndRequestStatusShouldReturnNoCaseWithCorrespondingArticleIfStatusIsNotMatching() {
        case2.setRequestStatus(Case.RUNNING);
        final List<Case> expectedCases = cases
                .findAllByArticleAndRequestStatus(case2.getArticle(), Case.REQUEST_ACCEPTED);
        Assertions.assertThat(expectedCases.size()).isZero();
    }

    @Test
    public void customQueryFindAllByReceiverShouldReturnCaseWithCorrespondingReceiver() {
        final List<Case> expectedCase = cases.findAllByReceiver(case1.getReceiver());
        Assertions.assertThat(expectedCase.size()).isOne();
        Assertions.assertThat(expectedCase.get(0)).isEqualTo(case1);
    }

    @Test
    public void customQueryFindAllByArticleOwnerShouldReturnCaseWithCorrespondingArticleOwner() {
        case2.getArticle().setOwner(new User());
        case1.getArticle().setOwner(new User());

        final List<Case> expectedCase = cases.findAllByArticleOwner(case2.getOwner());
        Assertions.assertThat(expectedCase.size()).isOne();
        Assertions.assertThat(expectedCase.get(0)).isEqualTo(case2);
    }

    @Test
    public void customQueryFindAllByArticleOwnerShouldReturnTwoCaseWithCorrespondingArticleOwner() {
        final User user = new User();
        case2.getArticle().setOwner(user);
        case1.getArticle().setOwner(user);

        final List<Case> expectedCase = cases.findAllByArticleOwner(case2.getOwner());
        Assertions.assertThat(expectedCase.size()).isEqualTo(2);
        Assertions.assertThat(expectedCase).containsExactlyInAnyOrder(case1, case2);
    }


    @Test
    public void customQueryFindAllByArticleOwnerIdShouldReturnCaseWithCorrespondingArticleOwner() {
        final User owner1 = new User();
        final User owner2 = new User();
        users.saveAll(Arrays.asList(owner1, owner2));

        case2.getArticle().setOwner(owner2);
        case1.getArticle().setOwner(owner1);

        final List<Case> expectedCase = cases.findAllByArticleOwnerId(case2.getOwner().getId());
        Assertions.assertThat(expectedCase.size()).isOne();
        Assertions.assertThat(expectedCase.get(0)).isEqualTo(case2);
    }

    @Test
    public void customQueryFindAllByArticleOwnerIdShouldReturnTwoCaseWithCorrespondingArticleOwnerOrderedByRequestStatusAsc() {
        case1.setRequestStatus(Case.RUNNING);
        case2.setRequestStatus(Case.RUNNING_EMAILSENT);
        final User user = new User();
        users.save(user);

        case2.getArticle().setOwner(user);
        case1.getArticle().setOwner(user);

        final List<Case> expectedCase = cases.findAllByArticleOwnerId(case2.getOwner().getId());
        Assertions.assertThat(expectedCase.size()).isEqualTo(2);
        Assertions.assertThat(expectedCase).containsExactly(case1, case2);
    }

    @Test
    public void customQueryFindAllExpiredCasesByUserIdShouldReturnTwoCases() {
        User user1 = new User();
        users.save(user1);

        case1.getArticle().setForSale(false);
        case1.getArticle().setOwner(user1);
        case1.setEndTime(0L);
        case1.setRequestStatus(8);

        case2.getArticle().setForSale(false);
        case2.getArticle().setOwner(user1);
        case2.setEndTime(0L);
        case2.setRequestStatus(14);

        final List<Case> actualCases = cases
                .findAllExpiredCasesByUserId(case1.getArticle().getOwner().getId(), 1L);
        final List<Case> expectedCases = new ArrayList<>(Arrays.asList(case1, case2));

        Assertions.assertThat(actualCases.size()).isEqualTo(2);
        Assertions.assertThat(actualCases).isEqualTo(expectedCases);
    }

    @Test
    public void customQueryFindAllExpiredCasesByUserIdShouldReturnZeroCases() {
        User user1 = new User();
        users.save(user1);

        case1.getArticle().setForSale(true);
        case1.getArticle().setOwner(user1);
        case1.setEndTime(2L);
        case1.setRequestStatus(8);

        case2.getArticle().setForSale(false);
        case2.getArticle().setOwner(user1);
        case2.setEndTime(0L);
        case2.setRequestStatus(5);

        final List<Case> actualCases = cases
                .findAllExpiredCasesByUserId(case1.getArticle().getOwner().getId(), 1L);

        Assertions.assertThat(actualCases.isEmpty()).isTrue();
    }

    @Test
    public void customQueryFindAllRequestedCasesByUserIdShouldReturnTwoCases() {
        User user1 = new User();
        users.save(user1);

        case1.getArticle().setForSale(false);
        case1.getArticle().setOwner(user1);
        case1.setRequestStatus(1);

        case2.getArticle().setForSale(false);
        case2.getArticle().setOwner(user1);
        case2.setRequestStatus(4);

        final List<Case> actualCases = cases
                .findAllRequestedCasesByUserId(case1.getArticle().getOwner().getId());
        final List<Case> expectedCases = new ArrayList<>(Arrays.asList(case1, case2));

        Assertions.assertThat(actualCases.size()).isEqualTo(2);
        Assertions.assertThat(actualCases).isEqualTo(expectedCases);
    }

    @Test
    public void customQueryFindAllRequestedCasesByUserIdShouldReturnZeroCases() {
        User user1 = new User();
        users.save(user1);

        case1.getArticle().setForSale(true);
        case1.getArticle().setOwner(user1);
        case1.setRequestStatus(1);

        case2.getArticle().setForSale(false);
        case2.getArticle().setOwner(new User());
        case2.setRequestStatus(14);

        final List<Case> actualCases = cases
                .findAllRequestedCasesByUserId(case1.getArticle().getOwner().getId());

        Assertions.assertThat(actualCases.isEmpty()).isTrue();
    }

    @Test
    public void customQueryGetLendCasesFromPersonReceiverShouldReturnOneCase() {
        Person person = new Person();
        persons.save(person);

        case1.getArticle().setForSale(true);
        case1.getReceiver().setPerson(person);

        case2.getArticle().setForSale(false);
        case2.getReceiver().setPerson(person);

        final List<Case> actualCases = cases
                .getLendCasesFromPersonReceiver(case2.getReceiver().getPerson().getId());
        final List<Case> expectedCases = new ArrayList<>(Arrays.asList(case2));

        Assertions.assertThat(actualCases).isEqualTo(expectedCases);
    }

    @Test
    public void customQueryfindAllSoldItemsByUserIdShouldReturnOneCases() {
        User user1 = new User();
        users.save(user1);

        case1.getArticle().setForSale(false);
        case1.getArticle().setOwner(new User());
        case1.setRequestStatus(1);

        case2.getArticle().setForSale(true);
        case2.getArticle().setOwner(user1);
        case2.setRequestStatus(14);

        final List<Case> actualCases = cases
                .findAllSoldItemsByUserId(case2.getArticle().getOwner().getId());
        final List<Case> expectedCases = new ArrayList<>(Arrays.asList(case2));

        Assertions.assertThat(actualCases).isEqualTo(expectedCases);
    }

    @Test
    public void customQueryfindAllOutrunningCasesByUserIdShouldReturnOneCases() {
        User user1 = new User();
        users.save(user1);

        case1.getArticle().setForSale(false);
        case1.setReceiver(user1);
        case1.setEndTime(2L);

        case2.getArticle().setForSale(true);
        case2.setReceiver(new User());
        case2.setEndTime(4L);

        final List<Case> actualCases = cases
                .findAllOutrunningCasesByUserId(case1.getReceiver().getId(), 1L, 3L);
        final List<Case> expectedCases = new ArrayList<>(Arrays.asList(case1));

        Assertions.assertThat(actualCases).isEqualTo(expectedCases);
    }
}
