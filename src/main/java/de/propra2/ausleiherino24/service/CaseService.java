package de.propra2.ausleiherino24.service;

import de.propra2.ausleiherino24.data.CaseRepository;
import de.propra2.ausleiherino24.data.PersonRepository;
import de.propra2.ausleiherino24.model.Article;
import de.propra2.ausleiherino24.model.Case;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import de.propra2.ausleiherino24.model.PPTransaction;
import de.propra2.ausleiherino24.propayhandler.AccountHandler;
import de.propra2.ausleiherino24.propayhandler.ReservationHandler;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CaseService {

    private final CaseRepository caseRepository;
    private final PersonRepository personRepository;
    private final ArticleService articleService;
    private final UserService userService;
    private final AccountHandler accountHandler;
    private final ReservationHandler reservationHandler;

    /**
     * TODO JavaDoc.
     *
     * @param caseRepository Description
     * @param personRepository Description
     * @param articleService Description
     * @param userService Description
     */
    @Autowired
    public CaseService(CaseRepository caseRepository, PersonRepository personRepository,
            ArticleService articleService,
            UserService userService, AccountHandler accountHandler,
            ReservationHandler reservationHandler) {
        this.caseRepository = caseRepository;
        this.personRepository = personRepository;
        this.articleService = articleService;
        this.userService = userService;
        this.accountHandler = accountHandler;
        this.reservationHandler = reservationHandler;
    }

    /**
     * Fügt einen Artikel, welcher frei zum Verleih ist, von einer Person hinzu.
     */
    public void addCaseForNewArticle(Article article, Double price, Double deposit) {
        Case c = new Case();
        c.setArticle(article);
        c.setDeposit(deposit);
        c.setPrice(price);

        caseRepository.save(c);
    }

    /**
     * Gibt alle Cases zurück, wo die Person der Verleihende ist.
     */
    public List<Case> getAllCasesFromPersonOwner(Long personId) {
        return caseRepository
                .findAllByArticleOwner(personRepository.findById(personId).get().getUser());
    }

    /**
     * Gibt alle Cases zurück, wo die Person der Verleihende ist.
     */
    public List<Case> findAllCasesbyUserId(Long userId) {
        return caseRepository
                .findAllByArticleOwnerId(userId);
    }

    /**
     * Gibt alle Cases zurück, wo die Person der Verleihende ist und der Artikel momentan verliehen
     * ist.
     */
    public List<Case> getLendCasesFromPersonOwner(Long personId) {
        List<Case> cases = getAllCasesFromPersonOwner(personId);
        return cases.stream()
                .filter(c -> c.getReceiver() != null)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Gibt alle Cases zurück, wo die Person der Verleihende ist und der Artikel momentan nicht
     * verliehen ist.
     */
    public List<Case> getFreeCasesFromPersonOwner(Long personId) {
        List<Case> cases = getAllCasesFromPersonOwner(personId);
        return cases.stream()
                .filter(c -> c.getReceiver() == null)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Gibt alle Cases zurück, wo die Person sich von jemanden etwas geliehen hat.
     */
    public List<Case> getLendCasesFromPersonReceiver(Long personId) {
        return caseRepository
                .findAllByReceiver(personRepository.findById(personId).get().getUser());
    }

    /**
     * Gibt alle Cases zurück, die zu einem Artikel vom User gehören und dessen requestStatus auf
     * REQUESTED steht.
     */
    public List<Case> getAllRequestedCasesbyUser(Long userId) {
        return caseRepository
                .findAllByArticleOwner(userService.findUserById(userId))
                .stream()
                .filter(c -> c.getRequestStatus() == Case.REQUESTED)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Erwartet Case mit wo Artikel verliehen werden kann. Case wird modifiziert, dass es nun
     * verliehen ist.
     */
    public boolean requestArticle(Long articleId, Long starttime, Long endtime, String username)
            throws Exception {

        Double totalCost = getCostForAllDays(articleId, starttime, endtime);

        if (accountHandler.hasValidFunds(username,
                totalCost + articleService.findArticleById(articleId).getDeposit())) {

            PPTransaction ppTransaction = new PPTransaction();
            ppTransaction.setLendingCost(totalCost);
            ppTransaction.setCautionPaid(false);

            Case c = new Case();
            c.setArticle(articleService.findArticleById(articleId));
            c.setStartTime(starttime);
            c.setEndTime(endtime);
            c.setDeposit(c.getArticle().getDeposit());
            c.setPrice(c.getArticle().getCostPerDay());
            c.setReceiver(userService.findUserByUsername(username));
            c.setRequestStatus(Case.REQUESTED);
            c.setPpTransaction(ppTransaction);

            caseRepository.save(c);

            reservationHandler.handleReservedMoney(c);

            return true;
        }
        return false;
    }

    Double getCostForAllDays(Long articleId, Long starttime, Long endtime) throws Exception {

        Double dailyCost = articleService.findArticleById(articleId).getCostPerDay();
        Date startdate = new Date(starttime);
        Date enddate = new Date(endtime);

        long diffInMillies = Math.abs(enddate.getTime() - startdate.getTime());

        return new Double(TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS)) * dailyCost;
    }


    /**
     * return true, falls Erfolg
     * return false, falls Misserfolg
     */
    public boolean acceptArticleRequest(Long id) {
        Optional<Case> optCase = caseRepository.findById(id);
        if (!optCase.isPresent())
            return false;
        Case c = optCase.get();

        //Check whether the article is not reserved in this period of time
        if (requestIsOk(id) && accountHandler.hasValidFunds(c)) {
            c.setRequestStatus(Case.REQUEST_ACCEPTED);
            reservationHandler.handleReservedMoney(c);
            caseRepository.save(c);
            return true;
        } else {
            c.setRequestStatus(Case.RENTAL_NOT_POSSIBLE);
            caseRepository.save(c);
            return false;
        }
    }

    boolean requestIsOk(Long id) {
        Case c = caseRepository.findById(id).get();
        Article article = c.getArticle();
        List<Case> cases = article.getCases().stream()
                .filter(ca -> ca.getRequestStatus() == Case.REQUEST_ACCEPTED)
                .collect(Collectors.toList());
        cases.remove(c); //Makes sure, that c is not an element in cases

        for (Case ca : cases) {
            if (!(ca.getStartTime() > c.getEndTime() || ca.getEndTime() < c.getStartTime())) {
                return false;
            }
        }
        return true;
    }

    public void declineArticleRequest(Long id) {
        Optional<Case> optCase = caseRepository.findById(id);
        if(!optCase.isPresent())
            return;
        Case c = optCase.get();
        c.setRequestStatus(Case.REQUEST_DECLINED);
        reservationHandler.releaseReservation(c);
        c.setPpTransaction(new PPTransaction());
        caseRepository.save(c);
    }


    public List<Case> findAllExpiredCasesbyUserId(Long id) {
        return findAllCasesbyUserId(id)
                .stream()
                .filter(c -> c.getEndTime() < new Date().getTime())
                .filter(c -> c.getRequestStatus() == Case.RUNNING ||
                        c.getRequestStatus() == Case.FINISHED ||
                        c.getRequestStatus() == Case.OPEN_CONFLICT)
                .collect(Collectors.toList());
    }

    /**
     * Stellt den Status von Case mit id id auf Case.OPEN_CONFLICT
     *
     * @param id CaseId
     */
    public void conflictOpened(Long id) {
        Optional<Case> opt = caseRepository.findById(id);
        if (opt.isPresent()) {
            Case c = opt.get();
            c.setRequestStatus(Case.OPEN_CONFLICT);
            caseRepository.save(c);
        }
    }

    /**
     * Stellt den Status von Case mit id id auf Case.FINISHED
     *
     * @param id CaseId
     */
    public void acceptCaseReturn(Long id) {
        Optional<Case> opt = caseRepository.findById(id);
        if (opt.isPresent()) {
            Case c = opt.get();
            c.setRequestStatus(Case.FINISHED);
            caseRepository.save(c);
        }
    }

    /**
     * Findet alle Cases mit Status in {REQUESTED, REQUEST_ACCEPTED, REQUEST_DECLINED,
     * RENTAL_NOT_POSSIBLE}
     * @param id
     * @return
     */
    public List<Case> findAllRequestedCasesbyUserId(Long id) {
        return findAllCasesbyUserId(id)
                .stream()
                .filter(c -> c.getRequestStatus() == Case.REQUESTED
                        || c.getRequestStatus() == Case.REQUEST_ACCEPTED
                        || c.getRequestStatus() == Case.REQUEST_DECLINED
                        || c.getRequestStatus() == Case.RENTAL_NOT_POSSIBLE)
                .collect(Collectors.toList());
    }

    public List<LocalDate> findAllReservedDaysbyArticle(Long id) throws Exception {
        return caseRepository
                .findAllByArticleAndRequestStatus(articleService.findArticleById(id), 2)
                .stream()
                .map(c -> {
                    LocalDate start = LocalDate.ofInstant(Instant.ofEpochMilli(c.getStartTime()),
                            ZoneId.systemDefault());
                    LocalDate end = LocalDate.ofInstant(Instant.ofEpochMilli(c.getEndTime()),
                            ZoneId.systemDefault());
                    int daysInBetween = Period.between(start, end).getDays();
                    return IntStream
                            .range(0, daysInBetween+1)
                            .mapToObj(start::plusDays);
                })
                .flatMap(Function.identity())
                .collect(Collectors.toList());


    }
}
